package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.crypto.KryptCryptoEngine
import com.example.data.local.KryptPreferences
import com.example.data.model.DecryptedMessageUi
import com.example.data.model.GroupMemberRecord
import com.example.data.model.GroupRole
import com.example.data.model.GroupSenderKeyDoc
import com.example.data.model.KeyBundle
import com.example.data.model.KryptGroup
import com.example.data.model.KryptMessage
import com.example.data.model.MessageType
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.SecureRandom
import java.util.Date
import java.util.UUID

class KryptGroupRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val crypto: KryptCryptoEngine,
    private val prefs: KryptPreferences
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id)),
        auth = FirebaseAuth.getInstance(),
        crypto = KryptCryptoEngine(context.applicationContext),
        prefs = KryptPreferences(context.applicationContext)
    )

    private val random = SecureRandom()

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Authentication required.")

    fun observeMyGroups(uid: String): Flow<List<KryptGroup>> {
        val path = "groups"
        return db.collection("groups")
            .whereArrayContains("memberUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(KryptGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.lastMessageAt?.toDate()?.time ?: it.updatedAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, uid)
                throw error
            }
    }

    fun observeDiscoverableGroups(): Flow<List<KryptGroup>> {
        val path = "groups"
        return db.collection("groups")
            .whereEqualTo("discoverable", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(KryptGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { !it.suspended }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, auth.currentUser?.uid)
                throw error
            }
    }

    fun observeGroup(groupId: String): Flow<KryptGroup?> {
        val path = "groups/$groupId"
        return db.collection("groups").document(groupId)
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(KryptGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, path, auth.currentUser?.uid)
                throw error
            }
    }

    fun observeGroupMembers(groupId: String): Flow<List<GroupMemberRecord>> {
        val path = "groups/$groupId/members"
        return db.collection("groups").document(groupId).collection("members")
            .snapshots()
            .map { snap ->
                snap.toObjects(GroupMemberRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, auth.currentUser?.uid)
                throw error
            }
    }

    fun observeGroupMessages(groupId: String, viewerUid: String): Flow<List<DecryptedMessageUi>> {
        val path = "groups/$groupId/messages"
        return db.collection("groups").document(groupId).collection("messages")
            .whereArrayContains("memberUids", viewerUid)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(100)
            .snapshots()
            .map { snap ->
                // Sync any newly received sender keys first
                runCatching { syncIncomingSenderKeys(groupId, viewerUid) }
                snap.toObjects(KryptMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .map { msg ->
                        val plain = if (msg.deleted) {
                            "Message deleted by moderation or author"
                        } else {
                            crypto.decryptGroupMessage(
                                groupId = groupId,
                                senderUid = msg.senderId,
                                ciphertextBase64 = msg.ciphertext,
                                ivBase64 = msg.iv,
                                epoch = msg.keyVersion
                            )
                        }
                        DecryptedMessageUi(
                            id = msg.id,
                            senderId = msg.senderId,
                            senderName = msg.senderName,
                            plaintext = plain,
                            messageType = MessageType.safeValueOf(msg.messageType),
                            mediaName = msg.mediaName,
                            mediaMime = msg.mediaMime,
                            mediaDataBase64 = msg.mediaDataBase64,
                            mediaDurationMs = msg.mediaDurationMs,
                            replyToMessageId = msg.replyToMessageId,
                            replyToPreview = msg.replyToPreview,
                            forwardedFrom = msg.forwardedFrom,
                            reactions = msg.reactions,
                            edited = msg.edited,
                            deleted = msg.deleted,
                            pinned = msg.pinned,
                            deliveredToUids = emptyList(),
                            readByUids = msg.readByUids,
                            createdAtMillis = msg.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, viewerUid)
                throw error
            }
    }

    private suspend fun syncIncomingSenderKeys(groupId: String, viewerUid: String) {
        val keysSnap = db.collection("groups").document(groupId).collection("sender_keys")
            .whereEqualTo("recipientUid", viewerUid)
            .get()
            .await()
        keysSnap.toObjects(GroupSenderKeyDoc::class.java).forEach { doc ->
            val unwrapped = crypto.unwrapGroupSenderKey(
                viewerUid = viewerUid,
                groupId = groupId,
                wrappedCipherBase64 = doc.wrappedKeyCiphertext,
                ivBase64 = doc.wrappedKeyIv,
                senderEphemeralPubBase64 = doc.senderEphemeralPubKey
            )
            if (unwrapped != null) {
                crypto.saveReceivedGroupSenderKey(groupId, doc.senderUid, doc.keyEpoch, unwrapped)
            }
        }
    }

    suspend fun distributeSenderKey(groupId: String, senderUid: String, memberUids: List<String>, epoch: Long = 1L) {
        val groupKey = crypto.getOrCreateGroupSenderKey(groupId, epoch)
        memberUids.forEach { targetUid ->
            val keyBundle = db.collection("key_bundles").document(targetUid).get().await()
                .toObject(KeyBundle::class.java) ?: return@forEach
            val wrapped = crypto.wrapSenderKeyForMember(groupId, groupKey, keyBundle.identityKeyPub)
            val docId = "${senderUid}_${targetUid}_$epoch"
            db.collection("groups").document(groupId).collection("sender_keys").document(docId)
                .set(
                    mapOf(
                        "groupId" to groupId,
                        "senderUid" to senderUid,
                        "recipientUid" to targetUid,
                        "keyEpoch" to epoch,
                        "wrappedKeyCiphertext" to wrapped.ciphertextBase64,
                        "wrappedKeyIv" to wrapped.ivBase64,
                        "senderEphemeralPubKey" to wrapped.ephemeralPubKeyBase64,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
        }
    }

    suspend fun createGroup(
        name: String,
        description: String,
        discoverable: Boolean,
        slowModeSeconds: Long,
        initialMemberUids: List<String>
    ): Result<String> = try {
        val uid = requireUid()
        val cleanName = name.trim().take(60)
        require(cleanName.isNotEmpty()) { "Group name is required." }
        val groupId = "grp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val allMembers = (listOf(uid) + initialMemberUids).distinct().take(100)
        val inviteCode = generateInviteCode()
        val inviteExpiry = Timestamp(Date(System.currentTimeMillis() + 7L * 24L * 3600L * 1000L))

        val groupRef = db.collection("groups").document(groupId)
        db.runBatch { batch ->
            batch.set(
                groupRef,
                mapOf(
                    "name" to cleanName,
                    "description" to description.trim().take(300),
                    "iconBase64" to "",
                    "ownerId" to uid,
                    "memberUids" to allMembers,
                    "adminUids" to listOf(uid),
                    "moderatorUids" to emptyList<String>(),
                    "discoverable" to discoverable,
                    "locked" to false,
                    "suspended" to false,
                    "slowModeSeconds" to slowModeSeconds.coerceIn(0L, 3600L),
                    "inviteCode" to inviteCode,
                    "inviteExpiresAt" to inviteExpiry,
                    "announcementText" to "",
                    "pinnedMessageId" to "",
                    "mutedByUids" to emptyList<String>(),
                    "lastMessagePreview" to "Group vault created",
                    "lastMessageSenderId" to uid,
                    "lastMessageAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            allMembers.forEach { memberUid ->
                val mRef = groupRef.collection("members").document(memberUid)
                batch.set(
                    mRef,
                    mapOf(
                        "groupId" to groupId,
                        "userId" to memberUid,
                        "role" to if (memberUid == uid) "OWNER" else "MEMBER",
                        "muted" to false,
                        "canPost" to true,
                        "joinedAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                db.collection("group_invites").document(inviteCode),
                mapOf(
                    "inviteCode" to inviteCode,
                    "groupId" to groupId,
                    "expiresAt" to inviteExpiry,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()

        runCatching { distributeSenderKey(groupId, uid, allMembers) }
        Result.success(groupId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "groups")
        Result.failure(e)
    }

    suspend fun sendGroupMessage(
        group: KryptGroup,
        senderProfile: UserProfile,
        plaintext: String,
        messageType: MessageType = MessageType.TEXT,
        mediaName: String = "",
        mediaMime: String = "",
        mediaDataBase64: String = "",
        mediaDurationMs: Long = 0L,
        replyToMessageId: String = "",
        replyToPreview: String = "",
        forwardedFrom: String = ""
    ): Result<Unit> {
        val uid = requireUid()
        if (group.suspended) return Result.failure(IllegalStateException("This group is suspended by Platform Moderation."))
        val isPrivileged = uid == group.ownerId || uid in group.adminUids || uid in group.moderatorUids || senderProfile.role == "OWNER"
        if (group.locked && !isPrivileged) {
            return Result.failure(IllegalStateException("Group is locked. Only admins & moderators may post."))
        }

        return try {
            val encrypted = crypto.encryptGroupMessage(group.id, uid, plaintext.ifBlank { mediaName.ifBlank { messageType.name } }, 1L)
            runCatching { distributeSenderKey(group.id, uid, group.memberUids, 1L) }

            val msgId = "gmsg_${UUID.randomUUID().toString().replace("-", "").take(20)}"
            val groupRef = db.collection("groups").document(group.id)
            val msgRef = groupRef.collection("messages").document(msgId)

            val preview = when (messageType) {
                MessageType.TEXT -> "${senderProfile.displayName}: Encrypted message"
                else -> "${senderProfile.displayName}: Encrypted ${messageType.name.lowercase()}"
            }.take(150)

            db.runBatch { batch ->
                batch.set(
                    msgRef,
                    mapOf(
                        "groupId" to group.id,
                        "memberUids" to group.memberUids,
                        "senderId" to uid,
                        "senderName" to senderProfile.displayName.take(60),
                        "ciphertext" to encrypted.ciphertextBase64,
                        "iv" to encrypted.ivBase64,
                        "senderKeyId" to "${uid}_1",
                        "keyVersion" to 1L,
                        "messageType" to messageType.name,
                        "mediaName" to mediaName.take(160),
                        "mediaMime" to mediaMime.take(100),
                        "mediaDataBase64" to mediaDataBase64.take(450_000),
                        "mediaDurationMs" to mediaDurationMs,
                        "replyToMessageId" to replyToMessageId.take(128),
                        "replyToPreview" to replyToPreview.take(200),
                        "forwardedFrom" to forwardedFrom.take(100),
                        "reactions" to emptyMap<String, String>(),
                        "edited" to false,
                        "deleted" to false,
                        "pinned" to false,
                        "readByUids" to listOf(uid),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(
                    groupRef,
                    mapOf(
                        "lastMessagePreview" to preview,
                        "lastMessageSenderId" to uid,
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "groups/${group.id}/messages")
            Result.failure(e)
        }
    }

    suspend fun joinGroupByCode(inviteCode: String, uid: String): Result<String> = try {
        val code = inviteCode.trim().uppercase()
        require(code.isNotEmpty()) { "Enter an invite code." }
        val inviteSnap = db.collection("group_invites").document(code).get().await()
        if (!inviteSnap.exists()) {
            return Result.failure(IllegalArgumentException("No group found for invite code."))
        }
        val groupId = inviteSnap.getString("groupId")
            ?: return Result.failure(IllegalArgumentException("Invalid invite record."))
        val exp = inviteSnap.getTimestamp("expiresAt")?.toDate()
        if (exp != null && exp.before(Date())) {
            return Result.failure(IllegalStateException("Invite link has expired."))
        }
        val groupSnap = db.collection("groups").document(groupId).get().await()
        val group = groupSnap.toObject(KryptGroup::class.java)
            ?: return Result.failure(IllegalArgumentException("Group no longer exists."))
        if (group.inviteCode != code) {
            return Result.failure(IllegalStateException("Invite code has been rotated."))
        }
        if (group.suspended) return Result.failure(IllegalStateException("Group is suspended."))
        joinGroupDirect(group.id, uid)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.GET, "group_invites")
        Result.failure(e)
    }

    suspend fun joinGroupDirect(groupId: String, uid: String): Result<String> = try {
        val groupRef = db.collection("groups").document(groupId)
        groupRef.update(
            mapOf(
                "memberUids" to FieldValue.arrayUnion(uid),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        groupRef.collection("members").document(uid).set(
            mapOf(
                "groupId" to groupId,
                "userId" to uid,
                "role" to "MEMBER",
                "muted" to false,
                "canPost" to true,
                "joinedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(groupId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "groups/$groupId")
        Result.failure(e)
    }

    suspend fun addMember(groupId: String, targetUid: String): Result<Unit> = try {
        val groupRef = db.collection("groups").document(groupId)
        groupRef.update(
            mapOf(
                "memberUids" to FieldValue.arrayUnion(targetUid),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        groupRef.collection("members").document(targetUid).set(
            mapOf(
                "groupId" to groupId,
                "userId" to targetUid,
                "role" to "MEMBER",
                "muted" to false,
                "canPost" to true,
                "joinedAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "groups/$groupId")
        Result.failure(e)
    }

    suspend fun removeMember(groupId: String, targetUid: String): Result<Unit> = try {
        val groupRef = db.collection("groups").document(groupId)
        groupRef.update(
            mapOf(
                "memberUids" to FieldValue.arrayRemove(targetUid),
                "adminUids" to FieldValue.arrayRemove(targetUid),
                "moderatorUids" to FieldValue.arrayRemove(targetUid),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        groupRef.collection("members").document(targetUid).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "groups/$groupId")
        Result.failure(e)
    }

    suspend fun assignMemberRole(groupId: String, targetUid: String, role: GroupRole): Result<Unit> = try {
        val groupRef = db.collection("groups").document(groupId)
        val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
        when (role) {
            GroupRole.ADMIN -> {
                updates["adminUids"] = FieldValue.arrayUnion(targetUid)
                updates["moderatorUids"] = FieldValue.arrayRemove(targetUid)
            }
            GroupRole.MODERATOR -> {
                updates["adminUids"] = FieldValue.arrayRemove(targetUid)
                updates["moderatorUids"] = FieldValue.arrayUnion(targetUid)
            }
            GroupRole.MEMBER, GroupRole.OWNER -> {
                updates["adminUids"] = FieldValue.arrayRemove(targetUid)
                updates["moderatorUids"] = FieldValue.arrayRemove(targetUid)
            }
        }
        groupRef.update(updates).await()
        groupRef.collection("members").document(targetUid).update(
            mapOf("role" to role.name, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "groups/$groupId/members/$targetUid")
        Result.failure(e)
    }

    suspend fun updateGroupSettings(
        groupId: String,
        name: String? = null,
        description: String? = null,
        announcementText: String? = null,
        locked: Boolean? = null,
        slowModeSeconds: Long? = null,
        regenerateInvite: Boolean = false
    ): Result<Unit> = try {
        val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
        if (name != null) updates["name"] = name.trim().take(60)
        if (description != null) updates["description"] = description.trim().take(300)
        if (announcementText != null) updates["announcementText"] = announcementText.trim().take(400)
        if (locked != null) updates["locked"] = locked
        if (slowModeSeconds != null) updates["slowModeSeconds"] = slowModeSeconds.coerceIn(0L, 3600L)
        if (regenerateInvite) {
            val newCode = generateInviteCode()
            val newExpiry = Timestamp(Date(System.currentTimeMillis() + 7L * 24L * 3600L * 1000L))
            updates["inviteCode"] = newCode
            updates["inviteExpiresAt"] = newExpiry
            db.collection("groups").document(groupId).update(updates).await()
            val uid = requireUid()
            db.collection("group_invites").document(newCode).set(
                mapOf(
                    "inviteCode" to newCode,
                    "groupId" to groupId,
                    "expiresAt" to newExpiry,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            db.collection("groups").document(groupId).update(updates).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "groups/$groupId")
        Result.failure(e)
    }

    suspend fun toggleGroupMute(groupId: String, uid: String, muted: Boolean): Result<Unit> = try {
        val op = if (muted) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)
        db.collection("groups").document(groupId)
            .update(mapOf("mutedByUids" to op, "updatedAt" to FieldValue.serverTimestamp())).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "groups/$groupId")
        Result.failure(e)
    }

    suspend fun deleteGroup(groupId: String): Result<Unit> = try {
        db.collection("groups").document(groupId).delete().await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.DELETE, "groups/$groupId")
        Result.failure(e)
    }

    suspend fun toggleGroupMessageReaction(groupId: String, messageId: String, uid: String, emoji: String): Result<Unit> = try {
        val ref = db.collection("groups").document(groupId).collection("messages").document(messageId)
        val snap = ref.get().await()
        val existing = snap.get("reactions.$uid") as? String
        val nextVal = if (existing == emoji) FieldValue.delete() else emoji
        ref.update(mapOf("reactions.$uid" to nextVal, "updatedAt" to FieldValue.serverTimestamp())).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "groups/$groupId/messages/$messageId")
        Result.failure(e)
    }

    suspend fun markGroupMessagesRead(groupId: String, uid: String, messages: List<DecryptedMessageUi>) {
        val ghost = prefs.ghostModeFlow.first()
        if (ghost.isEffectivelyActive()) return
        try {
            val unread = messages.filter { it.senderId != uid && uid !in it.readByUids }.takeLast(15)
            if (unread.isNotEmpty()) {
                val batch = db.batch()
                unread.forEach { msg ->
                    val ref = db.collection("groups").document(groupId).collection("messages").document(msg.id)
                    batch.update(
                        ref,
                        mapOf(
                            "readByUids" to FieldValue.arrayUnion(uid),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
                batch.commit().await()
            }
        } catch (_: Exception) {
        }
    }

    private fun generateInviteCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        return "KRP-" + (1..6).map { chars[random.nextInt(chars.length)] }.joinToString("")
    }

    private fun logFlowError(error: Throwable, op: OperationType, path: String, uid: String?) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        if (uid != null && auth.currentUser?.uid == uid) {
            handleFirestoreError(firestoreError ?: error, op, path)
        }
    }
}
