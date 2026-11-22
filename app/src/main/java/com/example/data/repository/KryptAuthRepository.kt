package com.example.data.repository

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.crypto.KryptCryptoEngine
import com.example.data.local.GhostModeState
import com.example.data.local.KryptDatabase
import com.example.data.local.KryptPreferences
import com.example.data.model.AccountStatus
import com.example.data.model.PlatformSettings
import com.example.data.model.UserPrivateAccount
import com.example.data.model.UserProfile
import com.example.data.model.UserSession
import com.example.data.remote.OperationType
import com.example.data.remote.handleFirestoreError
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Date

class KryptAuthRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth,
    val crypto: KryptCryptoEngine,
    val prefs: KryptPreferences,
    private val localDb: KryptDatabase
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(context.applicationContext.getString(R.string.firestore_database_id)),
        auth = FirebaseAuth.getInstance(),
        crypto = KryptCryptoEngine(context.applicationContext),
        prefs = KryptPreferences(context.applicationContext),
        localDb = KryptDatabase.getInstance(context.applicationContext)
    )

    fun currentUserId(): String? = auth.currentUser?.uid
    fun currentUserEmail(): String? = auth.currentUser?.email

    fun isBootstrappedOwner(): Boolean {
        val user = auth.currentUser ?: return false
        return user.isEmailVerified && user.email.equals(OWNER_EMAIL, ignoreCase = true)
    }

    fun observeUserProfile(uid: String): Flow<UserProfile?> {
        val path = "users/$uid"
        return db.collection("users").document(uid)
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, path, uid)
                throw error
            }
    }

    fun observePrivateAccount(uid: String): Flow<UserPrivateAccount?> {
        val path = "users/$uid/private/account"
        return db.collection("users").document(uid)
            .collection("private").document("account")
            .snapshots()
            .map { snap ->
                if (!snap.exists()) null
                else snap.toObject(UserPrivateAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, path, uid)
                throw error
            }
    }

    fun observeUserDirectory(): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection("users")
            .snapshots()
            .map { snap ->
                snap.toObjects(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .filter { it.accountStatus != AccountStatus.BANNED.name }
                    .sortedBy { it.displayName.lowercase() }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, auth.currentUser?.uid)
                throw error
            }
    }

    fun observeSessions(uid: String): Flow<List<UserSession>> {
        val path = "users/$uid/sessions"
        return db.collection("users").document(uid).collection("sessions")
            .snapshots()
            .map { snap ->
                snap.toObjects(UserSession::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.lastActiveAt?.toDate()?.time ?: 0L }
            }
            .catch { error ->
                logFlowError(error, OperationType.LIST, path, uid)
                throw error
            }
    }

    fun observePlatformSettings(): Flow<PlatformSettings> {
        val path = "platform_settings/global"
        return db.collection("platform_settings").document("global")
            .snapshots()
            .map { snap ->
                if (snap.exists()) {
                    snap.toObject(PlatformSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        ?: PlatformSettings()
                } else {
                    PlatformSettings()
                }
            }
            .catch { error ->
                logFlowError(error, OperationType.GET, path, auth.currentUser?.uid)
                emit(PlatformSettings())
            }
    }

    fun observeGhostMode(): Flow<GhostModeState> = prefs.ghostModeFlow

    suspend fun ensureSessionAndKeys(uid: String): Result<Unit> = try {
        val bundle = crypto.ensureLocalIdentityKeys(uid)
        val keyRef = db.collection("key_bundles").document(uid)
        val existingKey = keyRef.get().await()
        if (!existingKey.exists()) {
            keyRef.set(
                mapOf(
                    "userId" to uid,
                    "identityKeyPub" to bundle.identityPubBase64,
                    "signedPreKeyPub" to bundle.signedPreKeyPubBase64,
                    "signedPreKeyId" to bundle.signedPreKeyId,
                    "fingerprint" to bundle.fingerprint,
                    "oneTimePreKeys" to bundle.oneTimePreKeys,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            keyRef.update(
                mapOf(
                    "identityKeyPub" to bundle.identityPubBase64,
                    "signedPreKeyPub" to bundle.signedPreKeyPubBase64,
                    "fingerprint" to bundle.fingerprint,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }

        val sessionId = prefs.getOrCreateDeviceSessionId()
        val sessionRef = db.collection("users").document(uid).collection("sessions").document(sessionId)
        val existingSession = sessionRef.get().await()
        if (!existingSession.exists()) {
            sessionRef.set(
                mapOf(
                    "userId" to uid,
                    "deviceName" to "${Build.MANUFACTURER} ${Build.MODEL}",
                    "deviceModel" to Build.DEVICE,
                    "platform" to "Android ${Build.VERSION.RELEASE}",
                    "appVersion" to "1.0",
                    "revoked" to false,
                    "lastActiveAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } else {
            sessionRef.update(
                mapOf(
                    "lastActiveAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "key_bundles/$uid")
        Result.failure(e)
    }

    suspend fun bootstrapOwnerProfileIfEligible(username: String, displayName: String, bio: String): Result<Unit> = try {
        val user = auth.currentUser ?: error("Not signed in")
        require(isBootstrappedOwner()) { "Not authorized as platform Owner" }
        val uid = user.uid
        val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_\\-]"), "")
        require(cleanUsername.length in 3..30) { "Username must be 3-30 valid characters" }

        val userRef = db.collection("users").document(uid)
        val privRef = userRef.collection("private").document("account")
        val adminRef = db.collection("admins").document(uid)
        val settingsRef = db.collection("platform_settings").document("global")

        db.runBatch { batch ->
            batch.set(
                userRef,
                mapOf(
                    "userId" to uid,
                    "username" to cleanUsername,
                    "displayName" to displayName.trim().ifBlank { "KRYPT Owner" }.take(50),
                    "bio" to bio.trim().take(240),
                    "accountStatus" to "ACTIVE",
                    "role" to "OWNER",
                    "onlineVisible" to true,
                    "ghostModeActive" to false,
                    "ghostModeUntilEpochMs" to 0L,
                    "readReceiptsEnabled" to true,
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                privRef,
                mapOf(
                    "userId" to uid,
                    "email" to (user.email ?: OWNER_EMAIL),
                    "passHashUsed" to "OWNER_BOOTSTRAP",
                    "blockedUids" to emptyList<String>(),
                    "notifyMessages" to true,
                    "notifyGroups" to true,
                    "notifyCalls" to true,
                    "notifyAnnouncements" to true,
                    "hideLockScreenPreview" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                adminRef,
                mapOf(
                    "userId" to uid,
                    "email" to (user.email ?: OWNER_EMAIL),
                    "role" to "OWNER",
                    "grantedBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                settingsRef,
                mapOf(
                    "maintenanceMode" to false,
                    "registrationEnabled" to true,
                    "requirePassForSignup" to true,
                    "maxPassAttemptsPerHour" to 8L,
                    "announcementBanner" to "",
                    "updatedBy" to uid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()

        ensureSessionAndKeys(uid)
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.WRITE, "users/${auth.currentUser?.uid}")
        Result.failure(e)
    }

    suspend fun redeemAccessPassAndCreateProfile(
        rawPassCode: String,
        username: String,
        displayName: String,
        bio: String
    ): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(IllegalStateException("Sign in with Google first."))
        val uid = user.uid
        val cooldownUntil = prefs.passCooldownFlow.first()
        if (System.currentTimeMillis() < cooldownUntil) {
            val sec = ((cooldownUntil - System.currentTimeMillis()) / 1000L).coerceAtLeast(1L)
            return Result.failure(IllegalStateException("Too many failed attempts. Retry in ${sec}s."))
        }

        val cleanCode = rawPassCode.trim().uppercase()
        if (cleanCode.length < 6) {
            return Result.failure(IllegalArgumentException("Enter a valid Special Access Pass code."))
        }
        val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_\\-]"), "")
        if (cleanUsername.length !in 3..30) {
            return Result.failure(IllegalArgumentException("Username must be 3-30 letters, numbers, _ or -."))
        }
        val cleanDisplay = displayName.trim()
        if (cleanDisplay.isEmpty() || cleanDisplay.length > 50) {
            return Result.failure(IllegalArgumentException("Display name must be 1-50 characters."))
        }

        val passHash = crypto.normalizeAndHashPass(cleanCode)
        return try {
            val settingsSnap = db.collection("platform_settings").document("global").get().await()
            if (settingsSnap.exists() && settingsSnap.getBoolean("registrationEnabled") == false) {
                return Result.failure(IllegalStateException("New member registration is currently paused by the Owner."))
            }

            val passRef = db.collection("access_passes").document(passHash)
            val redemptionRef = db.collection("access_pass_redemptions").document("${passHash}_$uid")
            val userRef = db.collection("users").document(uid)
            val privRef = userRef.collection("private").document("account")

            db.runTransaction { tx ->
                val passSnap = tx.get(passRef)
                if (!passSnap.exists()) {
                    throw IllegalArgumentException("Invalid Special Access Pass.")
                }
                val revoked = passSnap.getBoolean("revoked") ?: false
                if (revoked) {
                    throw IllegalStateException("This Special Access Pass has been revoked.")
                }
                val maxUses = passSnap.getLong("maxUses") ?: 1L
                val usedCount = passSnap.getLong("usedCount") ?: 0L
                if (usedCount >= maxUses) {
                    throw IllegalStateException("This Special Access Pass has reached its usage limit.")
                }
                val expiresAt = passSnap.getTimestamp("expiresAt")
                if (expiresAt != null && expiresAt.toDate().before(Date())) {
                    throw IllegalStateException("This Special Access Pass has expired.")
                }
                val existingRedemption = tx.get(redemptionRef)
                if (existingRedemption.exists()) {
                    throw IllegalStateException("This pass was already redeemed by your account.")
                }

                tx.update(
                    passRef,
                    mapOf(
                        "usedCount" to usedCount + 1L,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    redemptionRef,
                    mapOf(
                        "passHash" to passHash,
                        "userId" to uid,
                        "username" to cleanUsername,
                        "redeemedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    userRef,
                    mapOf(
                        "userId" to uid,
                        "username" to cleanUsername,
                        "displayName" to cleanDisplay,
                        "bio" to bio.trim().take(240),
                        "accountStatus" to "ACTIVE",
                        "role" to if (isBootstrappedOwner()) "OWNER" else "MEMBER",
                        "onlineVisible" to true,
                        "ghostModeActive" to false,
                        "ghostModeUntilEpochMs" to 0L,
                        "readReceiptsEnabled" to true,
                        "lastSeenAt" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                tx.set(
                    privRef,
                    mapOf(
                        "userId" to uid,
                        "email" to (user.email ?: ""),
                        "passHashUsed" to passHash,
                        "blockedUids" to emptyList<String>(),
                        "notifyMessages" to true,
                        "notifyGroups" to true,
                        "notifyCalls" to true,
                        "notifyAnnouncements" to true,
                        "hideLockScreenPreview" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                null
            }.await()

            prefs.clearFailedPassAttempts()
            ensureSessionAndKeys(uid)
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            prefs.recordFailedPassAttempt()
            handleFirestoreError(e, OperationType.WRITE, "access_passes/$passHash")
            Result.failure(e)
        }
    }

    suspend fun updateProfile(
        uid: String,
        username: String,
        displayName: String,
        bio: String,
        avatarBase64: String?
    ): Result<Unit> = try {
        val currentUid = auth.currentUser?.uid ?: error("Not signed in")
        check(currentUid == uid)
        val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_\\-]"), "")
        require(cleanUsername.length in 3..30) { "Username must be 3-30 characters" }
        require(displayName.trim().length in 1..50) { "Display name must be 1-50 characters" }

        val updates = mutableMapOf<String, Any>(
            "username" to cleanUsername,
            "displayName" to displayName.trim(),
            "bio" to bio.trim().take(240),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (!avatarBase64.isNullOrBlank() && avatarBase64.length <= 150_000) {
            updates["avatarBase64"] = avatarBase64
        }
        db.collection("users").document(uid).update(updates).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
        Result.failure(e)
    }

    suspend fun updateGhostMode(
        uid: String,
        enabled: Boolean,
        durationMs: Long?,
        label: String
    ): Result<Unit> = try {
        prefs.setGhostMode(enabled, durationMs, label)
        val untilEpoch = if (enabled && durationMs != null) System.currentTimeMillis() + durationMs else 0L
        db.collection("users").document(uid).update(
            mapOf(
                "ghostModeActive" to enabled,
                "ghostModeUntilEpochMs" to untilEpoch,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
        Result.failure(e)
    }

    suspend fun updatePresence(uid: String, online: Boolean) {
        val ghost = prefs.ghostModeFlow.first()
        if (ghost.isEffectivelyActive()) {
            // Ghost Mode active: strictly suppress presence and last-seen updates!
            return
        }
        try {
            db.collection("users").document(uid).update(
                mapOf(
                    "lastSeenAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
            // Presence heartbeat failure is non-fatal
        }
    }

    suspend fun updatePrivacyAndNotificationSettings(
        uid: String,
        onlineVisible: Boolean,
        readReceiptsEnabled: Boolean,
        hideLockScreenPreview: Boolean,
        notifyMessages: Boolean,
        notifyGroups: Boolean,
        notifyCalls: Boolean,
        notifyAnnouncements: Boolean
    ): Result<Unit> = try {
        prefs.setHideLockScreen(hideLockScreenPreview)
        db.collection("users").document(uid).update(
            mapOf(
                "onlineVisible" to onlineVisible,
                "readReceiptsEnabled" to readReceiptsEnabled,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        db.collection("users").document(uid).collection("private").document("account").update(
            mapOf(
                "hideLockScreenPreview" to hideLockScreenPreview,
                "notifyMessages" to notifyMessages,
                "notifyGroups" to notifyGroups,
                "notifyCalls" to notifyCalls,
                "notifyAnnouncements" to notifyAnnouncements,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid/private/account")
        Result.failure(e)
    }

    suspend fun revokeSession(uid: String, sessionId: String): Result<Unit> = try {
        db.collection("users").document(uid).collection("sessions").document(sessionId)
            .update(
                mapOf(
                    "revoked" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid/sessions/$sessionId")
        Result.failure(e)
    }

    suspend fun signOutSafely(credentialManager: CredentialManager) {
        withContext(NonCancellable) {
            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Credential clear failed: ${e.message}")
            }
            try {
                localDb.cacheDao().clearMessages()
                localDb.cacheDao().clearConversations()
            } catch (_: Exception) {
            }
            auth.signOut()
        }
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

    companion object {
        const val OWNER_EMAIL = "anuragsinghgjp@gmail.com"
    }
}
