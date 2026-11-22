package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.GhostModeState
import com.example.data.model.Announcement
import com.example.data.model.CallType
import com.example.data.model.Conversation
import com.example.data.model.DecryptedMessageUi
import com.example.data.model.KryptGroup
import com.example.data.model.MessageType
import com.example.data.model.PlatformSettings
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.ui.components.GhostModeBanner
import com.example.ui.components.KryptAvatar
import com.example.ui.components.KryptBadge
import com.example.ui.components.KryptE2eeBadge
import com.example.ui.components.KryptEmptyState
import com.example.ui.components.KryptPrimaryButton
import com.example.ui.components.KryptSurfaceCard
import com.example.ui.components.KryptTextField
import com.example.ui.components.KryptTopBar
import com.example.ui.components.formatRelativeTime
import com.example.ui.theme.KryptAmber
import com.example.ui.theme.KryptAmberSurface
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
fun KryptHomeScreen(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    profile: UserProfile,
    ghostMode: GhostModeState,
    platformSettings: PlatformSettings,
    announcements: List<Announcement>,
    unreadNotificationCount: Int,
    isOwnerOrAdmin: Boolean,
    onOpenNotifications: () -> Unit,
    onOpenOwnerPanel: () -> Unit,
    onOpenPrivacySettings: () -> Unit,
    onDisableGhostMode: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        containerColor = KryptBlack,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KryptBlack)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KRYPT",
                        style = MaterialTheme.typography.headlineMedium,
                        color = KryptWhite,
                        modifier = Modifier.weight(1f)
                    )
                    if (isOwnerOrAdmin) {
                        IconButton(
                            onClick = onOpenOwnerPanel,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("owner_portal_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = "Owner Control Center",
                                tint = KryptAmber
                            )
                        }
                    }
                    Box {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("notifications_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                tint = KryptWhite
                            )
                        }
                        if (unreadNotificationCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(KryptEmerald)
                            )
                        }
                    }
                }
                GhostModeBanner(
                    ghostMode = ghostMode,
                    onDisable = onDisableGhostMode,
                    onOpenSettings = onOpenPrivacySettings
                )
                if (platformSettings.maintenanceMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KryptCrimson.copy(alpha = 0.2f))
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "PLATFORM MAINTENANCE MODE ACTIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = KryptCrimson
                        )
                    }
                } else if (announcements.isNotEmpty()) {
                    val topAnn = announcements.first()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(KryptCharcoal)
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        KryptBadge(label = "NOTICE", color = KryptWhite)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = topAnn.title,
                            style = MaterialTheme.typography.labelSmall,
                            color = KryptSilver,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                HorizontalDivider(color = KryptBorder, thickness = 1.dp)
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KryptCharcoalDeep)
                    .navigationBarsPadding()
            ) {
                HorizontalDivider(color = KryptBorder, thickness = 1.dp)
                NavigationBar(
                    containerColor = KryptCharcoalDeep,
                    tonalElevation = 0.dp
                ) {
                    val tabs = listOf(
                        Triple("Chats", Icons.Filled.Forum, Icons.Outlined.Forum),
                        Triple("Groups", Icons.Filled.Groups, Icons.Outlined.Groups),
                        Triple("Calls", Icons.Filled.Phone, Icons.Outlined.Phone),
                        Triple("Profile", Icons.Filled.Person, Icons.Outlined.Person)
                    )
                    tabs.forEachIndexed { idx, (label, filled, outlined) ->
                        NavigationBarItem(
                            selected = selectedTab == idx,
                            onClick = { onSelectTab(idx) },
                            icon = {
                                Icon(
                                    imageVector = if (selectedTab == idx) filled else outlined,
                                    contentDescription = label
                                )
                            },
                            label = {
                                Text(label, style = MaterialTheme.typography.labelSmall)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = KryptWhite,
                                selectedTextColor = KryptWhite,
                                indicatorColor = KryptCharcoalElevated,
                                unselectedIconColor = KryptAsh,
                                unselectedTextColor = KryptAsh
                            ),
                            modifier = Modifier.testTag("nav_tab_${label.lowercase()}")
                        )
                    }
                }
            }
        },
        content = content
    )
}

@Composable
fun KryptChatListScreen(
    viewModel: KryptMainViewModel,
    currentUser: UserProfile,
    onOpenChat: (String) -> Unit
) {
    val convState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val directory by viewModel.directoryUsers.collectAsStateWithLifecycle()
    val priv by viewModel.privateAccountState.collectAsStateWithLifecycle()
    val blocked = priv?.blockedUids.orEmpty()

    var searchQuery by remember { mutableStateOf("") }
    var showArchived by remember { mutableStateOf(false) }
    var showNewChatDialog by remember { mutableStateOf(false) }

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
                    placeholder = { Text("Search encrypted chats", color = KryptAsh) },
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
                    modifier = Modifier.weight(1f).testTag("chat_search_input")
                )
                IconButton(
                    onClick = { showArchived = !showArchived },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (showArchived) KryptCharcoalElevated else KryptCharcoal)
                        .border(1.dp, KryptBorder, RoundedCornerShape(10.dp))
                        .testTag("toggle_archived_chats")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Archive,
                        contentDescription = if (showArchived) "Show active chats" else "Show archived chats",
                        tint = if (showArchived) KryptWhite else KryptSilver
                    )
                }
            }

            when (val state = convState) {
                is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Decrypting index...", style = MaterialTheme.typography.labelMedium, color = KryptAsh)
                    }
                }
                is UiState.Error -> {
                    KryptEmptyState(title = "Sync Interrupted", subtitle = state.message)
                }
                is UiState.Success -> {
                    val userMap = remember(directory) { directory.associateBy { it.userId } }
                    val filtered = remember(state.data, searchQuery, showArchived, blocked, userMap) {
                        state.data.filter { conv ->
                            val peerUid = conv.participantUids.firstOrNull { it != currentUser.userId } ?: ""
                            if (peerUid in blocked) return@filter false
                            val archived = currentUser.userId in conv.archivedByUids
                            if (archived != showArchived) return@filter false
                            val peer = userMap[peerUid]
                            val matchQuery = searchQuery.isBlank() ||
                                (peer?.displayName?.contains(searchQuery, ignoreCase = true) == true) ||
                                (peer?.username?.contains(searchQuery, ignoreCase = true) == true)
                            matchQuery
                        }.sortedWith(
                            compareByDescending<Conversation> { currentUser.userId in it.pinnedByUids }
                                .thenByDescending { it.lastMessageAt?.toDate()?.time ?: 0L }
                        )
                    }

                    if (filtered.isEmpty()) {
                        KryptEmptyState(
                            title = if (showArchived) "No Archived Threads" else "No Active Chats",
                            subtitle = if (showArchived) "Archived conversations appear here."
                            else "Tap + to initiate an end-to-end encrypted 1:1 session."
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 88.dp)
                        ) {
                            items(filtered, key = { it.id }) { conv ->
                                val peerUid = conv.participantUids.firstOrNull { it != currentUser.userId } ?: ""
                                val peer = userMap[peerUid]
                                val unread = conv.unreadCounts[currentUser.userId] ?: 0L
                                val isPinned = currentUser.userId in conv.pinnedByUids
                                val peerOnline = peer != null && peer.onlineVisible && !peer.ghostModeActive

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onOpenChat(conv.id) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .testTag("chat_item_${conv.id}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    KryptAvatar(
                                        displayName = peer?.displayName ?: "Member",
                                        avatarBase64 = peer?.avatarBase64 ?: "",
                                        showOnlineDot = true,
                                        isOnline = peerOnline
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = peer?.displayName ?: "KRYPT Member",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = KryptWhite,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            if (isPinned) {
                                                Spacer(Modifier.width(6.dp))
                                                Icon(
                                                    imageVector = Icons.Filled.PushPin,
                                                    contentDescription = "Pinned",
                                                    tint = KryptSilver,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = conv.lastMessagePreviewHint.ifBlank { "Encrypted conversation" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (unread > 0) KryptWhite else KryptSilver,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = formatRelativeTime(conv.lastMessageAt?.toDate()?.time ?: 0L),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = KryptAsh
                                        )
                                        if (unread > 0) {
                                            Spacer(Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(KryptWhite)
                                                    .padding(horizontal = 7.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = unread.toString(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = KryptBlack
                                                )
                                            }
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    color = KryptBorder.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(start = 74.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showNewChatDialog = true },
            containerColor = KryptWhite,
            contentColor = KryptBlack,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .testTag("new_chat_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "New encrypted chat")
        }
    }

    if (showNewChatDialog) {
        val candidates = directory.filter { it.userId != currentUser.userId && it.userId !in blocked }
        AlertDialog(
            onDismissRequest = { showNewChatDialog = false },
            containerColor = KryptCharcoalDeep,
            title = { Text("Start Encrypted Chat", color = KryptWhite) },
            text = {
                if (candidates.isEmpty()) {
                    Text("No other verified members in directory yet.", color = KryptSilver)
                } else {
                    LazyColumn(modifier = Modifier.height(300.dp)) {
                        items(candidates, key = { it.userId }) { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showNewChatDialog = false
                                        viewModel.openOrCreateConversation(user.userId) { convId ->
                                            onOpenChat(convId)
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KryptAvatar(
                                    displayName = user.displayName,
                                    avatarBase64 = user.avatarBase64,
                                    size = 40.dp
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(user.displayName, color = KryptWhite, style = MaterialTheme.typography.titleMedium)
                                    Text("@${user.username}", color = KryptAsh, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNewChatDialog = false }) {
                    Text("CLOSE", color = KryptSilver)
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KryptIndividualChatScreen(
    conversationId: String,
    viewModel: KryptMainViewModel,
    currentUser: UserProfile,
    onBack: () -> Unit,
    onStartCall: (String) -> Unit
) {
    val context = LocalContext.current
    val convState by viewModel.conversationsState.collectAsStateWithLifecycle()
    val directory by viewModel.directoryUsers.collectAsStateWithLifecycle()
    val ghostMode by viewModel.ghostModeState.collectAsStateWithLifecycle()
    val myGroupsState by viewModel.myGroupsState.collectAsStateWithLifecycle()
    val messages by viewModel.observeChatMessages(conversationId).collectAsStateWithLifecycle(initialValue = emptyList())

    val conversation = (convState as? UiState.Success)?.data?.firstOrNull { it.id == conversationId }
    val peerUid = conversation?.participantUids?.firstOrNull { it != currentUser.userId } ?: ""
    val peer = directory.firstOrNull { it.userId == peerUid }

    var messageText by remember { mutableStateOf("") }
    var searchMode by remember { mutableStateOf(false) }
    var messageSearchQuery by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<DecryptedMessageUi?>(null) }
    var editingMessage by remember { mutableStateOf<DecryptedMessageUi?>(null) }
    var selectedMessageForActions by remember { mutableStateOf<DecryptedMessageUi?>(null) }
    var forwardMessage by remember { mutableStateOf<DecryptedMessageUi?>(null) }
    var reportTargetMsg by remember { mutableStateOf<DecryptedMessageUi?>(null) }
    var reportReason by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }

    val voiceRecorder = remember { KryptVoiceRecorder(context) }
    val voicePlayer = remember { KryptVoicePlayer(context) }
    var isRecording by remember { mutableStateOf(false) }
    var playingMsgId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.cancel()
            voicePlayer.stop()
        }
    }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null && conversation != null) {
            val encoded = MediaAndVoiceHelper.encodeImageUriToBase64(context, uri)
            if (encoded != null) {
                viewModel.sendDirectMessage(
                    conversationId = conversationId,
                    participantUids = conversation.participantUids,
                    senderProfile = currentUser,
                    plaintext = "Shared an encrypted image",
                    messageType = MessageType.IMAGE,
                    mediaName = "image.jpg",
                    mediaMime = "image/jpeg",
                    mediaDataBase64 = encoded
                )
            } else {
                viewModel.postStatus("Image too large or unreadable.")
            }
        }
    }

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null && conversation != null) {
            val doc = MediaAndVoiceHelper.readDocumentToBase64(context, uri)
            if (doc != null) {
                viewModel.sendDirectMessage(
                    conversationId = conversationId,
                    participantUids = conversation.participantUids,
                    senderProfile = currentUser,
                    plaintext = "Document: ${doc.first}",
                    messageType = MessageType.DOCUMENT,
                    mediaName = doc.first,
                    mediaMime = doc.second,
                    mediaDataBase64 = doc.third
                )
            } else {
                viewModel.postStatus("Document exceeds 300KB inline vault limit.")
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            isRecording = voiceRecorder.startRecording()
        } else {
            viewModel.postStatus("Microphone permission required for voice notes.")
        }
    }

    LaunchedEffect(messages.size, ghostMode) {
        if (messages.isNotEmpty()) {
            viewModel.markConversationRead(conversationId, currentUser, messages)
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val visibleMessages = remember(messages, messageSearchQuery) {
        if (messageSearchQuery.isBlank()) messages
        else messages.filter { it.plaintext.contains(messageSearchQuery, ignoreCase = true) }
    }
    val pinnedMessage = remember(messages) { messages.lastOrNull { it.pinned && !it.deleted } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        val peerStatus = when {
            peer == null -> "Encrypted Session"
            peer.ghostModeActive || !peer.onlineVisible -> "Last seen hidden"
            conversation?.typingUid == peerUid -> "typing..."
            else -> "Online · E2EE Verified"
        }
        KryptTopBar(
            title = peer?.displayName ?: "Encrypted Chat",
            subtitle = peerStatus,
            onBack = onBack,
            actions = {
                if (peer != null) {
                    IconButton(
                        onClick = {
                            viewModel.startCall(currentUser, peer, CallType.VOICE, onStartCall)
                        },
                        modifier = Modifier.size(48.dp).testTag("chat_voice_call_button")
                    ) {
                        Icon(Icons.Filled.Call, "Voice Call", tint = KryptWhite)
                    }
                    IconButton(
                        onClick = {
                            viewModel.startCall(currentUser, peer, CallType.VIDEO, onStartCall)
                        },
                        modifier = Modifier.size(48.dp).testTag("chat_video_call_button")
                    ) {
                        Icon(Icons.Filled.Videocam, "Video Call", tint = KryptWhite)
                    }
                }
                Box {
                    IconButton(onClick = { showMenu = true }, modifier = Modifier.size(48.dp)) {
                        Icon(Icons.Filled.MoreVert, "Chat Options", tint = KryptWhite)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = KryptCharcoalElevated
                    ) {
                        DropdownMenuItem(
                            text = { Text("Search Messages", color = KryptWhite) },
                            onClick = { showMenu = false; searchMode = !searchMode }
                        )
                        if (conversation != null) {
                            val isPinned = currentUser.userId in conversation.pinnedByUids
                            val isArchived = currentUser.userId in conversation.archivedByUids
                            val isMuted = currentUser.userId in conversation.mutedByUids
                            DropdownMenuItem(
                                text = { Text(if (isPinned) "Unpin Chat" else "Pin Chat", color = KryptWhite) },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleConversationFlag(conversationId, "pinnedByUids", !isPinned)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isMuted) "Unmute" else "Mute Notifications", color = KryptWhite) },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleConversationFlag(conversationId, "mutedByUids", !isMuted)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (isArchived) "Unarchive" else "Archive Chat", color = KryptWhite) },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleConversationFlag(conversationId, "archivedByUids", !isArchived)
                                }
                            )
                        }
                        if (peer != null) {
                            DropdownMenuItem(
                                text = { Text("Block User", color = KryptCrimson) },
                                onClick = {
                                    showMenu = false
                                    viewModel.setUserBlocked(peer.userId, true)
                                    onBack()
                                }
                            )
                        }
                    }
                }
            }
        )

        if (ghostMode.isEffectivelyActive()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KryptAmberSurface)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.VisibilityOff, "Ghost Mode", tint = KryptAmber, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Ghost Mode: Reading silently without sending read receipts or typing",
                    style = MaterialTheme.typography.labelSmall,
                    color = KryptAmber
                )
            }
        }

        if (searchMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KryptCharcoalDeep)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KryptTextField(
                    value = messageSearchQuery,
                    onValueChange = { messageSearchQuery = it },
                    label = "Search in conversation",
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { searchMode = false; messageSearchQuery = "" }) {
                    Icon(Icons.Filled.Close, "Close search", tint = KryptWhite)
                }
            }
        }

        if (pinnedMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KryptCharcoal)
                    .border(0.5.dp, KryptBorder)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.PushPin, "Pinned message", tint = KryptSilver, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = pinnedMessage.plaintext,
                    style = MaterialTheme.typography.bodyMedium,
                    color = KryptWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    KryptE2eeBadge("Messages secured with ECDH P-256 + AES-256-GCM")
                }
            }
            items(visibleMessages, key = { it.id }) { msg ->
                val isMine = msg.senderId == currentUser.userId
                KryptMessageBubble(
                    message = msg,
                    isMine = isMine,
                    isPlayingVoice = playingMsgId == msg.id,
                    onPlayVoice = {
                        if (playingMsgId == msg.id) {
                            voicePlayer.stop()
                            playingMsgId = null
                        } else {
                            playingMsgId = msg.id
                            voicePlayer.playBase64(msg.mediaDataBase64) { playingMsgId = null }
                        }
                    },
                    onLongClick = { selectedMessageForActions = msg }
                )
            }
        }

        if (replyTarget != null || editingMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KryptCharcoal)
                    .border(0.5.dp, KryptBorder)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.AutoMirrored.Filled.Reply, "Reply or Edit", tint = KryptSilver, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (editingMessage != null) "Editing: ${editingMessage!!.plaintext}" else "Replying to: ${replyTarget!!.plaintext}",
                    style = MaterialTheme.typography.labelMedium,
                    color = KryptSilver,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { replyTarget = null; editingMessage = null; messageText = "" }) {
                    Icon(Icons.Filled.Close, "Cancel", tint = KryptWhite, modifier = Modifier.size(18.dp))
                }
            }
        }

        HorizontalDivider(color = KryptBorder)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KryptCharcoalDeep)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.Image, "Attach Image", tint = KryptSilver)
            }
            IconButton(
                onClick = { docPicker.launch(arrayOf("*/*")) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Filled.AttachFile, "Attach Document", tint = KryptSilver)
            }

            OutlinedTextField(
                value = messageText,
                onValueChange = {
                    messageText = it
                    viewModel.setTyping(conversationId, it.isNotBlank())
                },
                placeholder = { Text(if (isRecording) "Recording voice..." else "Encrypted message", color = KryptAsh) },
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
                modifier = Modifier.weight(1f).testTag("chat_message_input")
            )
            Spacer(Modifier.width(6.dp))
            if (messageText.isBlank()) {
                IconButton(
                    onClick = {
                        if (isRecording) {
                            isRecording = false
                            val recorded = voiceRecorder.stopAndEncode()
                            if (recorded != null && conversation != null) {
                                viewModel.sendDirectMessage(
                                    conversationId = conversationId,
                                    participantUids = conversation.participantUids,
                                    senderProfile = currentUser,
                                    plaintext = "Voice note (${recorded.second / 1000L}s)",
                                    messageType = MessageType.VOICE,
                                    mediaName = "voice.m4a",
                                    mediaMime = "audio/mp4",
                                    mediaDataBase64 = recorded.first,
                                    mediaDurationMs = recorded.second
                                )
                            }
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                isRecording = voiceRecorder.startRecording()
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isRecording) KryptCrimson else KryptCharcoalElevated)
                        .testTag("voice_record_button")
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (isRecording) "Stop and send voice note" else "Record voice note",
                        tint = KryptWhite
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        val txt = messageText.trim()
                        if (txt.isNotEmpty() && conversation != null) {
                            if (editingMessage != null) {
                                viewModel.editDirectMessage(
                                    conversationId,
                                    conversation.participantUids,
                                    editingMessage!!.id,
                                    txt
                                )
                                editingMessage = null
                            } else {
                                viewModel.sendDirectMessage(
                                    conversationId = conversationId,
                                    participantUids = conversation.participantUids,
                                    senderProfile = currentUser,
                                    plaintext = txt,
                                    replyToMessageId = replyTarget?.id ?: "",
                                    replyToPreview = replyTarget?.plaintext ?: "",
                                    recentTexts = messages.filter { it.senderId == currentUser.userId }.map { it.plaintext }
                                )
                                replyTarget = null
                            }
                            messageText = ""
                            viewModel.setTyping(conversationId, false)
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(KryptWhite)
                        .testTag("send_message_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send message", tint = KryptBlack)
                }
            }
        }
    }

    if (selectedMessageForActions != null) {
        val msg = selectedMessageForActions!!
        AlertDialog(
            onDismissRequest = { selectedMessageForActions = null },
            containerColor = KryptCharcoalDeep,
            title = { Text("Message Actions", color = KryptWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("👍", "🔥", "🔒", "⚡", "👀").forEach { emoji ->
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.headlineMedium,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.toggleDirectReaction(conversationId, msg.id, emoji)
                                        selectedMessageForActions = null
                                    }
                                    .padding(6.dp)
                            )
                        }
                    }
                    HorizontalDivider(color = KryptBorder)
                    TextButton(onClick = { replyTarget = msg; selectedMessageForActions = null }) {
                        Text("Reply", color = KryptWhite)
                    }
                    TextButton(onClick = { forwardMessage = msg; selectedMessageForActions = null }) {
                        Text("Forward", color = KryptWhite)
                    }
                    TextButton(onClick = {
                        viewModel.togglePinDirectMessage(conversationId, msg.id, !msg.pinned)
                        selectedMessageForActions = null
                    }) {
                        Text(if (msg.pinned) "Unpin" else "Pin Message", color = KryptWhite)
                    }
                    if (msg.senderId == currentUser.userId && !msg.deleted) {
                        if (msg.messageType == MessageType.TEXT) {
                            TextButton(onClick = {
                                editingMessage = msg
                                messageText = msg.plaintext
                                selectedMessageForActions = null
                            }) {
                                Text("Edit Message", color = KryptWhite)
                            }
                        }
                        TextButton(onClick = {
                            viewModel.deleteDirectMessage(conversationId, msg.id)
                            selectedMessageForActions = null
                        }) {
                            Text("Delete Message", color = KryptCrimson)
                        }
                    } else {
                        TextButton(onClick = {
                            reportTargetMsg = msg
                            selectedMessageForActions = null
                        }) {
                            Text("Report to Safety Engine", color = KryptCrimson)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedMessageForActions = null }) {
                    Text("CLOSE", color = KryptSilver)
                }
            }
        )
    }

    if (forwardMessage != null) {
        val fwd = forwardMessage!!
        val convList = (convState as? UiState.Success)?.data.orEmpty()
        AlertDialog(
            onDismissRequest = { forwardMessage = null },
            containerColor = KryptCharcoalDeep,
            title = { Text("Forward Message", color = KryptWhite) },
            text = {
                LazyColumn(modifier = Modifier.height(240.dp)) {
                    items(convList, key = { it.id }) { targetConv ->
                        val targetPeer = targetConv.participantUids.firstOrNull { it != currentUser.userId } ?: ""
                        val name = directory.firstOrNull { it.userId == targetPeer }?.displayName ?: targetConv.id
                        TextButton(
                            onClick = {
                                viewModel.sendDirectMessage(
                                    conversationId = targetConv.id,
                                    participantUids = targetConv.participantUids,
                                    senderProfile = currentUser,
                                    plaintext = fwd.plaintext,
                                    messageType = fwd.messageType,
                                    mediaName = fwd.mediaName,
                                    mediaMime = fwd.mediaMime,
                                    mediaDataBase64 = fwd.mediaDataBase64,
                                    forwardedFrom = peer?.displayName ?: "Member"
                                )
                                forwardMessage = null
                                viewModel.postStatus("Forwarded to $name")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Chat: $name", color = KryptWhite)
                        }
                    }
                    val groups = (myGroupsState as? UiState.Success)?.data.orEmpty()
                    items(groups, key = { it.id }) { grp ->
                        TextButton(
                            onClick = {
                                viewModel.sendGroupMessage(
                                    group = grp,
                                    senderProfile = currentUser,
                                    plaintext = fwd.plaintext,
                                    messageType = fwd.messageType,
                                    mediaName = fwd.mediaName,
                                    mediaMime = fwd.mediaMime,
                                    mediaDataBase64 = fwd.mediaDataBase64,
                                    forwardedFrom = peer?.displayName ?: "Member"
                                )
                                forwardMessage = null
                                viewModel.postStatus("Forwarded to ${grp.name}")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Group: ${grp.name}", color = KryptWhite)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { forwardMessage = null }) { Text("CANCEL", color = KryptSilver) }
            }
        )
    }

    if (reportTargetMsg != null) {
        val target = reportTargetMsg!!
        AlertDialog(
            onDismissRequest = { reportTargetMsg = null },
            containerColor = KryptCharcoalDeep,
            title = { Text("Report Message", color = KryptWhite) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "An immutable incident evidence snapshot of this decrypted message will be attached for Safety Review.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KryptSilver
                    )
                    KryptTextField(
                        value = reportReason,
                        onValueChange = { reportReason = it },
                        label = "REASON",
                        placeholder = "Harassment, spam, or threat"
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (reportReason.isNotBlank()) {
                        viewModel.submitSafetyReport(
                            targetType = "MESSAGE",
                            targetId = target.id,
                            targetUserId = target.senderId,
                            conversationId = conversationId,
                            reason = reportReason,
                            details = "Reported from 1:1 chat",
                            evidenceSnapshot = target.plaintext
                        )
                        reportReason = ""
                        reportTargetMsg = null
                    }
                }) { Text("SUBMIT REPORT", color = KryptCrimson) }
            },
            dismissButton = {
                TextButton(onClick = { reportTargetMsg = null }) { Text("CANCEL", color = KryptSilver) }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KryptMessageBubble(
    message: DecryptedMessageUi,
    isMine: Boolean,
    isPlayingVoice: Boolean,
    onPlayVoice: () -> Unit,
    onLongClick: () -> Unit,
    showSenderName: Boolean = false
) {
    val containerColor = if (isMine) KryptCharcoalElevated else KryptCharcoal
    val borderColor = if (message.pinned) KryptAmber.copy(alpha = 0.6f) else KryptBorder

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isMine) Alignment.End else Alignment.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 12.dp,
                topEnd = 12.dp,
                bottomStart = if (isMine) 12.dp else 4.dp,
                bottomEnd = if (isMine) 4.dp else 12.dp
            ),
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = Modifier
                .widthIn(max = 310.dp)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
        ) {
            Column(modifier = Modifier.padding(11.dp)) {
                if (showSenderName && !isMine && message.senderName.isNotBlank()) {
                    Text(
                        text = message.senderName,
                        style = MaterialTheme.typography.labelMedium,
                        color = KryptSilver
                    )
                    Spacer(Modifier.height(3.dp))
                }
                if (message.forwardedFrom.isNotBlank()) {
                    Text(
                        text = "Forwarded from ${message.forwardedFrom}",
                        style = MaterialTheme.typography.labelSmall,
                        color = KryptAsh,
                        fontStyle = FontStyle.Italic
                    )
                    Spacer(Modifier.height(4.dp))
                }
                if (message.replyToPreview.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(KryptBlack.copy(alpha = 0.45f))
                            .padding(horizontal = 8.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = message.replyToPreview,
                            style = MaterialTheme.typography.labelSmall,
                            color = KryptSilver,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }

                when {
                    message.deleted -> {
                        Text(
                            text = message.plaintext,
                            style = MaterialTheme.typography.bodyMedium,
                            color = KryptAsh,
                            fontStyle = FontStyle.Italic
                        )
                    }
                    message.messageType == MessageType.IMAGE && message.mediaDataBase64.isNotBlank() -> {
                        val bmp = remember(message.mediaDataBase64) {
                            MediaAndVoiceHelper.decodeBase64ToBitmap(message.mediaDataBase64)?.asImageBitmap()
                        }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = "Encrypted image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        Text(message.plaintext, style = MaterialTheme.typography.bodyMedium, color = KryptWhite)
                    }
                    message.messageType == MessageType.VOICE && message.mediaDataBase64.isNotBlank() -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(KryptBlack.copy(alpha = 0.4f))
                                .clickable(onClick = onPlayVoice)
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlayingVoice) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                                contentDescription = "Play voice note",
                                tint = KryptWhite,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Voice Note · ${(message.mediaDurationMs / 1000L).coerceAtLeast(1L)}s",
                                style = MaterialTheme.typography.labelMedium,
                                color = KryptWhite
                            )
                        }
                    }
                    message.messageType == MessageType.DOCUMENT -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Description, "Document", tint = KryptSilver, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = message.mediaName.ifBlank { message.plaintext },
                                style = MaterialTheme.typography.bodyMedium,
                                color = KryptWhite
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = message.plaintext,
                            style = MaterialTheme.typography.bodyLarge,
                            color = KryptWhite
                        )
                    }
                }

                Spacer(Modifier.height(5.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    if (message.edited) {
                        Text("edited · ", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                    }
                    Text(
                        text = formatRelativeTime(message.createdAtMillis),
                        style = MaterialTheme.typography.labelSmall,
                        color = KryptAsh
                    )
                    if (isMine) {
                        Spacer(Modifier.width(4.dp))
                        val readByPeer = message.readByUids.any { it != message.senderId }
                        val delivered = message.deliveredToUids.any { it != message.senderId }
                        Icon(
                            imageVector = if (readByPeer || delivered) Icons.Filled.DoneAll else Icons.Filled.Check,
                            contentDescription = if (readByPeer) "Read" else "Sent",
                            tint = if (readByPeer) KryptEmerald else KryptAsh,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                message.reactions.values.groupingBy { it }.eachCount().forEach { (emoji, count) ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = KryptCharcoal,
                        border = BorderStroke(0.5.dp, KryptBorder)
                    ) {
                        Text(
                            text = "$emoji $count",
                            style = MaterialTheme.typography.labelSmall,
                            color = KryptWhite,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
