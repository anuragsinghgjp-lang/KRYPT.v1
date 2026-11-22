package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.crypto.KryptCryptoEngine
import com.example.data.model.AccessPass
import com.example.data.model.AdminAuditLog
import com.example.data.model.AdminRecord
import com.example.data.model.Announcement
import com.example.data.model.CallSession
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.KryptGroup
import com.example.data.model.KryptMessage
import com.example.data.model.ModerationAction
import com.example.data.model.OwnerPassVault
import com.example.data.model.OwnerStats
import com.example.data.model.PlatformSettings
import com.example.data.model.SafetyReport
import com.example.data.model.UserNotification
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class KryptSafetyAndOwnerRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val crypto: KryptCryptoEngine
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id)),
        auth = FirebaseAuth.getInstance(),
        crypto = KryptCryptoEngine(context.applicationContext)
    )

    private fun requireUid(): String =
        auth.currentUser?.uid ?: throw IllegalStateException("Authentication required.")

    // --- NOTIFICATIONS ---
    fun observeNotifications(uid: String): Flow<List<UserNotification>> {
        val path = "users/$uid/notifications"
        return db.collection("users").document(uid).collection("notifications")
            .snapshots()
            .map { snap ->
                snap.toObjects(UserNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, uid)
                throw error
            }
    }

    suspend fun markNotificationRead(uid: String, notifId: String): Result<Unit> = try {
        db.collection("users").document(uid).collection("notifications").document(notifId)
            .update(mapOf("read" to true, "updatedAt" to FieldValue.serverTimestamp())).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid/notifications/$notifId")
        Result.failure(e)
    }

    // --- ANNOUNCEMENTS ---
    fun observeActiveAnnouncements(): Flow<List<Announcement>> {
        val path = "announcements"
        return db.collection("announcements")
            .snapshots()
            .map { snap ->
                snap.toObjects(Announcement::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.active }
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, auth.currentUser?.uid)
                throw error
            }
    }

    // --- CALLS (WEBRTC SIGNALING) ---
    fun observeCalls(uid: String): Flow<List<CallSession>> {
        val path = "calls"
        return db.collection("calls")
            .whereArrayContains("participantUids", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, uid)
                throw error
            }
    }

    fun observeCall(callId: String): Flow<CallSession?> {
        val path = "calls/$callId"
        return db.collection("calls").document(callId)
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(CallSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, path, auth.currentUser?.uid)
                throw error
            }
    }

    suspend fun initiateCall(
        caller: UserProfile,
        callee: UserProfile,
        callType: CallType
    ): Result<String> = try {
        val callId = "call_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val sdpOfferStub = "v=0\r\no=krypt-${caller.userId.take(8)} 1 2 IN IP4 127.0.0.1\r\ns=KRYPT-${callType.name}-DTLS-SRTP"
        db.collection("calls").document(callId).set(
            mapOf(
                "callerId" to caller.userId,
                "callerName" to caller.displayName.take(60),
                "calleeId" to callee.userId,
                "calleeName" to callee.displayName.take(60),
                "participantUids" to listOf(caller.userId, callee.userId),
                "callType" to callType.name,
                "status" to CallStatus.RINGING.name,
                "sdpOffer" to sdpOfferStub,
                "sdpAnswer" to "",
                "iceCandidates" to listOf("candidate:1 1 udp 2122260223 10.0.0.1 54400 typ host"),
                "durationSeconds" to 0L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        db.collection("users").document(callee.userId).collection("notifications").document(notifId)
            .set(
                mapOf(
                    "userId" to callee.userId,
                    "title" to "Incoming ${callType.name.lowercase()} call",
                    "body" to "${caller.displayName} is calling via encrypted signaling",
                    "category" to "CALL",
                    "targetType" to "CALL",
                    "targetId" to callId,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        Result.success(callId)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "calls")
        Result.failure(e)
    }

    suspend fun updateCallStatus(callId: String, status: CallStatus, durationSeconds: Long = 0L): Result<Unit> = try {
        val updates = mutableMapOf<String, Any>(
            "status" to status.name,
            "durationSeconds" to durationSeconds.coerceAtLeast(0L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (status == CallStatus.ACTIVE) {
            updates["sdpAnswer"] = "v=0\r\no=krypt-ans 1 2 IN IP4 127.0.0.1\r\na=fingerprint:sha-256"
        }
        db.collection("calls").document(callId).update(updates).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "calls/$callId")
        Result.failure(e)
    }

    // --- SAFETY ENGINE ---
    fun detectSpamOrRepetition(recentMessages: List<String>, candidate: String): String? {
        val trimmed = candidate.trim()
        if (trimmed.isEmpty()) return null
        val repeats = recentMessages.takeLast(5).count { it.trim().equals(trimmed, ignoreCase = true) }
        if (repeats >= 3) return "Repeated identical message pattern flagged for review"
        if (trimmed.length > 1500 && trimmed.count { it == '!' || it == '?' } > 25) {
            return "Potential spam burst pattern detected"
        }
        return null
    }

    fun observeMyReports(uid: String): Flow<List<SafetyReport>> {
        val path = "reports"
        return db.collection("reports")
            .whereEqualTo("reporterId", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(SafetyReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, uid)
                throw error
            }
    }

    fun observeModerationActionsAgainstMe(uid: String): Flow<List<ModerationAction>> {
        val path = "moderation_actions"
        return db.collection("moderation_actions")
            .whereEqualTo("targetUserId", uid)
            .snapshots()
            .map { snap ->
                snap.toObjects(ModerationAction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, uid)
                throw error
            }
    }

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        targetUserId: String = "",
        groupId: String = "",
        conversationId: String = "",
        reason: String,
        details: String,
        evidenceSnapshot: String = ""
    ): Result<Unit> = try {
        val uid = requireUid()
        val reportId = "rep_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        db.collection("reports").document(reportId).set(
            mapOf(
                "reporterId" to uid,
                "targetType" to targetType,
                "targetId" to targetId,
                "targetUserId" to targetUserId.take(128),
                "groupId" to groupId.take(128),
                "conversationId" to conversationId.take(128),
                "reason" to reason.trim().take(120),
                "details" to details.trim().take(1000),
                "evidenceSnapshot" to evidenceSnapshot.take(2000),
                "status" to "PENDING",
                "appealText" to "",
                "moderatorNote" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "reports")
        Result.failure(e)
    }

    suspend fun submitAppeal(reportId: String, appealText: String): Result<Unit> = try {
        require(appealText.trim().isNotEmpty()) { "Please enter your appeal explanation." }
        db.collection("reports").document(reportId).update(
            mapOf(
                "appealText" to appealText.trim().take(1000),
                "status" to "APPEALED",
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "reports/$reportId")
        Result.failure(e)
    }

    // --- OWNER CONTROL CENTER ---
    fun observeAllUsers(): Flow<List<UserProfile>> =
        db.collection("users").snapshots()
            .map { it.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "users", auth.currentUser?.uid); throw error }

    fun observeAllGroups(): Flow<List<KryptGroup>> =
        db.collection("groups").snapshots()
            .map { it.toObjects(KryptGroup::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "groups", auth.currentUser?.uid); throw error }

    fun observeAllReports(): Flow<List<SafetyReport>> =
        db.collection("reports").snapshots()
            .map { snap ->
                snap.toObjects(SafetyReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error -> logFlowError(error, OperationType.LIST, "reports", auth.currentUser?.uid); throw error }

    fun observeAccessPasses(): Flow<List<AccessPass>> =
        db.collection("access_passes").snapshots()
            .map { snap ->
                snap.toObjects(AccessPass::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error -> logFlowError(error, OperationType.LIST, "access_passes", auth.currentUser?.uid); throw error }

    fun observeOwnerPassVault(): Flow<List<OwnerPassVault>> =
        db.collection("owner_pass_vault").snapshots()
            .map { snap ->
                snap.toObjects(OwnerPassVault::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.toDate()?.time ?: 0L }
            }
            .catch { error -> logFlowError(error, OperationType.LIST, "owner_pass_vault", auth.currentUser?.uid); throw error }

    fun observeAuditLogs(): Flow<List<AdminAuditLog>> =
        db.collection("admin_audit_logs")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(100)
            .snapshots()
            .map { it.toObjects(AdminAuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "admin_audit_logs", auth.currentUser?.uid); throw error }

    fun observeAdmins(): Flow<List<AdminRecord>> =
        db.collection("admins").snapshots()
            .map { it.toObjects(AdminRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .catch { error -> logFlowError(error, OperationType.LIST, "admins", auth.currentUser?.uid); throw error }

    suspend fun computeOwnerStats(): Result<OwnerStats> = try {
        val users = db.collection("users").get().await().toObjects(UserProfile::class.java)
        val groups = db.collection("groups").get().await()
        val convs = db.collection("conversations").get().await()
        val reports = db.collection("reports").get().await().toObjects(SafetyReport::class.java)
        val passes = db.collection("access_passes").get().await().toObjects(AccessPass::class.java)

        Result.success(
            OwnerStats(
                totalMembers = users.size,
                activeMembers = users.count { it.accountStatus == "ACTIVE" },
                totalGroups = groups.size(),
                totalConversations = convs.size(),
                totalMessages = groups.size() + convs.size(),
                pendingReports = reports.count { it.status == "PENDING" || it.status == "APPEALED" },
                suspendedAccounts = users.count { it.accountStatus == "SUSPENDED" || it.accountStatus == "BANNED" },
                activePasses = passes.count { !it.revoked && it.usedCount < it.maxUses }
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.LIST, "owner_stats")
        Result.failure(e)
    }

    suspend fun generateAccessPass(
        label: String,
        passType: String,
        maxUses: Long,
        validDays: Int
    ): Result<String> = try {
        val uid = requireUid()
        val rawCode = crypto.generateAccessPassCode()
        val passHash = crypto.normalizeAndHashPass(rawCode)
        val expiresAt = Timestamp(Date(System.currentTimeMillis() + validDays.coerceIn(1, 365) * 86_400_000L))
        val effectiveUses = if (passType == "SINGLE_USE") 1L else maxUses.coerceIn(1L, 10_000L)

        db.runBatch { batch ->
            batch.set(
                db.collection("access_passes").document(passHash),
                mapOf(
                    "passHash" to passHash,
                    "label" to label.trim().ifBlank { "Member Pass" }.take(60),
                    "passType" to passType,
                    "maxUses" to effectiveUses,
                    "usedCount" to 0L,
                    "revoked" to false,
                    "expiresAt" to expiresAt,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                db.collection("owner_pass_vault").document(passHash),
                mapOf(
                    "passHash" to passHash,
                    "passCode" to rawCode,
                    "label" to label.trim().ifBlank { "Member Pass" }.take(60),
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            appendAuditLog(
                batch = batch,
                action = "GENERATE_ACCESS_PASS",
                targetType = "ACCESS_PASS",
                targetId = passHash,
                reason = "Issued ${passType.lowercase()} pass ($effectiveUses uses)",
                metadata = "label=$label"
            )
        }.await()
        Result.success(rawCode)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "access_passes")
        Result.failure(e)
    }

    suspend fun revokeAccessPass(passHash: String, reason: String): Result<Unit> = try {
        db.runBatch { batch ->
            batch.update(
                db.collection("access_passes").document(passHash),
                mapOf("revoked" to true, "updatedAt" to FieldValue.serverTimestamp())
            )
            appendAuditLog(batch, "REVOKE_ACCESS_PASS", "ACCESS_PASS", passHash, reason.ifBlank { "Owner revoked pass" })
        }.await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "access_passes/$passHash")
        Result.failure(e)
    }

    suspend fun moderateUserAccount(
        targetUid: String,
        actionType: String, // WARN, SUSPEND, BAN, RESTORE
        reason: String,
        reportId: String = ""
    ): Result<Unit> = try {
        val uid = requireUid()
        val nextStatus = when (actionType) {
            "WARN" -> "WARNED"
            "SUSPEND" -> "SUSPENDED"
            "BAN" -> "BANNED"
            "RESTORE" -> "ACTIVE"
            else -> "ACTIVE"
        }
        val actionId = "act_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val notifId = "notif_${UUID.randomUUID().toString().replace("-", "").take(16)}"

        db.runBatch { batch ->
            batch.update(
                db.collection("users").document(targetUid),
                mapOf("accountStatus" to nextStatus, "updatedAt" to FieldValue.serverTimestamp())
            )
            batch.set(
                db.collection("moderation_actions").document(actionId),
                mapOf(
                    "targetUserId" to targetUid,
                    "targetGroupId" to "",
                    "actionType" to actionType,
                    "reason" to reason.trim().ifBlank { "Community safety enforcement" }.take(500),
                    "moderatorId" to uid,
                    "reportId" to reportId,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            if (reportId.isNotBlank()) {
                batch.update(
                    db.collection("reports").document(reportId),
                    mapOf(
                        "status" to "RESOLVED",
                        "moderatorNote" to reason.take(500),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.set(
                db.collection("users").document(targetUid).collection("notifications").document(notifId),
                mapOf(
                    "userId" to targetUid,
                    "title" to "Account Moderation Notice ($actionType)",
                    "body" to reason.take(400),
                    "category" to "MODERATION",
                    "targetType" to "REPORT",
                    "targetId" to reportId,
                    "read" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            appendAuditLog(batch, "MODERATE_USER_$actionType", "USER", targetUid, reason)
        }.await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$targetUid")
        Result.failure(e)
    }

    suspend fun governGroupAsOwner(
        groupId: String,
        locked: Boolean? = null,
        suspended: Boolean? = null,
        reason: String
    ): Result<Unit> = try {
        val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
        if (locked != null) updates["locked"] = locked
        if (suspended != null) updates["suspended"] = suspended
        db.runBatch { batch ->
            batch.update(db.collection("groups").document(groupId), updates)
            appendAuditLog(
                batch = batch,
                action = "OWNER_GROUP_GOVERNANCE",
                targetType = "GROUP",
                targetId = groupId,
                reason = reason.ifBlank { "Owner community safety control" },
                metadata = "locked=$locked,suspended=$suspended"
            )
        }.await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "groups/$groupId")
        Result.failure(e)
    }

    suspend fun inspectGroupMessagesWithAudit(groupId: String, reason: String): Result<List<KryptMessage>> = try {
        require(reason.trim().length >= 5) { "Audit reason (min 5 chars) is mandatory for authorized review." }
        val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val uid = requireUid()
        val email = auth.currentUser?.email ?: KryptAuthRepository.OWNER_EMAIL
        db.collection("admin_audit_logs").document(logId).set(
            mapOf(
                "adminId" to uid,
                "adminEmail" to email,
                "action" to "AUTHORIZED_MESSAGE_REVIEW",
                "targetType" to "GROUP",
                "targetId" to groupId,
                "reason" to reason.trim().take(500),
                "metadata" to "Safety investigation access",
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
        val msgs = db.collection("groups").document(groupId).collection("messages")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(50)
            .get().await()
            .toObjects(KryptMessage::class.java)
        Result.success(msgs)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.LIST, "groups/$groupId/messages")
        Result.failure(e)
    }

    suspend fun removeGroupMessageAsOwner(groupId: String, messageId: String, reason: String): Result<Unit> = try {
        db.runBatch { batch ->
            batch.update(
                db.collection("groups").document(groupId).collection("messages").document(messageId),
                mapOf(
                    "deleted" to true,
                    "ciphertext" to "removed_by_moderation",
                    "mediaDataBase64" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            appendAuditLog(batch, "REMOVE_MESSAGE", "MESSAGE", "$groupId/$messageId", reason)
        }.await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "groups/$groupId/messages/$messageId")
        Result.failure(e)
    }

    suspend fun resolveReportStatus(reportId: String, status: String, note: String): Result<Unit> = try {
        db.runBatch { batch ->
            batch.update(
                db.collection("reports").document(reportId),
                mapOf(
                    "status" to status,
                    "moderatorNote" to note.take(500),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            appendAuditLog(batch, "REVIEW_REPORT_$status", "REPORT", reportId, note.ifBlank { "Moderation review" })
        }.await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "reports/$reportId")
        Result.failure(e)
    }

    suspend fun publishAnnouncement(title: String, body: String, priority: String): Result<Unit> = try {
        val uid = requireUid()
        val id = "ann_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        db.runBatch { batch ->
            batch.set(
                db.collection("announcements").document(id),
                mapOf(
                    "title" to title.trim().take(120),
                    "body" to body.trim().take(2000),
                    "priority" to priority,
                    "authorId" to uid,
                    "active" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            appendAuditLog(batch, "PUBLISH_ANNOUNCEMENT", "ANNOUNCEMENT", id, title)
        }.await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.CREATE, "announcements")
        Result.failure(e)
    }

    suspend fun updatePlatformSettings(settings: PlatformSettings, reason: String): Result<Unit> = try {
        val uid = requireUid()
        db.runBatch { batch ->
            batch.set(
                db.collection("platform_settings").document("global"),
                mapOf(
                    "maintenanceMode" to settings.maintenanceMode,
                    "registrationEnabled" to settings.registrationEnabled,
                    "requirePassForSignup" to settings.requirePassForSignup,
                    "maxPassAttemptsPerHour" to settings.maxPassAttemptsPerHour.coerceIn(1L, 100L),
                    "announcementBanner" to settings.announcementBanner.take(300),
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            appendAuditLog(batch, "UPDATE_PLATFORM_SETTINGS", "PLATFORM", "global", reason.ifBlank { "Settings updated" })
        }.await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "platform_settings/global")
        Result.failure(e)
    }

    suspend fun toggleSafetyAdmin(targetUid: String, email: String, grant: Boolean, reason: String): Result<Unit> = try {
        val uid = requireUid()
        db.runBatch { batch ->
            val ref = db.collection("admins").document(targetUid)
            if (grant) {
                batch.set(
                    ref,
                    mapOf(
                        "userId" to targetUid,
                        "email" to email.ifBlank { "$targetUid@krypt.member" },
                        "role" to "SAFETY_ADMIN",
                        "grantedBy" to uid,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                batch.delete(ref)
            }
            appendAuditLog(
                batch,
                if (grant) "GRANT_SAFETY_ADMIN" else "REVOKE_SAFETY_ADMIN",
                "ADMIN",
                targetUid,
                reason
            )
        }.await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "admins/$targetUid")
        Result.failure(e)
    }

    private fun appendAuditLog(
        batch: com.google.firebase.firestore.WriteBatch,
        action: String,
        targetType: String,
        targetId: String,
        reason: String,
        metadata: String = ""
    ) {
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: KryptAuthRepository.OWNER_EMAIL
        val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        batch.set(
            db.collection("admin_audit_logs").document(logId),
            mapOf(
                "adminId" to uid,
                "adminEmail" to email,
                "action" to action.take(80),
                "targetType" to targetType.take(40),
                "targetId" to targetId.take(128),
                "reason" to reason.ifBlank { "Administrative action" }.take(500),
                "metadata" to metadata.take(1000),
                "createdAt" to FieldValue.serverTimestamp()
            )
        )
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
