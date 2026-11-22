package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AccountStatus {
    ACTIVE, WARNED, SUSPENDED, BANNED;
    companion object {
        fun safeValueOf(raw: String?): AccountStatus =
            entries.firstOrNull { it.name == raw } ?: ACTIVE
    }
}

enum class PlatformRole {
    OWNER, MEMBER;
    companion object {
        fun safeValueOf(raw: String?): PlatformRole =
            entries.firstOrNull { it.name == raw } ?: MEMBER
    }
}

enum class GroupRole {
    OWNER, ADMIN, MODERATOR, MEMBER;
    companion object {
        fun safeValueOf(raw: String?): GroupRole =
            entries.firstOrNull { it.name == raw } ?: MEMBER
    }
}

enum class MessageType {
    TEXT, IMAGE, VIDEO, DOCUMENT, VOICE;
    companion object {
        fun safeValueOf(raw: String?): MessageType =
            entries.firstOrNull { it.name == raw } ?: TEXT
    }
}

enum class CallType {
    VOICE, VIDEO;
    companion object {
        fun safeValueOf(raw: String?): CallType =
            entries.firstOrNull { it.name == raw } ?: VOICE
    }
}

enum class CallStatus {
    RINGING, ACTIVE, ENDED, MISSED, DECLINED;
    companion object {
        fun safeValueOf(raw: String?): CallStatus =
            entries.firstOrNull { it.name == raw } ?: ENDED
    }
}

enum class GhostDurationOption(val label: String, val durationMs: Long?) {
    FIFTEEN_MIN("15 Minutes", 15L * 60L * 1000L),
    ONE_HOUR("1 Hour", 60L * 60L * 1000L),
    EIGHT_HOURS("8 Hours", 8L * 60L * 60L * 1000L),
    TWENTY_FOUR_HOURS("24 Hours", 24L * 60L * 60L * 1000L),
    MANUAL("Until Turned Off", null)
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    val bio: String = "",
    val avatarBase64: String = "",
    val accountStatus: String = "ACTIVE",
    val role: String = "MEMBER",
    val onlineVisible: Boolean = true,
    val ghostModeActive: Boolean = false,
    val ghostModeUntilEpochMs: Long = 0L,
    val readReceiptsEnabled: Boolean = true,
    val lastSeenAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserPrivateAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val passHashUsed: String = "",
    val blockedUids: List<String> = emptyList(),
    val notifyMessages: Boolean = true,
    val notifyGroups: Boolean = true,
    val notifyCalls: Boolean = true,
    val notifyAnnouncements: Boolean = true,
    val hideLockScreenPreview: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val deviceName: String = "",
    val deviceModel: String = "",
    val platform: String = "Android",
    val appVersion: String = "1.0",
    val revoked: Boolean = false,
    val lastActiveAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "MESSAGE",
    val targetType: String = "",
    val targetId: String = "",
    val read: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class KeyBundle(
    @DocumentId val id: String = "",
    val userId: String = "",
    val identityKeyPub: String = "",
    val signedPreKeyPub: String = "",
    val signedPreKeyId: Long = 1L,
    val fingerprint: String = "",
    val oneTimePreKeys: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AccessPass(
    @DocumentId val id: String = "",
    val passHash: String = "",
    val label: String = "",
    val passType: String = "SINGLE_USE",
    val maxUses: Long = 1L,
    val usedCount: Long = 0L,
    val revoked: Boolean = false,
    val expiresAt: Timestamp? = null,
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class OwnerPassVault(
    @DocumentId val id: String = "",
    val passHash: String = "",
    val passCode: String = "",
    val label: String = "",
    val createdBy: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AccessPassRedemption(
    @DocumentId val id: String = "",
    val passHash: String = "",
    val userId: String = "",
    val username: String = "",
    val redeemedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Conversation(
    @DocumentId val id: String = "",
    val participantUids: List<String> = emptyList(),
    val lastMessageCiphertext: String = "",
    val lastMessageIv: String = "",
    val lastMessageEphemeralPub: String = "",
    val lastMessageSenderId: String = "",
    val lastMessagePreviewHint: String = "",
    val lastMessageAt: Timestamp? = null,
    val pinnedByUids: List<String> = emptyList(),
    val archivedByUids: List<String> = emptyList(),
    val mutedByUids: List<String> = emptyList(),
    val typingUid: String = "",
    val typingAt: Timestamp? = null,
    val unreadCounts: Map<String, Long> = emptyMap(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class KryptMessage(
    @DocumentId val id: String = "",
    val conversationId: String = "",
    val groupId: String = "",
    val participantUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    val senderId: String = "",
    val senderName: String = "",
    val ciphertext: String = "",
    val iv: String = "",
    val ephemeralPubKey: String = "",
    val senderKeyId: String = "",
    val keyVersion: Long = 1L,
    val messageType: String = "TEXT",
    val mediaName: String = "",
    val mediaMime: String = "",
    val mediaDataBase64: String = "",
    val mediaDurationMs: Long = 0L,
    val replyToMessageId: String = "",
    val replyToPreview: String = "",
    val forwardedFrom: String = "",
    val reactions: Map<String, String> = emptyMap(),
    val edited: Boolean = false,
    val deleted: Boolean = false,
    val pinned: Boolean = false,
    val deliveredToUids: List<String> = emptyList(),
    val readByUids: List<String> = emptyList(),
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class DecryptedMessageUi(
    val id: String,
    val senderId: String,
    val senderName: String,
    val plaintext: String,
    val messageType: MessageType,
    val mediaName: String,
    val mediaMime: String,
    val mediaDataBase64: String,
    val mediaDurationMs: Long,
    val replyToMessageId: String,
    val replyToPreview: String,
    val forwardedFrom: String,
    val reactions: Map<String, String>,
    val edited: Boolean,
    val deleted: Boolean,
    val pinned: Boolean,
    val deliveredToUids: List<String>,
    val readByUids: List<String>,
    val createdAtMillis: Long,
    val isLocalPending: Boolean = false,
    val isFailed: Boolean = false,
    val isEncryptedVerified: Boolean = true
)

@Keep
@IgnoreExtraProperties
data class KryptGroup(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val iconBase64: String = "",
    val ownerId: String = "",
    val memberUids: List<String> = emptyList(),
    val adminUids: List<String> = emptyList(),
    val moderatorUids: List<String> = emptyList(),
    val discoverable: Boolean = false,
    val locked: Boolean = false,
    val suspended: Boolean = false,
    val slowModeSeconds: Long = 0L,
    val inviteCode: String = "",
    val inviteExpiresAt: Timestamp? = null,
    val announcementText: String = "",
    val pinnedMessageId: String = "",
    val mutedByUids: List<String> = emptyList(),
    val lastMessagePreview: String = "",
    val lastMessageSenderId: String = "",
    val lastMessageAt: Timestamp? = null,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupMemberRecord(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val userId: String = "",
    val role: String = "MEMBER",
    val muted: Boolean = false,
    val canPost: Boolean = true,
    val joinedAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GroupSenderKeyDoc(
    @DocumentId val id: String = "",
    val groupId: String = "",
    val senderUid: String = "",
    val recipientUid: String = "",
    val keyEpoch: Long = 1L,
    val wrappedKeyCiphertext: String = "",
    val wrappedKeyIv: String = "",
    val senderEphemeralPubKey: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CallSession(
    @DocumentId val id: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val calleeId: String = "",
    val calleeName: String = "",
    val participantUids: List<String> = emptyList(),
    val callType: String = "VOICE",
    val status: String = "RINGING",
    val sdpOffer: String = "",
    val sdpAnswer: String = "",
    val iceCandidates: List<String> = emptyList(),
    val durationSeconds: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SafetyReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "MESSAGE",
    val targetId: String = "",
    val targetUserId: String = "",
    val groupId: String = "",
    val conversationId: String = "",
    val reason: String = "",
    val details: String = "",
    val evidenceSnapshot: String = "",
    val status: String = "PENDING",
    val appealText: String = "",
    val moderatorNote: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModerationAction(
    @DocumentId val id: String = "",
    val targetUserId: String = "",
    val targetGroupId: String = "",
    val actionType: String = "WARN",
    val reason: String = "",
    val moderatorId: String = "",
    val reportId: String = "",
    val expiresAt: Timestamp? = null,
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Announcement(
    @DocumentId val id: String = "",
    val title: String = "",
    val body: String = "",
    val priority: String = "NORMAL",
    val authorId: String = "",
    val active: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminAuditLog(
    @DocumentId val id: String = "",
    val adminId: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val reason: String = "",
    val metadata: String = "",
    val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlatformSettings(
    @DocumentId val id: String = "",
    val maintenanceMode: Boolean = false,
    val registrationEnabled: Boolean = true,
    val requirePassForSignup: Boolean = true,
    val maxPassAttemptsPerHour: Long = 8L,
    val announcementBanner: String = "",
    val updatedBy: String = "",
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val role: String = "SAFETY_ADMIN",
    val grantedBy: String = "",
    val createdAt: Timestamp? = null
)

data class OwnerStats(
    val totalMembers: Int = 0,
    val activeMembers: Int = 0,
    val totalGroups: Int = 0,
    val totalConversations: Int = 0,
    val totalMessages: Int = 0,
    val pendingReports: Int = 0,
    val suspendedAccounts: Int = 0,
    val activePasses: Int = 0
)
