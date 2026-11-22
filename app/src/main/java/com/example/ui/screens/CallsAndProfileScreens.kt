package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CallStatus
import com.example.data.model.CallType
import com.example.data.model.GhostDurationOption
import com.example.data.model.UserProfile
import com.example.ui.components.KryptAvatar
import com.example.ui.components.KryptBadge
import com.example.ui.components.KryptE2eeBadge
import com.example.ui.components.KryptEmptyState
import com.example.ui.components.KryptOutlinedButton
import com.example.ui.components.KryptPrimaryButton
import com.example.ui.components.KryptSurfaceCard
import com.example.ui.components.KryptTextField
import com.example.ui.components.KryptTopBar
import com.example.ui.components.formatDateTime
import com.example.ui.theme.KryptAmber
import com.example.ui.theme.KryptAmberSurface
import com.example.ui.theme.KryptAsh
import com.example.ui.theme.KryptBlack
import com.example.ui.theme.KryptBorder
import com.example.ui.theme.KryptCharcoal
import com.example.ui.theme.KryptCharcoalDeep
import com.example.ui.theme.KryptCharcoalElevated
import com.example.ui.theme.KryptCrimson
import com.example.ui.theme.KryptEmerald
import com.example.ui.theme.KryptSilver
import com.example.ui.theme.KryptWhite
import com.example.ui.viewmodel.KryptMainViewModel
import kotlinx.coroutines.delay

@Composable
fun KryptCallsScreen(
    viewModel: KryptMainViewModel,
    currentUser: UserProfile,
    onOpenCall: (String) -> Unit
) {
    val calls by viewModel.calls.collectAsStateWithLifecycle()
    val directory by viewModel.directoryUsers.collectAsStateWithLifecycle()
    var showNewCallDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(KryptBlack)) {
        if (calls.isEmpty()) {
            KryptEmptyState(
                title = "No Call History",
                subtitle = "Initiate an encrypted 1:1 WebRTC voice or video call using the + button."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(calls, key = { it.id }) { call ->
                    val isOutgoing = call.callerId == currentUser.userId
                    val peerName = if (isOutgoing) call.calleeName else call.callerName
                    val peerUid = if (isOutgoing) call.calleeId else call.callerId
                    val peer = directory.firstOrNull { it.userId == peerUid }

                    KryptSurfaceCard(onClick = { onOpenCall(call.id) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            KryptAvatar(displayName = peerName.ifBlank { "Member" }, size = 44.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(peerName.ifBlank { "Member" }, style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                                Text(
                                    text = "${if (isOutgoing) "Outgoing" else "Incoming"} ${call.callType} · ${call.status} (${call.durationSeconds}s)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (call.status == CallStatus.MISSED.name) KryptCrimson else KryptSilver
                                )
                            }
                            if (peer != null) {
                                IconButton(onClick = {
                                    viewModel.startCall(currentUser, peer, CallType.VOICE, onOpenCall)
                                }) {
                                    Icon(Icons.Filled.Call, "Voice redial", tint = KryptWhite)
                                }
                                IconButton(onClick = {
                                    viewModel.startCall(currentUser, peer, CallType.VIDEO, onOpenCall)
                                }) {
                                    Icon(Icons.Filled.Videocam, "Video redial", tint = KryptWhite)
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showNewCallDialog = true },
            containerColor = KryptWhite,
            contentColor = KryptBlack,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("new_call_fab")
        ) {
            Icon(Icons.Filled.Phone, "Start encrypted call")
        }
    }

    if (showNewCallDialog) {
        val candidates = directory.filter { it.userId != currentUser.userId }
        AlertDialog(
            onDismissRequest = { showNewCallDialog = false },
            containerColor = KryptCharcoalDeep,
            title = { Text("Start Encrypted Call", color = KryptWhite) },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp)) {
                    items(candidates, key = { it.userId }) { user ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            KryptAvatar(displayName = user.displayName, size = 38.dp)
                            Spacer(Modifier.width(10.dp))
                            Text(user.displayName, color = KryptWhite, modifier = Modifier.weight(1f))
                            IconButton(onClick = {
                                showNewCallDialog = false
                                viewModel.startCall(currentUser, user, CallType.VOICE, onOpenCall)
                            }) {
                                Icon(Icons.Filled.Call, "Voice call", tint = KryptWhite)
                            }
                            IconButton(onClick = {
                                showNewCallDialog = false
                                viewModel.startCall(currentUser, user, CallType.VIDEO, onOpenCall)
                            }) {
                                Icon(Icons.Filled.Videocam, "Video call", tint = KryptWhite)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewCallDialog = false }) { Text("CLOSE", color = KryptSilver) }
            }
        )
    }
}

@Composable
fun KryptCallScreen(
    callId: String,
    viewModel: KryptMainViewModel,
    currentUser: UserProfile,
    onExitCall: () -> Unit
) {
    val context = LocalContext.current
    val call by viewModel.observeCall(callId).collectAsStateWithLifecycle(initialValue = null)
    var micMuted by remember { mutableStateOf(false) }
    var speakerOn by remember { mutableStateOf(false) }
    var cameraEnabled by remember { mutableStateOf(call?.callType == CallType.VIDEO.name) }
    var elapsedSec by remember { mutableLongStateOf(0L) }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        cameraEnabled = granted
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            audioLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(call?.status) {
        if (call?.status == CallStatus.ACTIVE.name) {
            while (true) {
                delay(1000)
                elapsedSec++
            }
        }
    }

    val c = call
    val isCaller = c?.callerId == currentUser.userId
    val peerName = if (isCaller) c.calleeName else c?.callerName
    val isVideo = c?.callType == CallType.VIDEO.name

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))
            KryptE2eeBadge(if (isVideo) "WEBRTC DTLS-SRTP VIDEO" else "WEBRTC DTLS-SRTP VOICE")
            Spacer(Modifier.height(36.dp))
            KryptAvatar(displayName = peerName ?: "Member", size = 104.dp)
            Spacer(Modifier.height(20.dp))
            Text(
                text = peerName ?: "Connecting...",
                style = MaterialTheme.typography.headlineLarge,
                color = KryptWhite
            )
            Spacer(Modifier.height(8.dp))
            val statusLabel = when (c?.status) {
                CallStatus.RINGING.name -> if (isCaller) "RINGING · SIGNALING SENT" else "INCOMING ENCRYPTED CALL"
                CallStatus.ACTIVE.name -> String.format("%02d:%02d · CONNECTED", elapsedSec / 60, elapsedSec % 60)
                CallStatus.ENDED.name -> "CALL ENDED"
                CallStatus.DECLINED.name -> "CALL DECLINED"
                else -> "INITIALIZING"
            }
            Text(statusLabel, style = MaterialTheme.typography.labelMedium, color = KryptEmerald)
        }

        if (isVideo && cameraEnabled) {
            KryptSurfaceCard(borderColor = KryptBorder) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Videocam, "Camera feed active", tint = KryptEmerald)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Local Video Stream Ready · DTLS-SRTP Key Exchanged",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KryptSilver
                    )
                }
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (!isCaller && c?.status == CallStatus.RINGING.name) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    IconButton(
                        onClick = {
                            viewModel.updateCallStatus(callId, CallStatus.DECLINED)
                            onExitCall()
                        },
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(KryptCrimson)
                    ) {
                        Icon(Icons.Filled.CallEnd, "Decline", tint = KryptWhite)
                    }
                    IconButton(
                        onClick = { viewModel.updateCallStatus(callId, CallStatus.ACTIVE) },
                        modifier = Modifier.size(64.dp).clip(CircleShape).background(KryptEmerald)
                    ) {
                        Icon(Icons.Filled.Call, "Accept", tint = KryptBlack)
                    }
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    IconButton(
                        onClick = { micMuted = !micMuted },
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(KryptCharcoalElevated)
                    ) {
                        Icon(if (micMuted) Icons.Filled.MicOff else Icons.Filled.Mic, "Mute Mic", tint = KryptWhite)
                    }
                    IconButton(
                        onClick = { speakerOn = !speakerOn },
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(if (speakerOn) KryptWhite else KryptCharcoalElevated)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.VolumeUp, "Speaker", tint = if (speakerOn) KryptBlack else KryptWhite)
                    }
                    if (isVideo) {
                        IconButton(
                            onClick = {
                                if (!cameraEnabled && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                                    cameraLauncher.launch(Manifest.permission.CAMERA)
                                } else {
                                    cameraEnabled = !cameraEnabled
                                }
                            },
                            modifier = Modifier.size(56.dp).clip(CircleShape).background(KryptCharcoalElevated)
                        ) {
                            Icon(if (cameraEnabled) Icons.Filled.Videocam else Icons.Filled.VideocamOff, "Toggle Camera", tint = KryptWhite)
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.updateCallStatus(callId, CallStatus.ENDED, elapsedSec)
                            onExitCall()
                        },
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(KryptCrimson)
                    ) {
                        Icon(Icons.Filled.CallEnd, "End Call", tint = KryptWhite)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            if (isCaller && c.status == CallStatus.RINGING.name) {
                TextButton(onClick = { viewModel.updateCallStatus(callId, CallStatus.ACTIVE) }) {
                    Text("SIMULATE PEER ANSWER (SIGNALING TEST)", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                }
            }
        }
    }
}

@Composable
fun KryptNotificationsScreen(
    viewModel: KryptMainViewModel,
    onBack: () -> Unit
) {
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        KryptTopBar(title = "NOTIFICATIONS", subtitle = "Private Alert Center", onBack = onBack)
        if (notifications.isEmpty()) {
            KryptEmptyState(title = "All Clear", subtitle = "Encrypted message alerts, call notices, and security events appear here.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(notifications, key = { it.id }) { notif ->
                    KryptSurfaceCard(
                        borderColor = if (!notif.read) KryptWhite.copy(alpha = 0.4f) else KryptBorder,
                        onClick = { viewModel.markNotificationRead(notif.id) }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            KryptBadge(label = notif.category, color = if (notif.category == "MODERATION") KryptAmber else KryptSilver)
                            Text(formatDateTime(notif.createdAt?.toDate()?.time ?: 0L), style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(notif.title, style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                        Text(notif.body, style = MaterialTheme.typography.bodyMedium, color = KryptSilver)
                    }
                }
            }
        }
    }
}

@Composable
fun KryptUserProfileScreen(
    viewModel: KryptMainViewModel,
    currentUser: UserProfile,
    isOwnerOrAdmin: Boolean,
    onOpenSettings: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
    onOpenBlockedUsers: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenOwnerPanel: () -> Unit,
    onSignOut: () -> Unit
) {
    val ghostMode by viewModel.ghostModeState.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    var showEditDialog by remember { mutableStateOf(false) }
    val fingerprint = remember(currentUser.userId) { viewModel.getLocalKeyFingerprint() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        KryptSurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KryptAvatar(
                    displayName = currentUser.displayName,
                    avatarBase64 = currentUser.avatarBase64,
                    size = 64.dp
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(currentUser.displayName, style = MaterialTheme.typography.titleLarge, color = KryptWhite)
                    Text("@${currentUser.username}", style = MaterialTheme.typography.labelMedium, color = KryptSilver)
                    Spacer(Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        KryptBadge(label = currentUser.role, color = if (currentUser.role == "OWNER") KryptAmber else KryptWhite)
                        KryptBadge(label = currentUser.accountStatus, color = if (currentUser.accountStatus == "ACTIVE") KryptEmerald else KryptCrimson)
                    }
                }
                IconButton(onClick = { showEditDialog = true }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Edit, "Edit profile", tint = KryptWhite)
                }
            }
            if (currentUser.bio.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(currentUser.bio, style = MaterialTheme.typography.bodyMedium, color = KryptSilver)
            }
        }

        // Ghost Mode Quick Card
        KryptSurfaceCard(
            borderColor = if (ghostMode.isEffectivelyActive()) KryptAmber else KryptBorder,
            containerColor = if (ghostMode.isEffectivelyActive()) KryptAmberSurface else KryptCharcoal,
            onClick = onOpenPrivacySettings
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.VisibilityOff, "Ghost Mode", tint = if (ghostMode.isEffectivelyActive()) KryptAmber else KryptWhite)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Ghost Mode", style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                    Text(
                        text = if (ghostMode.isEffectivelyActive()) "ACTIVE (${ghostMode.durationLabel}) · Invisible browsing"
                        else "Off · Tap to configure stealth reading & presence",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (ghostMode.isEffectivelyActive()) KryptAmber else KryptSilver
                    )
                }
                Switch(
                    checked = ghostMode.isEffectivelyActive(),
                    onCheckedChange = {
                        viewModel.setGhostMode(it, if (it) 60L * 60L * 1000L else null, "1 Hour")
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = KryptBlack, checkedTrackColor = KryptAmber)
                )
            }
        }

        if (isOwnerOrAdmin) {
            KryptSurfaceCard(
                borderColor = KryptAmber.copy(alpha = 0.6f),
                onClick = onOpenOwnerPanel
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AdminPanelSettings, "Owner Control Center", tint = KryptAmber)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Owner Control Center", style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                        Text("Passes, governance, moderation queue & audit logs", style = MaterialTheme.typography.bodyMedium, color = KryptSilver)
                    }
                }
            }
        }

        KryptSurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Fingerprint, "Key fingerprint", tint = KryptEmerald)
                Spacer(Modifier.width(10.dp))
                Text("E2EE IDENTITY KEY FINGERPRINT", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
            }
            Spacer(Modifier.height(6.dp))
            Text(fingerprint, style = MaterialTheme.typography.labelMedium, color = KryptWhite)
        }

        ProfileActionRow(Icons.Filled.VisibilityOff, "Privacy & Ghost Mode Settings", onOpenPrivacySettings)
        ProfileActionRow(Icons.Filled.Settings, "Notifications & Security Settings", onOpenSettings)
        ProfileActionRow(Icons.Filled.Block, "Blocked Users", onOpenBlockedUsers)
        ProfileActionRow(Icons.Filled.Report, "Safety Reports & Appeals", onOpenReports)

        KryptSurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Devices, "Active sessions", tint = KryptWhite)
                Spacer(Modifier.width(10.dp))
                Text("ACTIVE DEVICE SESSIONS (${sessions.count { !it.revoked }})", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
            }
            Spacer(Modifier.height(10.dp))
            sessions.take(5).forEach { sess ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(sess.deviceName, style = MaterialTheme.typography.bodyMedium, color = KryptWhite)
                        Text(
                            text = if (sess.revoked) "Revoked" else "${sess.platform} · Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (sess.revoked) KryptCrimson else KryptEmerald
                        )
                    }
                    if (!sess.revoked) {
                        TextButton(onClick = { viewModel.revokeSession(sess.id) }) {
                            Text("REVOKE", style = MaterialTheme.typography.labelSmall, color = KryptCrimson)
                        }
                    }
                }
            }
        }

        KryptOutlinedButton(
            text = "SIGN OUT SECURELY",
            accentColor = KryptCrimson,
            onClick = onSignOut,
            modifier = Modifier.testTag("profile_sign_out_button")
        )
        Spacer(Modifier.height(16.dp))
    }

    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = KryptCharcoalDeep,
            title = { Text("Edit Profile", color = KryptWhite) },
            text = {
                KryptProfileEditDialogContent(
                    currentUsername = currentUser.username,
                    currentDisplayName = currentUser.displayName,
                    currentBio = currentUser.bio,
                    currentAvatarBase64 = currentUser.avatarBase64,
                    onSave = { u, d, b, av ->
                        viewModel.updateProfile(u, d, b, av)
                        showEditDialog = false
                    }
                )
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) { Text("CLOSE", color = KryptSilver) }
            }
        )
    }
}

@Composable
private fun ProfileActionRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = KryptCharcoal,
        border = BorderStroke(1.dp, KryptBorder),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, label, tint = KryptWhite, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, color = KryptWhite)
        }
    }
}

@Composable
fun KryptPrivacySettingsScreen(
    viewModel: KryptMainViewModel,
    currentUser: UserProfile,
    onBack: () -> Unit
) {
    val ghostMode by viewModel.ghostModeState.collectAsStateWithLifecycle()
    val priv by viewModel.privateAccountState.collectAsStateWithLifecycle()

    var selectedDuration by remember { mutableStateOf(GhostDurationOption.ONE_HOUR) }
    var onlineVisible by remember(currentUser.onlineVisible) { mutableStateOf(currentUser.onlineVisible) }
    var readReceipts by remember(currentUser.readReceiptsEnabled) { mutableStateOf(currentUser.readReceiptsEnabled) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        KryptTopBar(title = "PRIVACY & GHOST MODE", subtitle = "Presence & Receipt Controls", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KryptSurfaceCard(borderColor = if (ghostMode.isEffectivelyActive()) KryptAmber else KryptBorder) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Ghost Mode", style = MaterialTheme.typography.titleLarge, color = KryptWhite)
                        Text(
                            "Browse conversations and groups invisibly without updating online status, typing indicators, or sending read receipts.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KryptSilver
                        )
                    }
                    Switch(
                        checked = ghostMode.isEffectivelyActive(),
                        onCheckedChange = { enabled ->
                            viewModel.setGhostMode(enabled, selectedDuration.durationMs, selectedDuration.label)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = KryptBlack, checkedTrackColor = KryptAmber),
                        modifier = Modifier.testTag("ghost_mode_switch")
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text("CONFIGURABLE DURATION", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    GhostDurationOption.entries.forEach { opt ->
                        val selected = selectedDuration == opt
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) KryptCharcoalElevated else KryptCharcoalDeep)
                                .border(1.dp, if (selected) KryptAmber else KryptBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedDuration = opt
                                    if (ghostMode.isEffectivelyActive()) {
                                        viewModel.setGhostMode(true, opt.durationMs, opt.label)
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(opt.label, style = MaterialTheme.typography.bodyMedium, color = KryptWhite)
                            if (selected) Text("SELECTED", style = MaterialTheme.typography.labelSmall, color = KryptAmber)
                        }
                    }
                }
            }

            KryptSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Show Online Status & Last Seen", style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                        Text("Overridden automatically when Ghost Mode is active", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                    }
                    Switch(
                        checked = onlineVisible,
                        onCheckedChange = { onlineVisible = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = KryptBlack, checkedTrackColor = KryptWhite)
                    )
                }
                HorizontalDivider(color = KryptBorder, modifier = Modifier.padding(vertical = 10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Send Read Receipts", style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                        Text("Disabled automatically while Ghost Mode is active", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                    }
                    Switch(
                        checked = readReceipts,
                        onCheckedChange = { readReceipts = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = KryptBlack, checkedTrackColor = KryptWhite)
                    )
                }
                Spacer(Modifier.height(14.dp))
                KryptPrimaryButton(
                    text = "SAVE PRIVACY SETTINGS",
                    onClick = {
                        viewModel.updatePrivacyPreferences(
                            onlineVisible = onlineVisible,
                            readReceiptsEnabled = readReceipts,
                            hideLockScreenPreview = priv?.hideLockScreenPreview ?: true,
                            notifyMessages = priv?.notifyMessages ?: true,
                            notifyGroups = priv?.notifyGroups ?: true,
                            notifyCalls = priv?.notifyCalls ?: true,
                            notifyAnnouncements = priv?.notifyAnnouncements ?: true
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun KryptSettingsScreen(
    viewModel: KryptMainViewModel,
    currentUser: UserProfile,
    onBack: () -> Unit,
    onOpenRecovery: () -> Unit
) {
    val priv by viewModel.privateAccountState.collectAsStateWithLifecycle()
    var notifyMsgs by remember(priv) { mutableStateOf(priv?.notifyMessages ?: true) }
    var notifyGrps by remember(priv) { mutableStateOf(priv?.notifyGroups ?: true) }
    var notifyCalls by remember(priv) { mutableStateOf(priv?.notifyCalls ?: true) }
    var notifyAnn by remember(priv) { mutableStateOf(priv?.notifyAnnouncements ?: true) }
    var hideLock by remember(priv) { mutableStateOf(priv?.hideLockScreenPreview ?: true) }
    var showEncryptionDoc by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        KryptTopBar(title = "SETTINGS", subtitle = "Notifications & Cryptographic Policy", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            KryptSurfaceCard {
                Text("NOTIFICATION PREFERENCES", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                Spacer(Modifier.height(8.dp))
                ToggleSettingRow("Direct Messages", notifyMsgs) { notifyMsgs = it }
                ToggleSettingRow("Group Messages", notifyGrps) { notifyGrps = it }
                ToggleSettingRow("Voice & Video Calls", notifyCalls) { notifyCalls = it }
                ToggleSettingRow("Platform Announcements", notifyAnn) { notifyAnn = it }
                ToggleSettingRow("Hide Message Preview on Lock Screen", hideLock) { hideLock = it }
                Spacer(Modifier.height(10.dp))
                KryptPrimaryButton(
                    text = "SAVE PREFERENCES",
                    onClick = {
                        viewModel.updatePrivacyPreferences(
                            onlineVisible = currentUser.onlineVisible,
                            readReceiptsEnabled = currentUser.readReceiptsEnabled,
                            hideLockScreenPreview = hideLock,
                            notifyMessages = notifyMsgs,
                            notifyGroups = notifyGrps,
                            notifyCalls = notifyCalls,
                            notifyAnnouncements = notifyAnn
                        )
                    }
                )
            }

            ProfileActionRow(Icons.Filled.Lock, "E2EE Protocol & Limitations") { showEncryptionDoc = true }
            ProfileActionRow(Icons.Filled.Policy, "Privacy Policy & Moderation Disclosure") { showPrivacyPolicy = true }
            ProfileActionRow(Icons.Filled.Security, "Account & Key Recovery", onOpenRecovery)
        }
    }

    if (showEncryptionDoc) {
        AlertDialog(
            onDismissRequest = { showEncryptionDoc = false },
            containerColor = KryptCharcoalDeep,
            title = { Text("KRYPT Encryption Architecture", color = KryptWhite) },
            text = {
                Text(
                    text = "1:1 Chats: Ephemeral ECDH P-256 (secp256r1) + HKDF-SHA256 + AES-256-GCM with per-message random 96-bit IVs and conversation AAD.\n\n" +
                        "Groups: Sender Keys (256-bit AES-256-GCM epoch keys wrapped per member via ECDH+HKDF).\n\n" +
                        "Key Storage: Private keys never leave the device and are encrypted at rest with Android Keystore AES-256-GCM.\n\n" +
                        "Limitations: Metadata (sender/recipient IDs, timestamps) is visible for routing. If a user submits a Safety Report, the reporting client attaches an explicit incident evidence snapshot for Owner moderation review.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KryptSilver
                )
            },
            confirmButton = {
                TextButton(onClick = { showEncryptionDoc = false }) { Text("CLOSE", color = KryptWhite) }
            }
        )
    }

    if (showPrivacyPolicy) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicy = false },
            containerColor = KryptCharcoalDeep,
            title = { Text("Privacy & Authorized Moderation Policy", color = KryptWhite) },
            text = {
                Text(
                    text = "KRYPT operates as an invite-only community. There is no hidden surveillance or undisclosed backdoor.\n\n" +
                        "All un-reported E2EE messages remain unreadable to the server without participant private keys. When a member submits a safety report, authorized Platform Owners and Safety Administrators may review reported incident evidence snapshots and group moderation context. Every administrative inspection is permanently recorded in immutable Audit Logs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KryptSilver
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicy = false }) { Text("CLOSE", color = KryptWhite) }
            }
        )
    }
}

@Composable
private fun ToggleSettingRow(label: String, checked: Boolean, onChanged: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = KryptWhite, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChanged,
            colors = SwitchDefaults.colors(checkedThumbColor = KryptBlack, checkedTrackColor = KryptWhite)
        )
    }
}

@Composable
fun KryptBlockedUsersScreen(
    viewModel: KryptMainViewModel,
    onBack: () -> Unit
) {
    val priv by viewModel.privateAccountState.collectAsStateWithLifecycle()
    val directory by viewModel.directoryUsers.collectAsStateWithLifecycle()
    val userMap = remember(directory) { directory.associateBy { it.userId } }
    val blocked = priv?.blockedUids.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        KryptTopBar(title = "BLOCKED USERS", subtitle = "${blocked.size} blocked", onBack = onBack)
        if (blocked.isEmpty()) {
            KryptEmptyState(title = "No Blocked Accounts", subtitle = "Blocked members cannot message or call you.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(blocked, key = { it }) { uid ->
                    val u = userMap[uid]
                    KryptSurfaceCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            KryptAvatar(displayName = u?.displayName ?: uid, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(u?.displayName ?: uid, color = KryptWhite, style = MaterialTheme.typography.titleMedium)
                                Text("@${u?.username ?: "member"}", color = KryptAsh, style = MaterialTheme.typography.labelSmall)
                            }
                            TextButton(onClick = { viewModel.setUserBlocked(uid, false) }) {
                                Text("UNBLOCK", color = KryptWhite)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KryptReportsScreen(
    viewModel: KryptMainViewModel,
    onBack: () -> Unit
) {
    val myReports by viewModel.myReports.collectAsStateWithLifecycle()
    val actions by viewModel.myModerationActions.collectAsStateWithLifecycle()
    var appealTargetId by remember { mutableStateOf<String?>(null) }
    var appealText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        KryptTopBar(title = "SAFETY & APPEALS", subtitle = "Submitted Reports & Notices", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (actions.isNotEmpty()) {
                item {
                    Text("MODERATION NOTICES ON YOUR ACCOUNT", style = MaterialTheme.typography.labelSmall, color = KryptAmber)
                }
                items(actions, key = { it.id }) { act ->
                    KryptSurfaceCard(borderColor = KryptAmber.copy(alpha = 0.5f)) {
                        KryptBadge(label = act.actionType, color = KryptAmber)
                        Spacer(Modifier.height(6.dp))
                        Text(act.reason, style = MaterialTheme.typography.bodyMedium, color = KryptWhite)
                        if (act.reportId.isNotBlank()) {
                            TextButton(onClick = { appealTargetId = act.reportId }) {
                                Text("SUBMIT APPEAL", color = KryptWhite)
                            }
                        }
                    }
                }
            }

            item {
                Text("YOUR SUBMITTED SAFETY REPORTS (${myReports.size})", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
            }
            if (myReports.isEmpty()) {
                item {
                    KryptEmptyState(
                        title = "No Safety Reports",
                        subtitle = "Long-press any message or open Group Info to report abuse with preserved evidence."
                    )
                }
            } else {
                items(myReports, key = { it.id }) { rep ->
                    KryptSurfaceCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            KryptBadge(label = "${rep.targetType} · ${rep.status}", color = KryptWhite)
                            Text(formatDateTime(rep.createdAt?.toDate()?.time ?: 0L), style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(rep.reason, style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                        if (rep.moderatorNote.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text("Moderator Note: ${rep.moderatorNote}", style = MaterialTheme.typography.bodyMedium, color = KryptSilver)
                        }
                    }
                }
            }
        }
    }

    if (appealTargetId != null) {
        AlertDialog(
            onDismissRequest = { appealTargetId = null },
            containerColor = KryptCharcoalDeep,
            title = { Text("Submit Moderation Appeal", color = KryptWhite) },
            text = {
                KryptTextField(
                    value = appealText,
                    onValueChange = { appealText = it },
                    label = "APPEAL EXPLANATION",
                    singleLine = false,
                    maxLines = 4
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (appealText.isNotBlank()) {
                        viewModel.submitAppeal(appealTargetId!!, appealText)
                        appealText = ""
                        appealTargetId = null
                    }
                }) { Text("SUBMIT APPEAL", color = KryptWhite) }
            },
            dismissButton = {
                TextButton(onClick = { appealTargetId = null }) { Text("CANCEL", color = KryptSilver) }
            }
        )
    }
}
