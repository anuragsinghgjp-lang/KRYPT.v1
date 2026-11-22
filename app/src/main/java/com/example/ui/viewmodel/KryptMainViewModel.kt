package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.GhostModeState
import com.example.data.model.Announcement
import com.example.data.model.CallSession
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.Conversation
import com.example.data.model.DecryptedMessageUi
import com.example.data.model.GroupMemberRecord
import com.example.data.model.GroupRole
import com.example.data.model.KryptGroup
import com.example.data.model.MessageType
import com.example.data.model.ModerationAction
import com.example.data.model.PlatformSettings
import com.example.data.model.SafetyReport
import com.example.data.model.UiState
import com.example.data.model.UserNotification
import com.example.data.model.UserPrivateAccount
import com.example.data.model.UserProfile
import com.example.data.model.UserSession
import com.example.data.remote.unwrapFirestoreMessage
import com.example.data.repository.KryptAuthRepository
import com.example.data.repository.KryptGroupRepository
import com.example.data.repository.KryptMessagingRepository
import com.example.data.repository.KryptSafetyAndOwnerRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KryptMainViewModel(
    val currentUserId: String,
    val authRepo: KryptAuthRepository,
    val messagingRepo: KryptMessagingRepository,
    val groupRepo: KryptGroupRepository,
    val safetyRepo: KryptSafetyAndOwnerRepository
) : ViewModel() {

    val userProfileState: StateFlow<UiState<UserProfile?>> = authRepo.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            emit(UiState.Error(e.unwrapFirestoreMessage("Failed to load profile")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val privateAccountState: StateFlow<UserPrivateAccount?> = authRepo.observePrivateAccount(currentUserId)
        .catch { emit(null) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val ghostModeState: StateFlow<GhostModeState> = authRepo.observeGhostMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), GhostModeState())

    val platformSettings: StateFlow<PlatformSettings> = authRepo.observePlatformSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlatformSettings())

    val directoryUsers: StateFlow<List<UserProfile>> = authRepo.observeUserDirectory()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val sessions: StateFlow<List<UserSession>> = authRepo.observeSessions(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val conversationsState: StateFlow<UiState<List<Conversation>>> = messagingRepo.observeConversations(currentUserId)
        .map<List<Conversation>, UiState<List<Conversation>>> { UiState.Success(it) }
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            emit(UiState.Error(e.unwrapFirestoreMessage("Unable to load conversations")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val myGroupsState: StateFlow<UiState<List<KryptGroup>>> = groupRepo.observeMyGroups(currentUserId)
        .map<List<KryptGroup>, UiState<List<KryptGroup>>> { UiState.Success(it) }
        .catch { e ->
            if (e is CancellationException && e.cause == null) throw e
            emit(UiState.Error(e.unwrapFirestoreMessage("Unable to load groups")))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val discoverableGroups: StateFlow<List<KryptGroup>> = groupRepo.observeDiscoverableGroups()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val calls: StateFlow<List<CallSession>> = safetyRepo.observeCalls(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val notifications: StateFlow<List<UserNotification>> = safetyRepo.observeNotifications(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val announcements: StateFlow<List<Announcement>> = safetyRepo.observeActiveAnnouncements()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myReports: StateFlow<List<SafetyReport>> = safetyRepo.observeMyReports(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val myModerationActions: StateFlow<List<ModerationAction>> = safetyRepo.observeModerationActionsAgainstMe(currentUserId)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val passCooldownUntil: StateFlow<Long> = authRepo.prefs.passCooldownFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 0L)

    init {
        viewModelScope.launch {
            authRepo.ensureSessionAndKeys(currentUserId)
            authRepo.updatePresence(currentUserId, true)
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun postStatus(msg: String) {
        _statusMessage.value = msg
    }

    fun isBootstrappedOwner(): Boolean = authRepo.isBootstrappedOwner()

    fun getLocalKeyFingerprint(): String = authRepo.crypto.getLocalIdentityFingerprint(currentUserId)

    fun redeemAccessPass(
        rawCode: String,
        username: String,
        displayName: String,
        bio: String,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = authRepo.redeemAccessPassAndCreateProfile(rawCode, username, displayName, bio)
            _isBusy.value = false
            res.onSuccess { onSuccess() }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Access pass verification failed" }
        }
    }

    fun initializeOwnerProfile(username: String, displayName: String, bio: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = authRepo.bootstrapOwnerProfileIfEligible(username, displayName, bio)
            _isBusy.value = false
            res.onSuccess { onSuccess() }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Owner initialization failed" }
        }
    }

    fun updateProfile(username: String, displayName: String, bio: String, avatarBase64: String? = null) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = authRepo.updateProfile(currentUserId, username, displayName, bio, avatarBase64)
            _isBusy.value = false
            res.onSuccess { _statusMessage.value = "Profile updated" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Profile update failed" }
        }
    }

    fun setGhostMode(enabled: Boolean, durationMs: Long?, label: String) {
        viewModelScope.launch {
            val res = authRepo.updateGhostMode(currentUserId, enabled, durationMs, label)
            res.onSuccess {
                _statusMessage.value = if (enabled) "Ghost Mode enabled ($label)" else "Ghost Mode disabled"
            }.onFailure {
                _statusMessage.value = it.localizedMessage ?: "Failed to update Ghost Mode"
            }
        }
    }

    fun updatePrivacyPreferences(
        onlineVisible: Boolean,
        readReceiptsEnabled: Boolean,
        hideLockScreenPreview: Boolean,
        notifyMessages: Boolean,
        notifyGroups: Boolean,
        notifyCalls: Boolean,
        notifyAnnouncements: Boolean
    ) {
        viewModelScope.launch {
            authRepo.updatePrivacyAndNotificationSettings(
                currentUserId, onlineVisible, readReceiptsEnabled, hideLockScreenPreview,
                notifyMessages, notifyGroups, notifyCalls, notifyAnnouncements
            ).onSuccess { _statusMessage.value = "Privacy & notification preferences saved" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to save settings" }
        }
    }

    fun revokeSession(sessionId: String) {
        viewModelScope.launch {
            authRepo.revokeSession(currentUserId, sessionId)
                .onSuccess { _statusMessage.value = "Session revoked" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to revoke session" }
        }
    }

    fun openOrCreateConversation(peerUid: String, onReady: (String) -> Unit) {
        viewModelScope.launch {
            messagingRepo.startOrGetConversation(currentUserId, peerUid)
                .onSuccess(onReady)
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Could not start chat" }
        }
    }

    fun observeChatMessages(conversationId: String) =
        messagingRepo.observeMessages(conversationId, currentUserId)
            .catch { emit(emptyList()) }

    fun sendDirectMessage(
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
        forwardedFrom: String = "",
        recentTexts: List<String> = emptyList()
    ) {
        val spamFlag = safetyRepo.detectSpamOrRepetition(recentTexts, plaintext)
        if (spamFlag != null) {
            _statusMessage.value = "Safety Notice: $spamFlag"
        }
        viewModelScope.launch {
            messagingRepo.sendDirectMessage(
                conversationId, participantUids, senderProfile, plaintext,
                messageType, mediaName, mediaMime, mediaDataBase64, mediaDurationMs,
                replyToMessageId, replyToPreview, forwardedFrom
            ).onFailure { _statusMessage.value = it.localizedMessage ?: "Message failed to send" }
        }
    }

    fun markConversationRead(conversationId: String, profile: UserProfile?, messages: List<DecryptedMessageUi>) {
        viewModelScope.launch {
            messagingRepo.markConversationRead(conversationId, currentUserId, profile, messages)
        }
    }

    fun setTyping(conversationId: String, typing: Boolean) {
        viewModelScope.launch { messagingRepo.setTyping(conversationId, currentUserId, typing) }
    }

    fun toggleDirectReaction(conversationId: String, messageId: String, emoji: String) {
        viewModelScope.launch { messagingRepo.toggleReaction(conversationId, messageId, currentUserId, emoji) }
    }

    fun editDirectMessage(conversationId: String, participantUids: List<String>, messageId: String, newText: String) {
        viewModelScope.launch { messagingRepo.editDirectMessage(conversationId, participantUids, messageId, newText) }
    }

    fun deleteDirectMessage(conversationId: String, messageId: String) {
        viewModelScope.launch { messagingRepo.deleteDirectMessage(conversationId, messageId) }
    }

    fun togglePinDirectMessage(conversationId: String, messageId: String, pinned: Boolean) {
        viewModelScope.launch { messagingRepo.togglePinDirectMessage(conversationId, messageId, pinned) }
    }

    fun toggleConversationFlag(conversationId: String, field: String, active: Boolean) {
        viewModelScope.launch {
            messagingRepo.toggleConversationFlag(conversationId, field, currentUserId, active)
        }
    }

    fun setUserBlocked(targetUid: String, block: Boolean) {
        viewModelScope.launch {
            messagingRepo.setUserBlocked(currentUserId, targetUid, block)
                .onSuccess { _statusMessage.value = if (block) "User blocked" else "User unblocked" }
        }
    }

    // --- GROUPS ---
    fun observeGroup(groupId: String) = groupRepo.observeGroup(groupId).catch { emit(null) }
    fun observeGroupMembers(groupId: String) = groupRepo.observeGroupMembers(groupId).catch { emit(emptyList()) }
    fun observeGroupMessages(groupId: String) = groupRepo.observeGroupMessages(groupId, currentUserId).catch { emit(emptyList()) }

    fun createGroup(
        name: String,
        description: String,
        discoverable: Boolean,
        slowModeSeconds: Long,
        memberUids: List<String>,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = groupRepo.createGroup(name, description, discoverable, slowModeSeconds, memberUids)
            _isBusy.value = false
            res.onSuccess(onCreated)
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to create group" }
        }
    }

    fun joinGroupByCode(code: String, onJoined: (String) -> Unit) {
        viewModelScope.launch {
            groupRepo.joinGroupByCode(code, currentUserId)
                .onSuccess(onJoined)
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Invalid or expired invite" }
        }
    }

    fun joinDiscoverableGroup(groupId: String, onJoined: (String) -> Unit) {
        viewModelScope.launch {
            groupRepo.joinGroupDirect(groupId, currentUserId)
                .onSuccess(onJoined)
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Could not join group" }
        }
    }

    fun sendGroupMessage(
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
    ) {
        viewModelScope.launch {
            groupRepo.sendGroupMessage(
                group, senderProfile, plaintext, messageType,
                mediaName, mediaMime, mediaDataBase64, mediaDurationMs,
                replyToMessageId, replyToPreview, forwardedFrom
            ).onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to send group message" }
        }
    }

    fun markGroupRead(groupId: String, messages: List<DecryptedMessageUi>) {
        viewModelScope.launch { groupRepo.markGroupMessagesRead(groupId, currentUserId, messages) }
    }

    fun toggleGroupReaction(groupId: String, messageId: String, emoji: String) {
        viewModelScope.launch { groupRepo.toggleGroupMessageReaction(groupId, messageId, currentUserId, emoji) }
    }

    fun updateGroupSettings(
        groupId: String,
        name: String? = null,
        description: String? = null,
        announcement: String? = null,
        locked: Boolean? = null,
        slowModeSeconds: Long? = null,
        regenInvite: Boolean = false
    ) {
        viewModelScope.launch {
            groupRepo.updateGroupSettings(groupId, name, description, announcement, locked, slowModeSeconds, regenInvite)
                .onSuccess { _statusMessage.value = "Group updated" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to update group" }
        }
    }

    fun addGroupMember(groupId: String, targetUid: String) {
        viewModelScope.launch {
            groupRepo.addMember(groupId, targetUid)
                .onSuccess { _statusMessage.value = "Member added" }
        }
    }

    fun removeGroupMember(groupId: String, targetUid: String) {
        viewModelScope.launch {
            groupRepo.removeMember(groupId, targetUid)
                .onSuccess { _statusMessage.value = "Member removed" }
        }
    }

    fun assignGroupRole(groupId: String, targetUid: String, role: GroupRole) {
        viewModelScope.launch {
            groupRepo.assignMemberRole(groupId, targetUid, role)
                .onSuccess { _statusMessage.value = "Role updated to ${role.name}" }
        }
    }

    fun toggleGroupMute(groupId: String, muted: Boolean) {
        viewModelScope.launch { groupRepo.toggleGroupMute(groupId, currentUserId, muted) }
    }

    fun deleteGroup(groupId: String, onDeleted: () -> Unit) {
        viewModelScope.launch {
            groupRepo.deleteGroup(groupId)
                .onSuccess { onDeleted() }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Could not delete group" }
        }
    }

    // --- CALLS ---
    fun observeCall(callId: String) = safetyRepo.observeCall(callId).catch { emit(null) }

    fun startCall(caller: UserProfile, callee: UserProfile, type: CallType, onStarted: (String) -> Unit) {
        viewModelScope.launch {
            safetyRepo.initiateCall(caller, callee, type)
                .onSuccess(onStarted)
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Call signaling failed" }
        }
    }

    fun updateCallStatus(callId: String, status: CallStatus, durationSeconds: Long = 0L) {
        viewModelScope.launch { safetyRepo.updateCallStatus(callId, status, durationSeconds) }
    }

    // --- SAFETY & NOTIFICATIONS ---
    fun markNotificationRead(notifId: String) {
        viewModelScope.launch { safetyRepo.markNotificationRead(currentUserId, notifId) }
    }

    fun submitSafetyReport(
        targetType: String,
        targetId: String,
        targetUserId: String = "",
        groupId: String = "",
        conversationId: String = "",
        reason: String,
        details: String,
        evidenceSnapshot: String = ""
    ) {
        viewModelScope.launch {
            safetyRepo.submitReport(targetType, targetId, targetUserId, groupId, conversationId, reason, details, evidenceSnapshot)
                .onSuccess { _statusMessage.value = "Safety report submitted with preserved evidence" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to submit report" }
        }
    }

    fun submitAppeal(reportId: String, appealText: String) {
        viewModelScope.launch {
            safetyRepo.submitAppeal(reportId, appealText)
                .onSuccess { _statusMessage.value = "Appeal submitted for Owner review" }
                .onFailure { _statusMessage.value = it.localizedMessage ?: "Failed to submit appeal" }
        }
    }
}
