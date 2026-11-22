package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DecryptedMessageUi
import com.example.data.model.GroupRole
import com.example.data.model.KryptGroup
import com.example.data.model.MessageType
import com.example.data.model.UiState
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
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.KryptAmber
import com.example.ui.theme.KryptAsh
import com.example.ui.theme.KryptBlack
import com.example.ui.theme.KryptBorder
import com.example.ui.theme.KryptBorderBright
import com.example.ui.theme.KryptCharcoal
import com.example.ui.theme.KryptCharcoalDeep
import com.example.ui.theme.KryptCharcoalElevated
import com.example.ui.theme.KryptCrimson
import com.example.ui.theme.KryptEmerald
import com.example.ui.theme.KryptSilver
import com.example.ui.theme.KryptWhite
import com.example.ui.viewmodel.KryptMainViewModel
import com.example.ui.viewmodel.KryptVoicePlayer
import com.example.ui.viewmodel.KryptVoiceRecorder
import com.example.ui.viewmodel.MediaAndVoiceHelper

@Composable
fun KryptGroupListScreen(
    viewModel: KryptMainViewModel,
    currentUser: UserProfile,
    onOpenGroup: (String) -> Unit,
    onCreateGroup: () -> Unit
) {
    val groupsState by viewModel.myGroupsState.collectAsStateWithLifecycle()
    val discoverable by viewModel.discoverableGroups.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var showInviteDialog by remember { mutableStateOf(false) }
    var inviteInput by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(KryptBlack)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search groups", color = KryptAsh) },
                    leadingIcon = { Icon(Icons.Filled.Search, "Search", tint = KryptSilver) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = KryptCharcoal,
                        unfocusedContainerColor = KryptCharcoal,
                        focusedBorderColor = KryptBorderBright,
                        unfocusedBorderColor = KryptBorder,
                        focusedTextColor = KryptWhite,
                        unfocusedTextColor = KryptWhite
                    ),
                    modifier = Modifier.weight(1f).testTag("group_search_input")
                )
                IconButton(
                    onClick = { showInviteDialog = true },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(KryptCharcoal)
                        .border(1.dp, KryptBorder, RoundedCornerShape(10.dp))
                        .testTag("join_group_invite_button")
                ) {
                    Icon(Icons.Filled.Link, "Join by invite", tint = KryptWhite)
                }
            }

            when (val state = groupsState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Syncing groups...", style = MaterialTheme.typography.labelMedium, color = KryptAsh)
                    }
                }
                is UiState.Error -> {
                    KryptEmptyState(title = "Group Sync Error", subtitle = state.message)
                }
                is UiState.Success -> {
                    val myFiltered = state.data.filter {
                        searchQuery.isBlank() || it.name.contains(searchQuery, ignoreCase = true)
                    }
                    val invitedDiscover = discoverable.filter { d ->
                        d.id !in state.data.map { it.id } &&
                            (searchQuery.isBlank() || d.name.contains(searchQuery, ignoreCase = true))
                    }

                    if (myFiltered.isEmpty() && invitedDiscover.isEmpty()) {
                        KryptEmptyState(
                            title = "No Groups Yet",
                            subtitle = "Create a private encrypted community group or join using an invite code."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            if (myFiltered.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "JOINED COMMUNITIES (${myFiltered.size})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KryptAsh,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }
                                items(myFiltered, key = { it.id }) { grp ->
                                    GroupRowItem(group = grp, onClick = { onOpenGroup(grp.id) })
                                }
                            }
                            if (invitedDiscover.isNotEmpty()) {
                                item {
                                    Spacer(Modifier.height(12.dp))
                                    Text(
                                        text = "DISCOVER INVITED GROUPS",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KryptAsh,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }
                                items(invitedDiscover, key = { "disc_${it.id}" }) { grp ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        KryptAvatar(displayName = grp.name, size = 44.dp)
                                        Spacer(Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(grp.name, style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                                            Text(
                                                text = grp.description.ifBlank { "${grp.memberUids.size} members" },
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = KryptSilver,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                viewModel.joinDiscoverableGroup(grp.id) { onOpenGroup(it) }
                                            }
                                        ) {
                                            Text("JOIN", color = KryptWhite)
                                        }
                                    }
                                    HorizontalDivider(color = KryptBorder.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = onCreateGroup,
            containerColor = KryptWhite,
            contentColor = KryptBlack,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("new_group_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Create new group")
        }
    }

    if (showInviteDialog) {
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            containerColor = KryptCharcoalDeep,
            title = { Text("Join via Invite Code", color = KryptWhite) },
            text = {
                KryptTextField(
                    value = inviteInput,
                    onValueChange = { inviteInput = it.uppercase() },
                    label = "GROUP INVITE CODE",
                    placeholder = "KRP-XXXXXX",
                    isMonospace = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (inviteInput.isNotBlank()) {
                        viewModel.joinGroupByCode(inviteInput) { gid ->
                            showInviteDialog = false
                            inviteInput = ""
                            onOpenGroup(gid)
                        }
                    }
                }) { Text("JOIN GROUP", color = KryptWhite) }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("CANCEL", color = KryptSilver) }
            }
        )
    }
}

@Composable
private fun GroupRowItem(group: KryptGroup, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .testTag("group_item_${group.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        KryptAvatar(displayName = group.name, avatarBase64 = group.iconBase64, size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = KryptWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (group.locked) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Filled.Lock, "Locked", tint = KryptAmber, modifier = Modifier.size(13.dp))
                }
                if (group.suspended) {
                    Spacer(Modifier.width(6.dp))
                    KryptBadge(label = "SUSPENDED", color = KryptCrimson)
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = group.lastMessagePreview.ifBlank { group.description.ifBlank { "${group.memberUids.size} members" } },
                style = MaterialTheme.typography.bodyMedium,
                color = KryptSilver,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = formatRelativeTime(group.lastMessageAt?.toDate()?.time ?: 0L),
                style = MaterialTheme.typography.labelSmall,
                color = KryptAsh
            )
            Text(
                text = "${group.memberUids.size} mbrs",
                style = MaterialTheme.typography.labelSmall,
                color = KryptAsh
            )
        }
    }
    HorizontalDivider(color = KryptBorder.copy(alpha = 0.6f), modifier = Modifier.padding(start = 74.dp))
}

@Composable
fun KryptNewGroupScreen(
    viewModel: KryptMainViewModel,
    currentUser: UserProfile,
    onBack: () -> Unit,
    onGroupCreated: (String) -> Unit
) {
    val directory by viewModel.directoryUsers.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var discoverable by remember { mutableStateOf(false) }
    var slowModeSec by remember { mutableLongStateOf(0L) }
    val selectedMembers = remember { mutableStateListOf<String>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        KryptTopBar(title = "NEW GROUP VAULT", subtitle = "Sender Keys E2EE", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KryptTextField(
                value = name,
                onValueChange = { name = it },
                label = "GROUP NAME",
                placeholder = "Zero-Trust Engineering",
                modifier = Modifier.testTag("new_group_name_input")
            )
            KryptTextField(
                value = description,
                onValueChange = { description = it },
                label = "DESCRIPTION",
                placeholder = "Community guidelines & focus",
                singleLine = false,
                maxLines = 3
            )
            KryptSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Discoverable Community", style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                        Text("Allow verified members to find and join from Group List", style = MaterialTheme.typography.bodyMedium, color = KryptSilver)
                    }
                    Switch(
                        checked = discoverable,
                        onCheckedChange = { discoverable = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = KryptBlack, checkedTrackColor = KryptWhite)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text("Slow Mode Interval", style = MaterialTheme.typography.labelMedium, color = KryptSilver)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0L to "Off", 10L to "10s", 30L to "30s", 60L to "1m").forEach { (sec, label) ->
                        val active = slowModeSec == sec
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (active) KryptBlack else KryptWhite,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) KryptWhite else KryptCharcoalElevated)
                                .clickable { slowModeSec = sec }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            Text("ADD INITIAL MEMBERS", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
            val others = directory.filter { it.userId != currentUser.userId }
            others.forEach { user ->
                val checked = user.userId in selectedMembers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(KryptCharcoal)
                        .border(1.dp, KryptBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            if (checked) selectedMembers.remove(user.userId) else selectedMembers.add(user.userId)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KryptAvatar(displayName = user.displayName, size = 38.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.displayName, color = KryptWhite, style = MaterialTheme.typography.titleMedium)
                        Text("@${user.username}", color = KryptAsh, style = MaterialTheme.typography.labelSmall)
                    }
                    Checkbox(
                        checked = checked,
                        onCheckedChange = {
                            if (it) selectedMembers.add(user.userId) else selectedMembers.remove(user.userId)
                        },
                        colors = CheckboxDefaults.colors(checkedColor = KryptWhite, checkmarkColor = KryptBlack)
                    )
                }
            }

            KryptPrimaryButton(
                text = if (isBusy) "PROVISIONING GROUP..." else "CREATE GROUP",
                enabled = name.isNotBlank() && !isBusy,
                onClick = {
                    viewModel.createGroup(name, description, discoverable, slowModeSec, selectedMembers.toList(), onGroupCreated)
                },
                modifier = Modifier.testTag("create_group_submit_button")
            )
        }
    }
}

@Composable
fun KryptGroupChatScreen(
    groupId: String,
    viewModel: KryptMainViewModel,
    currentUser: UserProfile,
    onBack: () -> Unit,
    onOpenGroupInfo: () -> Unit
) {
    val context = LocalContext.current
    val group by viewModel.observeGroup(groupId).collectAsStateWithLifecycle(initialValue = null)
    val messages by viewModel.observeGroupMessages(groupId).collectAsStateWithLifecycle(initialValue = emptyList())

    var messageText by remember { mutableStateOf("") }
    var lastSentTimeMs by remember { mutableLongStateOf(0L) }
    var replyTarget by remember { mutableStateOf<DecryptedMessageUi?>(null) }
    var actionTarget by remember { mutableStateOf<DecryptedMessageUi?>(null) }
    var reportTarget by remember { mutableStateOf<DecryptedMessageUi?>(null) }
    var reportReason by remember { mutableStateOf("") }

    val voiceRecorder = remember { KryptVoiceRecorder(context) }
    val voicePlayer = remember { KryptVoicePlayer(context) }
    var isRecording by remember { mutableStateOf(false) }
    var playingMsgId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose { voiceRecorder.cancel(); voicePlayer.stop() }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        val g = group ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            val encoded = MediaAndVoiceHelper.encodeImageUriToBase64(context, uri)
            if (encoded != null) {
                viewModel.sendGroupMessage(
                    group = g,
                    senderProfile = currentUser,
                    plaintext = "Shared an encrypted group image",
                    messageType = MessageType.IMAGE,
                    mediaName = "image.jpg",
                    mediaMime = "image/jpeg",
                    mediaDataBase64 = encoded
                )
            }
        }
    }

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val g = group ?: return@rememberLauncherForActivityResult
        if (uri != null) {
            val doc = MediaAndVoiceHelper.readDocumentToBase64(context, uri)
            if (doc != null) {
                viewModel.sendGroupMessage(
                    group = g,
                    senderProfile = currentUser,
                    plaintext = "Document: ${doc.first}",
                    messageType = MessageType.DOCUMENT,
                    mediaName = doc.first,
                    mediaMime = doc.second,
                    mediaDataBase64 = doc.third
                )
            }
        }
    }

    val micPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) isRecording = voiceRecorder.startRecording()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) viewModel.markGroupRead(groupId, messages)
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        KryptTopBar(
            title = group?.name ?: "Group Chat",
            subtitle = "${group?.memberUids?.size ?: 0} members · Sender Keys E2EE",
            onBack = onBack,
            actions = {
                IconButton(
                    onClick = onOpenGroupInfo,
                    modifier = Modifier.size(48.dp).testTag("group_info_button")
                ) {
                    Icon(Icons.Filled.Info, "Group Info", tint = KryptWhite)
                }
            }
        )

        val g = group
        if (g != null && g.announcementText.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KryptCharcoal)
                    .border(0.5.dp, KryptBorder)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Campaign, "Announcement", tint = KryptAmber, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(g.announcementText, style = MaterialTheme.typography.bodyMedium, color = KryptWhite)
            }
        }

        if (g != null && g.slowModeSeconds > 0) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KryptCharcoalDeep)
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Timer, "Slow Mode", tint = KryptAsh, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(6.dp))
                Text("Slow Mode: ${g.slowModeSeconds}s between messages", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    KryptE2eeBadge("Group Sender Keys · AES-256-GCM")
                }
            }
            items(messages, key = { it.id }) { msg ->
                KryptMessageBubble(
                    message = msg,
                    isMine = msg.senderId == currentUser.userId,
                    isPlayingVoice = playingMsgId == msg.id,
                    showSenderName = true,
                    onPlayVoice = {
                        if (playingMsgId == msg.id) {
                            voicePlayer.stop(); playingMsgId = null
                        } else {
                            playingMsgId = msg.id
                            voicePlayer.playBase64(msg.mediaDataBase64) { playingMsgId = null }
                        }
                    },
                    onLongClick = { actionTarget = msg }
                )
            }
        }

        if (g != null && (g.suspended || (g.locked && currentUser.userId != g.ownerId && currentUser.userId !in g.adminUids))) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KryptCharcoal)
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (g.suspended) "Group suspended by Platform Moderation" else "Group is locked — only admins may post",
                    style = MaterialTheme.typography.labelMedium,
                    color = KryptAmber
                )
            }
        } else if (g != null) {
            HorizontalDivider(color = KryptBorder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KryptCharcoalDeep)
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Filled.Image, "Attach Image", tint = KryptSilver)
                }
                IconButton(onClick = { docPicker.launch(arrayOf("*/*")) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.AttachFile, "Attach File", tint = KryptSilver)
                }
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    placeholder = { Text(if (isRecording) "Recording..." else "Encrypted group message", color = KryptAsh) },
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = KryptCharcoal,
                        unfocusedContainerColor = KryptCharcoal,
                        focusedBorderColor = KryptBorderBright,
                        unfocusedBorderColor = KryptBorder,
                        focusedTextColor = KryptWhite,
                        unfocusedTextColor = KryptWhite
                    ),
                    modifier = Modifier.weight(1f).testTag("group_message_input")
                )
                Spacer(Modifier.width(6.dp))
                if (messageText.isBlank()) {
                    IconButton(
                        onClick = {
                            if (isRecording) {
                                isRecording = false
                                val rec = voiceRecorder.stopAndEncode()
                                if (rec != null) {
                                    viewModel.sendGroupMessage(
                                        group = g,
                                        senderProfile = currentUser,
                                        plaintext = "Voice note",
                                        messageType = MessageType.VOICE,
                                        mediaName = "voice.m4a",
                                        mediaMime = "audio/mp4",
                                        mediaDataBase64 = rec.first,
                                        mediaDurationMs = rec.second
                                    )
                                }
                            } else {
                                val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (granted) isRecording = voiceRecorder.startRecording()
                                else micPerm.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (isRecording) KryptCrimson else KryptCharcoalElevated)
                    ) {
                        Icon(if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic, "Voice note", tint = KryptWhite)
                    }
                } else {
                    IconButton(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val isPriv = currentUser.userId == g.ownerId || currentUser.userId in g.adminUids || currentUser.userId in g.moderatorUids
                            if (!isPriv && g.slowModeSeconds > 0 && now - lastSentTimeMs < g.slowModeSeconds * 1000L) {
                                val wait = ((g.slowModeSeconds * 1000L - (now - lastSentTimeMs)) / 1000L).coerceAtLeast(1L)
                                viewModel.postStatus("Slow mode active: wait ${wait}s")
                                return@IconButton
                            }
                            viewModel.sendGroupMessage(
                                group = g,
                                senderProfile = currentUser,
                                plaintext = messageText.trim(),
                                replyToMessageId = replyTarget?.id ?: "",
                                replyToPreview = replyTarget?.plaintext ?: ""
                            )
                            lastSentTimeMs = now
                            messageText = ""
                            replyTarget = null
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(KryptWhite)
                            .testTag("send_group_message_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = KryptBlack)
                    }
                }
            }
        }
    }

    if (actionTarget != null) {
        val msg = actionTarget!!
        AlertDialog(
            onDismissRequest = { actionTarget = null },
            containerColor = KryptCharcoalDeep,
            title = { Text("Group Message Actions", color = KryptWhite) },
            text = {
                Column {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        listOf("👍", "🔥", "🔒", "⚡").forEach { emoji ->
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier.clickable {
                                    viewModel.toggleGroupReaction(groupId, msg.id, emoji)
                                    actionTarget = null
                                }.padding(6.dp)
                            )
                        }
                    }
                    TextButton(onClick = { replyTarget = msg; actionTarget = null }) {
                        Text("Reply", color = KryptWhite)
                    }
                    TextButton(onClick = { reportTarget = msg; actionTarget = null }) {
                        Text("Report Message", color = KryptCrimson)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { actionTarget = null }) { Text("CLOSE", color = KryptSilver) }
            }
        )
    }

    if (reportTarget != null) {
        val msg = reportTarget!!
        AlertDialog(
            onDismissRequest = { reportTarget = null },
            containerColor = KryptCharcoalDeep,
            title = { Text("Report Group Message", color = KryptWhite) },
            text = {
                KryptTextField(
                    value = reportReason,
                    onValueChange = { reportReason = it },
                    label = "REASON",
                    placeholder = "Spam, harassment, or policy violation"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (reportReason.isNotBlank()) {
                        viewModel.submitSafetyReport(
                            targetType = "MESSAGE",
                            targetId = msg.id,
                            targetUserId = msg.senderId,
                            groupId = groupId,
                            reason = reportReason,
                            details = "Reported inside group ${group?.name ?: groupId}",
                            evidenceSnapshot = msg.plaintext
                        )
                        reportReason = ""
                        reportTarget = null
                    }
                }) { Text("SUBMIT", color = KryptCrimson) }
            },
            dismissButton = {
                TextButton(onClick = { reportTarget = null }) { Text("CANCEL", color = KryptSilver) }
            }
        )
    }
}

@Composable
fun KryptGroupInfoScreen(
    groupId: String,
    viewModel: KryptMainViewModel,
    currentUser: UserProfile,
    onBack: () -> Unit,
    onOpenMembers: () -> Unit,
    onDeleted: () -> Unit
) {
    val group by viewModel.observeGroup(groupId).collectAsStateWithLifecycle(initialValue = null)
    val g = group
    var editName by remember(g?.name) { mutableStateOf(g?.name ?: "") }
    var editDesc by remember(g?.description) { mutableStateOf(g?.description ?: "") }
    var announcement by remember(g?.announcementText) { mutableStateOf(g?.announcementText ?: "") }
    var reportReason by remember { mutableStateOf("") }
    var showReportDialog by remember { mutableStateOf(false) }

    val canManage = g != null && (
        currentUser.userId == g.ownerId ||
            currentUser.userId in g.adminUids ||
            currentUser.role == "OWNER"
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        KryptTopBar(title = "GROUP INFORMATION", subtitle = g?.name, onBack = onBack)
        if (g == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading...", color = KryptAsh)
            }
        } else {
            val isMuted = currentUser.userId in g.mutedByUids
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                KryptSurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KryptAvatar(displayName = g.name, size = 56.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(g.name, style = MaterialTheme.typography.titleLarge, color = KryptWhite)
                            Text("${g.memberUids.size} Members · Slow Mode ${g.slowModeSeconds}s", style = MaterialTheme.typography.labelMedium, color = KryptSilver)
                        }
                    }
                    if (g.description.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(g.description, style = MaterialTheme.typography.bodyMedium, color = KryptSilver)
                    }
                }

                KryptSurfaceCard {
                    Text("INVITE CODE", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = g.inviteCode.ifBlank { "No active code" },
                            style = MaterialTheme.typography.headlineMedium,
                            color = KryptWhite
                        )
                        if (canManage) {
                            TextButton(onClick = { viewModel.updateGroupSettings(groupId, regenInvite = true) }) {
                                Text("ROTATE CODE", color = KryptWhite)
                            }
                        }
                    }
                }

                KryptOutlinedButton(
                    text = "MEMBER ROLES & PERMISSIONS (${g.memberUids.size})",
                    onClick = onOpenMembers,
                    modifier = Modifier.testTag("open_group_members_button")
                )

                KryptSurfaceCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Mute Group Notifications", color = KryptWhite, style = MaterialTheme.typography.titleMedium)
                        Switch(
                            checked = isMuted,
                            onCheckedChange = { viewModel.toggleGroupMute(groupId, it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = KryptBlack, checkedTrackColor = KryptWhite)
                        )
                    }
                }

                if (canManage) {
                    KryptSurfaceCard {
                        Text("ADMINISTRATION", style = MaterialTheme.typography.labelSmall, color = KryptAmber)
                        Spacer(Modifier.height(10.dp))
                        KryptTextField(value = editName, onValueChange = { editName = it }, label = "GROUP NAME")
                        Spacer(Modifier.height(10.dp))
                        KryptTextField(value = editDesc, onValueChange = { editDesc = it }, label = "DESCRIPTION", singleLine = false, maxLines = 3)
                        Spacer(Modifier.height(10.dp))
                        KryptTextField(value = announcement, onValueChange = { announcement = it }, label = "PINNED ANNOUNCEMENT", singleLine = false, maxLines = 3)
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Lock Group (Admins Only)", color = KryptWhite)
                            Switch(
                                checked = g.locked,
                                onCheckedChange = { viewModel.updateGroupSettings(groupId, locked = it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = KryptBlack, checkedTrackColor = KryptAmber)
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        KryptPrimaryButton(
                            text = "SAVE GROUP SETTINGS",
                            onClick = { viewModel.updateGroupSettings(groupId, name = editName, description = editDesc, announcement = announcement) }
                        )
                    }
                }

                KryptOutlinedButton(
                    text = "REPORT GROUP TO SAFETY ENGINE",
                    accentColor = KryptCrimson,
                    onClick = { showReportDialog = true }
                )

                if (currentUser.userId == g.ownerId || currentUser.role == "OWNER") {
                    KryptOutlinedButton(
                        text = "DELETE GROUP",
                        accentColor = KryptCrimson,
                        onClick = { viewModel.deleteGroup(groupId, onDeleted) }
                    )
                }
            }
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            containerColor = KryptCharcoalDeep,
            title = { Text("Report Group", color = KryptWhite) },
            text = { KryptTextField(value = reportReason, onValueChange = { reportReason = it }, label = "REASON") },
            confirmButton = {
                TextButton(onClick = {
                    if (reportReason.isNotBlank()) {
                        viewModel.submitSafetyReport("GROUP", groupId, groupId = groupId, reason = reportReason, details = "Group report")
                        showReportDialog = false
                        reportReason = ""
                    }
                }) { Text("SUBMIT", color = KryptCrimson) }
            },
            dismissButton = { TextButton(onClick = { showReportDialog = false }) { Text("CANCEL", color = KryptSilver) } }
        )
    }
}

@Composable
fun KryptGroupMemberManagementScreen(
    groupId: String,
    viewModel: KryptMainViewModel,
    currentUser: UserProfile,
    onBack: () -> Unit
) {
    val group by viewModel.observeGroup(groupId).collectAsStateWithLifecycle(initialValue = null)
    val members by viewModel.observeGroupMembers(groupId).collectAsStateWithLifecycle(initialValue = emptyList())
    val directory by viewModel.directoryUsers.collectAsStateWithLifecycle()
    val userMap = remember(directory) { directory.associateBy { it.userId } }

    var showAddMember by remember { mutableStateOf(false) }

    val g = group
    val canAdmin = g != null && (currentUser.userId == g.ownerId || currentUser.userId in g.adminUids || currentUser.role == "OWNER")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        KryptTopBar(
            title = "GROUP MEMBERS",
            subtitle = "OWNER > ADMIN > MODERATOR > MEMBER",
            onBack = onBack,
            actions = {
                if (canAdmin) {
                    IconButton(onClick = { showAddMember = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.PersonAdd, "Add member", tint = KryptWhite)
                    }
                }
            }
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val uids = g?.memberUids.orEmpty()
            items(uids, key = { it }) { uid ->
                val user = userMap[uid]
                val role = when {
                    uid == g?.ownerId -> GroupRole.OWNER
                    uid in (g?.adminUids.orEmpty()) -> GroupRole.ADMIN
                    uid in (g?.moderatorUids.orEmpty()) -> GroupRole.MODERATOR
                    else -> GroupRole.MEMBER
                }
                KryptSurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        KryptAvatar(displayName = user?.displayName ?: uid, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(user?.displayName ?: uid, style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                            Text("@${user?.username ?: "member"}", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                        }
                        KryptBadge(
                            label = role.name,
                            color = when (role) {
                                GroupRole.OWNER -> KryptAmber
                                GroupRole.ADMIN -> KryptEmerald
                                GroupRole.MODERATOR -> KryptWhite
                                GroupRole.MEMBER -> KryptSilver
                            }
                        )
                    }
                    if (canAdmin && uid != g.ownerId && uid != currentUser.userId) {
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { viewModel.assignGroupRole(groupId, uid, GroupRole.ADMIN) }) {
                                Text("MAKE ADMIN", style = MaterialTheme.typography.labelSmall, color = KryptEmerald)
                            }
                            TextButton(onClick = { viewModel.assignGroupRole(groupId, uid, GroupRole.MODERATOR) }) {
                                Text("MAKE MOD", style = MaterialTheme.typography.labelSmall, color = KryptWhite)
                            }
                            TextButton(onClick = { viewModel.assignGroupRole(groupId, uid, GroupRole.MEMBER) }) {
                                Text("MEMBER", style = MaterialTheme.typography.labelSmall, color = KryptSilver)
                            }
                            TextButton(onClick = { viewModel.removeGroupMember(groupId, uid) }) {
                                Text("REMOVE", style = MaterialTheme.typography.labelSmall, color = KryptCrimson)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddMember) {
        val nonMembers = directory.filter { it.userId !in (g?.memberUids.orEmpty()) }
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            containerColor = KryptCharcoalDeep,
            title = { Text("Add Member to Group", color = KryptWhite) },
            text = {
                if (nonMembers.isEmpty()) {
                    Text("All directory members are already in this group.", color = KryptSilver)
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp)) {
                        items(nonMembers, key = { it.userId }) { u ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addGroupMember(groupId, u.userId)
                                        showAddMember = false
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KryptAvatar(displayName = u.displayName, size = 36.dp)
                                Spacer(Modifier.width(10.dp))
                                Text("${u.displayName} (@${u.username})", color = KryptWhite)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddMember = false }) { Text("CLOSE", color = KryptSilver) }
            }
        )
    }
}
