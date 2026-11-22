package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AccessPass
import com.example.data.model.AdminAuditLog
import com.example.data.model.AdminRecord
import com.example.data.model.KryptGroup
import com.example.data.model.KryptMessage
import com.example.data.model.OwnerPassVault
import com.example.data.model.OwnerStats
import com.example.data.model.PlatformSettings
import com.example.data.model.SafetyReport
import com.example.data.model.UserProfile
import com.example.data.repository.KryptSafetyAndOwnerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class OwnerControlViewModel(
    private val ownerRepo: KryptSafetyAndOwnerRepository
) : ViewModel() {

    val allUsers: StateFlow<List<UserProfile>> = ownerRepo.observeAllUsers()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allGroups: StateFlow<List<KryptGroup>> = ownerRepo.observeAllGroups()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allReports: StateFlow<List<SafetyReport>> = ownerRepo.observeAllReports()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val accessPasses: StateFlow<List<AccessPass>> = ownerRepo.observeAccessPasses()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val passVault: StateFlow<List<OwnerPassVault>> = ownerRepo.observeOwnerPassVault()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val auditLogs: StateFlow<List<AdminAuditLog>> = ownerRepo.observeAuditLogs()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val admins: StateFlow<List<AdminRecord>> = ownerRepo.observeAdmins()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _stats = MutableStateFlow(OwnerStats())
    val stats: StateFlow<OwnerStats> = _stats.asStateFlow()

    private val _lastGeneratedPass = MutableStateFlow<String?>(null)
    val lastGeneratedPass: StateFlow<String?> = _lastGeneratedPass.asStateFlow()

    private val _reviewedMessages = MutableStateFlow<List<KryptMessage>>(emptyList())
    val reviewedMessages: StateFlow<List<KryptMessage>> = _reviewedMessages.asStateFlow()

    private val _ownerFeedback = MutableStateFlow<String?>(null)
    val ownerFeedback: StateFlow<String?> = _ownerFeedback.asStateFlow()

    init {
        refreshStats()
    }

    fun clearFeedback() {
        _ownerFeedback.value = null
    }

    fun refreshStats() {
        viewModelScope.launch {
            ownerRepo.computeOwnerStats()
                .onSuccess { _stats.value = it }
        }
    }

    fun generateAccessPass(label: String, passType: String, maxUses: Long, validDays: Int) {
        viewModelScope.launch {
            ownerRepo.generateAccessPass(label, passType, maxUses, validDays)
                .onSuccess { code ->
                    _lastGeneratedPass.value = code
                    _ownerFeedback.value = "Generated pass: $code"
                    refreshStats()
                }
                .onFailure { _ownerFeedback.value = it.localizedMessage ?: "Pass generation failed" }
        }
    }

    fun revokeAccessPass(passHash: String, reason: String) {
        viewModelScope.launch {
            ownerRepo.revokeAccessPass(passHash, reason)
                .onSuccess {
                    _ownerFeedback.value = "Access pass revoked"
                    refreshStats()
                }
                .onFailure { _ownerFeedback.value = it.localizedMessage ?: "Failed to revoke pass" }
        }
    }

    fun moderateUser(targetUid: String, actionType: String, reason: String, reportId: String = "") {
        viewModelScope.launch {
            ownerRepo.moderateUserAccount(targetUid, actionType, reason, reportId)
                .onSuccess {
                    _ownerFeedback.value = "User moderation applied: $actionType"
                    refreshStats()
                }
                .onFailure { _ownerFeedback.value = it.localizedMessage ?: "Moderation action failed" }
        }
    }

    fun governGroup(groupId: String, locked: Boolean? = null, suspended: Boolean? = null, reason: String) {
        viewModelScope.launch {
            ownerRepo.governGroupAsOwner(groupId, locked, suspended, reason)
                .onSuccess { _ownerFeedback.value = "Group governance updated" }
                .onFailure { _ownerFeedback.value = it.localizedMessage ?: "Failed to update group" }
        }
    }

    fun inspectGroupMessages(groupId: String, auditReason: String) {
        viewModelScope.launch {
            ownerRepo.inspectGroupMessagesWithAudit(groupId, auditReason)
                .onSuccess {
                    _reviewedMessages.value = it
                    _ownerFeedback.value = "Audit log recorded. Loaded ${it.size} messages."
                }
                .onFailure { _ownerFeedback.value = it.localizedMessage ?: "Authorized review failed" }
        }
    }

    fun removeGroupMessage(groupId: String, messageId: String, reason: String) {
        viewModelScope.launch {
            ownerRepo.removeGroupMessageAsOwner(groupId, messageId, reason)
                .onSuccess {
                    _ownerFeedback.value = "Content removed & logged"
                    _reviewedMessages.value = _reviewedMessages.value.map {
                        if (it.id == messageId) it.copy(deleted = true) else it
                    }
                }
                .onFailure { _ownerFeedback.value = it.localizedMessage ?: "Failed to remove message" }
        }
    }

    fun resolveReport(reportId: String, status: String, note: String) {
        viewModelScope.launch {
            ownerRepo.resolveReportStatus(reportId, status, note)
                .onSuccess {
                    _ownerFeedback.value = "Report marked $status"
                    refreshStats()
                }
                .onFailure { _ownerFeedback.value = it.localizedMessage ?: "Failed to update report" }
        }
    }

    fun publishAnnouncement(title: String, body: String, priority: String) {
        viewModelScope.launch {
            ownerRepo.publishAnnouncement(title, body, priority)
                .onSuccess { _ownerFeedback.value = "Announcement published globally" }
                .onFailure { _ownerFeedback.value = it.localizedMessage ?: "Announcement failed" }
        }
    }

    fun updatePlatformSettings(settings: PlatformSettings, reason: String) {
        viewModelScope.launch {
            ownerRepo.updatePlatformSettings(settings, reason)
                .onSuccess { _ownerFeedback.value = "Platform governance settings updated" }
                .onFailure { _ownerFeedback.value = it.localizedMessage ?: "Settings update failed" }
        }
    }

    fun toggleSafetyAdmin(targetUid: String, email: String, grant: Boolean, reason: String) {
        viewModelScope.launch {
            ownerRepo.toggleSafetyAdmin(targetUid, email, grant, reason)
                .onSuccess { _ownerFeedback.value = if (grant) "Safety Admin granted" else "Safety Admin revoked" }
                .onFailure { _ownerFeedback.value = it.localizedMessage ?: "Role update failed" }
        }
    }
}
