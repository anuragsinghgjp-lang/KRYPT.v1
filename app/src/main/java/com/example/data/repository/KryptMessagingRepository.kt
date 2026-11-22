package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.crypto.KryptCryptoEngine
import com.example.data.local.CachedConversationEntity
import com.example.data.local.CachedMessageEntity
import com.example.data.local.KryptDatabase
import com.example.data.local.KryptPreferences
import com.example.data.model.Conversation
import com.example.data.model.DecryptedMessageUi
import com.example.data.model.KeyBundle
import com.example.data.model.KryptMessage
import com.example.data.model.MessageType
import com.example.data.model.UserProfile
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
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
import java.util.UUID

class KryptMessagingRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val crypto: KryptCryptoEngine,
    private val prefs: KryptPreferences,
    private val localDb: KryptDatabase
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id)),
        auth = FirebaseAuth.getInstance(),
        crypto = KryptCryptoEngine(context.applicationContext),
        prefs = KryptPreferences(context.applicationContext),
        localDb = KryptDatabase.getInstance(context.applicationContext)
    )

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Authentication required.")

    fun observeConversations(uid: String): Flow<List<Conversation>> {
        val path = "conversations"
        return db.collection("conversations")
            .whereArrayContains("participantUids", uid)
            .snapshots()
            .map { snap ->
                val list = snap.toObjects(Conversation::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.lastMessageAt?.toDate()?.time ?: it.updatedAt?.toDate()?.time ?: 0L }
                list
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, uid)
                throw error
            }
    }

    fun observeCachedConversations(): Flow<List<CachedConversationEntity>> =
        localDb.cacheDao().observeCachedConversations()

    fun observeMessages(conversationId: String, viewerUid: String): Flow<List<DecryptedMessageUi>> {
        val path = "conversations/$conversationId/messages"
        return db.collection("conversations").document(conversationId)
            .collection("messages")
            .whereArrayContains("participantUids", viewerUid)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .limitToLast(100)
            .snapshots()
            .map { snap ->
                val rawList = snap.toObjects(KryptMessage::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                val decrypted = rawList.map { msg ->
                    val plain = if (msg.deleted) {
                        "Message deleted"
                    } else {
                        crypto.decryptDirectMessage(
                            viewerUid = viewerUid,
                            senderUid = msg.senderId,
                            conversationId = conversationId,
                            envelopeOrCiphertext = msg.ciphertext,
                            ivBase64 = msg.iv,
                            ephemeralPubKeyBase64 = msg.ephemeralPubKey
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
                        deliveredToUids = msg.deliveredToUids,
                        readByUids = msg.readByUids,
                        createdAtMillis = msg.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                    )
                }
                // Cache decrypted items locally for offline startup
                localDb.cacheDao().upsertMessages(
                    decrypted.map { d ->
                        CachedMessageEntity(
                            messageId = d.id,
                            threadId = conversationId,
                            isGroup = false,
                            senderId = d.senderId,
                            senderName = d.senderName,
                            plaintext = d.plaintext,
                            messageType = d.messageType.name,
                            mediaName = d.mediaName,
                            mediaMime = d.mediaMime,
                            mediaDataBase64 = d.mediaDataBase64,
                            mediaDurationMs = d.mediaDurationMs,
                            replyToMessageId = d.replyToMessageId,
                            replyToPreview = d.replyToPreview,
                            forwardedFrom = d.forwardedFrom,
                            syncStatus = "SYNCED",
                            createdAtMillis = d.createdAtMillis
                        )
                    }
                )
                decrypted
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, viewerUid)
                throw error
            }
    }

    fun observeLocalThreadMessages(threadId: String): Flow<List<CachedMessageEntity>> =
        localDb.cacheDao().observeMessagesForThread(threadId)

    suspend fun startOrGetConversation(myUid: String, peerUid: String): Result<String> = try {
        require(myUid != peerUid) { "Cannot start conversation with yourself." }
        val sorted = listOf(myUid, peerUid).sorted()
        val convId = "dm_${sorted[0]}_${sorted[1]}"
        val ref = db.collection("conversations").document(convId)
        val snap = ref.get().await()
        if (!snap.exists()) {
            ref.set(
                mapOf(
                    "participantUids" to sorted,
                    "lastMessageCiphertext" to "",
                    "lastMessageIv" to "",
                    "lastMessageEphemeralPub" to "",
                    "lastMessageSenderId" to "",
                    "lastMessagePreviewHint" to "Encrypted session initialized",
                    "pinnedByUids" to emptyList<String>(),
                    "archivedByUids" to emptyList<String>(),
                    "mutedByUids" to emptyList<String>(),
                    "typingUid" to "",
                    "unreadCounts" to mapOf(sorted[0] to 0L, sorted[1] to 0L),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Result.success(convId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "conversations")
        Result.failure(e)
    }

    suspend fun sendDirectMessage(
        conversationId: String,
        participantUids: List<String>,
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
        val msgId = "msg_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val peerUid = participantUids.firstOrNull { it != uid } ?: uid

        // Queue locally first for offline resilience & retry
        localDb.cacheDao().upsertMessage(
            CachedMessageEntity(
                messageId = msgId,
                threadId = conversationId,
                isGroup = false,
                senderId = uid,
                senderName = senderProfile.displayName,
                plaintext = plaintext,
                messageType = messageType.name,
                mediaName = mediaName,
                mediaMime = mediaMime,
                mediaDataBase64 = mediaDataBase64,
                mediaDurationMs = mediaDurationMs,
                replyToMessageId = replyToMessageId,
                replyToPreview = replyToPreview,
                forwardedFrom = forwardedFrom,
                syncStatus = "PENDING_SEND",
                createdAtMillis = System.currentTimeMillis()
            )
        )

        return try {
            val peerKeySnap = db.collection("key_bundles").document(peerUid).get().await()
            val peerPub = peerKeySnap.toObject(KeyBundle::class.java)?.identityKeyPub
            val encrypted = crypto.encryptDirectMessage(
                senderUid = uid,
                recipientPubKeyBase64 = peerPub,
                conversationId = conversationId,
                plaintext = plaintext.ifBlank { mediaName.ifBlank { messageType.name } }
            )

            val msgPayload = mapOf(
                "conversationId" to conversationId,
                "participantUids" to participantUids,
                "senderId" to uid,
                "senderName" to senderProfile.displayName.take(60),
                "ciphertext" to encrypted.ciphertextBase64,
                "iv" to encrypted.ivBase64,
                "ephemeralPubKey" to encrypted.ephemeralPubKeyBase64,
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
                "deliveredToUids" to listOf(uid),
                "readByUids" to listOf(uid),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            val convRef = db.collection("conversations").document(conversationId)
            val msgRef = convRef.collection("messages").document(msgId)
            val convSnap = convRef.get().await()
            val currentUnread = (convSnap.get("unreadCounts.$peerUid") as? Number)?.toLong() ?: 0L

            val hint = when (messageType) {
                MessageType.TEXT -> "Encrypted message"
                MessageType.IMAGE -> "Encrypted image"
                MessageType.VIDEO -> "Encrypted video"
                MessageType.DOCUMENT -> "Encrypted document: ${mediaName.take(40)}"
                MessageType.VOICE -> "Encrypted voice note"
            }

            db.runBatch { batch ->
                batch.set(msgRef, msgPayload)
                batch.update(
                    convRef,
                    mapOf(
                        "lastMessageCiphertext" to encrypted.ciphertextBase64.take(8000),
                        "lastMessageIv" to encrypted.ivBase64,
                        "lastMessageEphemeralPub" to encrypted.ephemeralPubKeyBase64,
                        "lastMessageSenderId" to uid,
                        "lastMessagePreviewHint" to hint,
                        "lastMessageAt" to FieldValue.serverTimestamp(),
                        "typingUid" to "",
                        "unreadCounts.$peerUid" to (currentUnread + 1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            // Create privacy-preserving notification for recipient
            val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(16)}"
            db.collection("users").document(peerUid)
                .collection("notifications").document(notifId)
                .set(
                    mapOf(
                        "userId" to peerUid,
                        "title" to senderProfile.displayName.take(80),
                        "body" to "Sent an end-to-end encrypted ${messageType.name.lowercase()}",
                        "category" to "MESSAGE",
                        "targetType" to "CONVERSATION",
                        "targetId" to conversationId,
                        "read" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

            localDb.cacheDao().upsertMessage(
                CachedMessageEntity(
                    messageId = msgId,
                    threadId = conversationId,
                    isGroup = false,
                    senderId = uid,
                    senderName = senderProfile.displayName,
                    plaintext = plaintext,
                    messageType = messageType.name,
                    mediaName = mediaName,
                    mediaMime = mediaMime,
                    mediaDataBase64 = mediaDataBase64,
                    mediaDurationMs = mediaDurationMs,
                    replyToMessageId = replyToMessageId,
                    replyToPreview = replyToPreview,
                    forwardedFrom = forwardedFrom,
                    syncStatus = "SYNCED",
                    createdAtMillis = System.currentTimeMillis()
                )
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            localDb.cacheDao().upsertMessage(
                CachedMessageEntity(
                    messageId = msgId,
                    threadId = conversationId,
                    isGroup = false,
                    senderId = uid,
                    senderName = senderProfile.displayName,
                    plaintext = plaintext,
                    messageType = messageType.name,
                    mediaName = mediaName,
                    mediaMime = mediaMime,
                    mediaDataBase64 = mediaDataBase64,
                    mediaDurationMs = mediaDurationMs,
                    replyToMessageId = replyToMessageId,
                    replyToPreview = replyToPreview,
                    forwardedFrom = forwardedFrom,
                    syncStatus = "FAILED",
                    createdAtMillis = System.currentTimeMillis()
                )
            )
            handleFirestoreError(e, OperationType.WRITE, "conversations/$conversationId/messages/$msgId")
            Result.failure(e)
        }
    }

    suspend fun markConversationRead(
        conversationId: String,
        uid: String,
        userProfile: UserProfile?,
        messages: List<DecryptedMessageUi>
    ) {
        val ghost = prefs.ghostModeFlow.first()
        if (ghost.isEffectivelyActive() || userProfile?.readReceiptsEnabled == false) {
            // Ghost Mode or read receipts off: NEVER emit read receipts or reset peer-visible read state!
            return
        }
        try {
            db.collection("conversations").document(conversationId)
                .update(
                    mapOf(
                        "unreadCounts.$uid" to 0L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()

            val unreadIncoming = messages.filter { it.senderId != uid && uid !in it.readByUids }.takeLast(20)
            if (unreadIncoming.isNotEmpty()) {
                val batch = db.batch()
                unreadIncoming.forEach { msg ->
                    val ref = db.collection("conversations").document(conversationId)
                        .collection("messages").document(msg.id)
                    batch.update(
                        ref,
                        mapOf(
                            "deliveredToUids" to FieldValue.arrayUnion(uid),
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

    suspend fun setTyping(conversationId: String, uid: String, typing: Boolean) {
        val ghost = prefs.ghostModeFlow.first()
        if (ghost.isEffectivelyActive()) return // Ghost Mode hides typing activity too
        try {
            db.collection("conversations").document(conversationId).update(
                mapOf(
                    "typingUid" to if (typing) uid else "",
                    "typingAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
        }
    }

    suspend fun toggleReaction(conversationId: String, messageId: String, uid: String, emoji: String): Result<Unit> = try {
        val ref = db.collection("conversations").document(conversationId).collection("messages").document(messageId)
        val snap = ref.get().await()
        val existing = (snap.get("reactions.$uid") as? String)
        val nextVal = if (existing == emoji) FieldValue.delete() else emoji
        ref.update(
            mapOf(
                "reactions.$uid" to nextVal,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "conversations/$conversationId/messages/$messageId")
        Result.failure(e)
    }

    suspend fun editDirectMessage(
        conversationId: String,
        participantUids: List<String>,
        messageId: String,
        newPlaintext: String
    ): Result<Unit> = try {
        val uid = requireUid()
        val peerUid = participantUids.firstOrNull { it != uid } ?: uid
        val peerPub = db.collection("key_bundles").document(peerUid).get().await()
            .toObject(KeyBundle::class.java)?.identityKeyPub
        val encrypted = crypto.encryptDirectMessage(uid, peerPub, conversationId, newPlaintext)
        db.collection("conversations").document(conversationId).collection("messages").document(messageId)
            .update(
                mapOf(
                    "ciphertext" to encrypted.ciphertextBase64,
                    "iv" to encrypted.ivBase64,
                    "ephemeralPubKey" to encrypted.ephemeralPubKeyBase64,
                    "edited" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "conversations/$conversationId/messages/$messageId")
        Result.failure(e)
    }

    suspend fun deleteDirectMessage(conversationId: String, messageId: String): Result<Unit> = try {
        db.collection("conversations").document(conversationId).collection("messages").document(messageId)
            .update(
                mapOf(
                    "deleted" to true,
                    "ciphertext" to "deleted",
                    "mediaDataBase64" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "conversations/$conversationId/messages/$messageId")
        Result.failure(e)
    }

    suspend fun togglePinDirectMessage(conversationId: String, messageId: String, pinned: Boolean): Result<Unit> = try {
        db.collection("conversations").document(conversationId).collection("messages").document(messageId)
            .update(mapOf("pinned" to pinned, "updatedAt" to FieldValue.serverTimestamp())).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "conversations/$conversationId/messages/$messageId")
        Result.failure(e)
    }

    suspend fun toggleConversationFlag(conversationId: String, field: String, uid: String, active: Boolean): Result<Unit> = try {
        val op = if (active) FieldValue.arrayUnion(uid) else FieldValue.arrayRemove(uid)
        db.collection("conversations").document(conversationId)
            .update(mapOf(field to op, "updatedAt" to FieldValue.serverTimestamp())).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "conversations/$conversationId")
        Result.failure(e)
    }

    suspend fun setUserBlocked(myUid: String, targetUid: String, block: Boolean): Result<Unit> = try {
        val op = if (block) FieldValue.arrayUnion(targetUid) else FieldValue.arrayRemove(targetUid)
        db.collection("users").document(myUid).collection("private").document("account")
            .update(mapOf("blockedUids" to op, "updatedAt" to FieldValue.serverTimestamp())).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$myUid/private/account")
        Result.failure(e)
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
