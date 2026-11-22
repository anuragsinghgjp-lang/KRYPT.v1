package com.example.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.model.AccountStatus
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.repository.KryptAuthRepository
import com.example.data.repository.KryptGroupRepository
import com.example.data.repository.KryptMessagingRepository
import com.example.data.repository.KryptSafetyAndOwnerRepository
import com.example.ui.components.KryptBadge
import com.example.ui.components.KryptOutlinedButton
import com.example.ui.components.KryptPrimaryButton
import com.example.ui.components.KryptSurfaceCard
import com.example.ui.components.KryptTextField
import com.example.ui.screens.KryptAccessPassManagementScreen
import com.example.ui.screens.KryptAccountRecoveryScreen
import com.example.ui.screens.KryptAuditLogsScreen
import com.example.ui.screens.KryptAuthorizedMessageReviewScreen
import com.example.ui.screens.KryptBlockedUsersScreen
import com.example.ui.screens.KryptCallScreen
import com.example.ui.screens.KryptCallsScreen
import com.example.ui.screens.KryptChatListScreen
import com.example.ui.screens.KryptGroupChatScreen
import com.example.ui.screens.KryptGroupInfoScreen
import com.example.ui.screens.KryptGroupListScreen
import com.example.ui.screens.KryptGroupMemberManagementScreen
import com.example.ui.screens.KryptHomeScreen
import com.example.ui.screens.KryptIndividualChatScreen
import com.example.ui.screens.KryptLoginScreen
import com.example.ui.screens.KryptModerationQueueScreen
import com.example.ui.screens.KryptNewGroupScreen
import com.example.ui.screens.KryptNotificationsScreen
import com.example.ui.screens.KryptOwnerDashboardScreen
import com.example.ui.screens.KryptOwnerGroupManagementScreen
import com.example.ui.screens.KryptOwnerUserManagementScreen
import com.example.ui.screens.KryptPlatformSettingsScreen
import com.example.ui.screens.KryptPrivacySettingsScreen
import com.example.ui.screens.KryptReportsScreen
import com.example.ui.screens.KryptSettingsScreen
import com.example.ui.screens.KryptSpecialPassAndProfileSetupScreen
import com.example.ui.screens.KryptSplashScreen
import com.example.ui.screens.KryptUserProfileScreen
import com.example.ui.screens.KryptWelcomeScreen
import com.example.ui.theme.KryptAmber
import com.example.ui.theme.KryptBlack
import com.example.ui.theme.KryptCharcoalElevated
import com.example.ui.theme.KryptCrimson
import com.example.ui.theme.KryptSilver
import com.example.ui.theme.KryptWhite
import com.example.ui.viewmodel.KryptMainViewModel
import com.example.ui.viewmodel.OwnerControlViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.launch

object KryptRoutes {
    const val SPLASH = "splash"
    const val WELCOME = "welcome"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val RECOVERY = "recovery"

    const val HOME = "home"
    const val CHAT = "chat/{conversationId}"
    const val GROUP_CHAT = "group_chat/{groupId}"
    const val GROUP_INFO = "group_info/{groupId}"
    const val GROUP_MEMBERS = "group_members/{groupId}"
    const val NEW_GROUP = "new_group"
    const val CALL = "call/{callId}"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val PRIVACY_SETTINGS = "privacy_settings"
    const val BLOCKED_USERS = "blocked_users"
    const val REPORTS = "reports"

    const val OWNER_DASHBOARD = "owner_dashboard"
    const val OWNER_USERS = "owner_users"
    const val OWNER_GROUPS = "owner_groups"
    const val OWNER_REVIEW = "owner_review"
    const val OWNER_PASSES = "owner_passes"
    const val OWNER_MODERATION = "owner_moderation"
    const val OWNER_AUDIT = "owner_audit"
    const val OWNER_PLATFORM = "owner_platform"

    fun chat(conversationId: String) = "chat/$conversationId"
    fun groupChat(groupId: String) = "group_chat/$groupId"
    fun groupInfo(groupId: String) = "group_info/$groupId"
    fun groupMembers(groupId: String) = "group_members/$groupId"
    fun call(callId: String) = "call/$callId"
}

@Composable
fun KryptAppRoot() {
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUser by remember { mutableStateOf(auth.currentUser) }
    var splashCompleted by rememberSaveable { mutableStateOf(false) }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val user = currentUser
    if (user == null || !splashCompleted) {
        KryptAuthFlow(
            onSplashResolvedAuthenticated = {
                currentUser = auth.currentUser
                splashCompleted = true
            },
            onAuthCompleted = {
                currentUser = auth.currentUser
                splashCompleted = true
            }
        )
    } else {
        key(user.uid) {
            KryptAuthenticatedGraph(user = user)
        }
    }
}

@Composable
private fun KryptAuthFlow(
    onSplashResolvedAuthenticated: () -> Unit,
    onAuthCompleted: () -> Unit
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = KryptRoutes.SPLASH
    ) {
        composable(KryptRoutes.SPLASH) {
            KryptSplashScreen(
                onResolvedUnauthenticated = {
                    navController.navigate(KryptRoutes.WELCOME) {
                        popUpTo(KryptRoutes.SPLASH) { inclusive = true }
                    }
                },
                onResolvedAuthenticated = onSplashResolvedAuthenticated
            )
        }
        composable(KryptRoutes.WELCOME) {
            KryptWelcomeScreen(
                onOpenLogin = { navController.navigate(KryptRoutes.LOGIN) },
                onOpenRegistration = { navController.navigate(KryptRoutes.REGISTER) },
                onOpenRecovery = { navController.navigate(KryptRoutes.RECOVERY) }
            )
        }
        composable(KryptRoutes.LOGIN) {
            KryptLoginScreen(
                isRegistrationFlow = false,
                onBack = { navController.popBackStack() },
                onAuthSuccess = onAuthCompleted,
                onOpenRecovery = { navController.navigate(KryptRoutes.RECOVERY) }
            )
        }
        composable(KryptRoutes.REGISTER) {
            KryptLoginScreen(
                isRegistrationFlow = true,
                onBack = { navController.popBackStack() },
                onAuthSuccess = onAuthCompleted,
                onOpenRecovery = { navController.navigate(KryptRoutes.RECOVERY) }
            )
        }
        composable(KryptRoutes.RECOVERY) {
            KryptAccountRecoveryScreen(onBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun KryptAuthenticatedGraph(user: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    val authRepo = remember(user.uid) { KryptAuthRepository(context) }
    val messagingRepo = remember(user.uid) { KryptMessagingRepository(context) }
    val groupRepo = remember(user.uid) { KryptGroupRepository(context) }
    val safetyRepo = remember(user.uid) { KryptSafetyAndOwnerRepository(context) }

    val mainVm: KryptMainViewModel = viewModel(
        key = "main_${user.uid}",
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return KryptMainViewModel(user.uid, authRepo, messagingRepo, groupRepo, safetyRepo) as T
            }
        }
    )

    val profileState by mainVm.userProfileState.collectAsStateWithLifecycle()
    val ghostMode by mainVm.ghostModeState.collectAsStateWithLifecycle()
    val platformSettings by mainVm.platformSettings.collectAsStateWithLifecycle()
    val announcements by mainVm.announcements.collectAsStateWithLifecycle()
    val notifications by mainVm.notifications.collectAsStateWithLifecycle()
    val statusMessage by mainVm.statusMessage.collectAsStateWithLifecycle()
    val isBusy by mainVm.isBusy.collectAsStateWithLifecycle()
    val passCooldownUntil by mainVm.passCooldownUntil.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(statusMessage) {
        if (!statusMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(statusMessage!!)
            mainVm.clearStatusMessage()
        }
    }

    val performSignOut: () -> Unit = {
        scope.launch {
            authRepo.signOutSafely(credentialManager)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(KryptBlack)) {
        when (val state = profileState) {
            is UiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = KryptWhite, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(14.dp))
                        Text("Unlocking KRYPT Vault...", style = MaterialTheme.typography.labelMedium, color = KryptSilver)
                    }
                }
            }

            is UiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Vault Synchronization Error", style = MaterialTheme.typography.headlineMedium, color = KryptWhite)
                    Spacer(Modifier.height(8.dp))
                    Text(state.message, style = MaterialTheme.typography.bodyMedium, color = KryptCrimson)
                    Spacer(Modifier.height(20.dp))
                    KryptOutlinedButton(text = "SIGN OUT", accentColor = KryptCrimson, onClick = performSignOut)
                }
            }

            is UiState.Success -> {
                val profile = state.data
                if (profile == null) {
                    KryptSpecialPassAndProfileSetupScreen(
                        userEmail = user.email ?: "",
                        isBootstrappedOwner = mainVm.isBootstrappedOwner(),
                        isBusy = isBusy,
                        statusMessage = statusMessage,
                        cooldownUntilEpochMs = passCooldownUntil,
                        onRedeemPass = { code, uname, dname, bio ->
                            mainVm.redeemAccessPass(code, uname, dname, bio) {
                                mainVm.postStatus("Welcome to KRYPT")
                            }
                        },
                        onBootstrapOwner = { uname, dname, bio ->
                            mainVm.initializeOwnerProfile(uname, dname, bio) {
                                mainVm.postStatus("Owner Control Center activated")
                            }
                        },
                        onSignOut = performSignOut
                    )
                } else if (profile.accountStatus == AccountStatus.SUSPENDED.name ||
                    profile.accountStatus == AccountStatus.BANNED.name
                ) {
                    KryptAccountSuspendedGate(
                        profile = profile,
                        mainVm = mainVm,
                        onSignOut = performSignOut
                    )
                } else {
                    val isOwnerOrAdmin = profile.role == "OWNER" || mainVm.isBootstrappedOwner()
                    KryptActiveMemberShell(
                        user = user,
                        profile = profile,
                        isOwnerOrAdmin = isOwnerOrAdmin,
                        mainVm = mainVm,
                        safetyRepo = safetyRepo,
                        ghostMode = ghostMode,
                        platformSettings = platformSettings,
                        announcements = announcements,
                        unreadNotifs = notifications.count { !it.read },
                        snackbarHostState = snackbarHostState,
                        onSignOut = performSignOut
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 72.dp)
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = KryptCharcoalElevated,
                contentColor = KryptWhite
            )
        }
    }
}

@Composable
private fun KryptActiveMemberShell(
    user: FirebaseUser,
    profile: UserProfile,
    isOwnerOrAdmin: Boolean,
    mainVm: KryptMainViewModel,
    safetyRepo: KryptSafetyAndOwnerRepository,
    ghostMode: com.example.data.local.GhostModeState,
    platformSettings: com.example.data.model.PlatformSettings,
    announcements: List<com.example.data.model.Announcement>,
    unreadNotifs: Int,
    snackbarHostState: SnackbarHostState,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val navController = rememberNavController()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    val ownerVm: OwnerControlViewModel? = if (isOwnerOrAdmin) {
        val vm: OwnerControlViewModel = viewModel(
            key = "owner_${user.uid}",
            factory = object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OwnerControlViewModel(safetyRepo) as T
                }
            }
        )
        val feedback by vm.ownerFeedback.collectAsStateWithLifecycle()
        LaunchedEffect(feedback) {
            if (!feedback.isNullOrBlank()) {
                snackbarHostState.showSnackbar(feedback!!)
                vm.clearFeedback()
            }
        }
        vm
    } else null

    NavHost(
        navController = navController,
        startDestination = KryptRoutes.HOME
    ) {
        composable(KryptRoutes.HOME) {
            BackHandler(enabled = selectedTab != 0) {
                selectedTab = 0
            }
            KryptHomeScreen(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it },
                profile = profile,
                ghostMode = ghostMode,
                platformSettings = platformSettings,
                announcements = announcements,
                unreadNotificationCount = unreadNotifs,
                isOwnerOrAdmin = isOwnerOrAdmin,
                onOpenNotifications = { navController.navigate(KryptRoutes.NOTIFICATIONS) },
                onOpenOwnerPanel = {
                    if (isOwnerOrAdmin) navController.navigate(KryptRoutes.OWNER_DASHBOARD)
                },
                onOpenPrivacySettings = { navController.navigate(KryptRoutes.PRIVACY_SETTINGS) },
                onDisableGhostMode = { mainVm.setGhostMode(false, null, "Manual") }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedTab) {
                        0 -> KryptChatListScreen(
                            viewModel = mainVm,
                            currentUser = profile,
                            onOpenChat = { convId -> navController.navigate(KryptRoutes.chat(convId)) }
                        )
                        1 -> KryptGroupListScreen(
                            viewModel = mainVm,
                            currentUser = profile,
                            onOpenGroup = { gid -> navController.navigate(KryptRoutes.groupChat(gid)) },
                            onCreateGroup = { navController.navigate(KryptRoutes.NEW_GROUP) }
                        )
                        2 -> KryptCallsScreen(
                            viewModel = mainVm,
                            currentUser = profile,
                            onOpenCall = { cid -> navController.navigate(KryptRoutes.call(cid)) }
                        )
                        else -> KryptUserProfileScreen(
                            viewModel = mainVm,
                            currentUser = profile,
                            isOwnerOrAdmin = isOwnerOrAdmin,
                            onOpenSettings = { navController.navigate(KryptRoutes.SETTINGS) },
                            onOpenPrivacySettings = { navController.navigate(KryptRoutes.PRIVACY_SETTINGS) },
                            onOpenBlockedUsers = { navController.navigate(KryptRoutes.BLOCKED_USERS) },
                            onOpenReports = { navController.navigate(KryptRoutes.REPORTS) },
                            onOpenOwnerPanel = {
                                if (isOwnerOrAdmin) navController.navigate(KryptRoutes.OWNER_DASHBOARD)
                            },
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }

        composable(
            route = KryptRoutes.CHAT,
            arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
        ) { entry ->
            val convId = entry.arguments?.getString("conversationId") ?: return@composable
            KryptIndividualChatScreen(
                conversationId = convId,
                viewModel = mainVm,
                currentUser = profile,
                onBack = { navController.popBackStack() },
                onStartCall = { callId -> navController.navigate(KryptRoutes.call(callId)) }
            )
        }

        composable(
            route = KryptRoutes.GROUP_CHAT,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { entry ->
            val gid = entry.arguments?.getString("groupId") ?: return@composable
            KryptGroupChatScreen(
                groupId = gid,
                viewModel = mainVm,
                currentUser = profile,
                onBack = { navController.popBackStack() },
                onOpenGroupInfo = { navController.navigate(KryptRoutes.groupInfo(gid)) }
            )
        }

        composable(
            route = KryptRoutes.GROUP_INFO,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { entry ->
            val gid = entry.arguments?.getString("groupId") ?: return@composable
            KryptGroupInfoScreen(
                groupId = gid,
                viewModel = mainVm,
                currentUser = profile,
                onBack = { navController.popBackStack() },
                onOpenMembers = { navController.navigate(KryptRoutes.groupMembers(gid)) },
                onDeleted = {
                    navController.popBackStack(KryptRoutes.HOME, inclusive = false)
                }
            )
        }

        composable(
            route = KryptRoutes.GROUP_MEMBERS,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { entry ->
            val gid = entry.arguments?.getString("groupId") ?: return@composable
            KryptGroupMemberManagementScreen(
                groupId = gid,
                viewModel = mainVm,
                currentUser = profile,
                onBack = { navController.popBackStack() }
            )
        }

        composable(KryptRoutes.NEW_GROUP) {
            KryptNewGroupScreen(
                viewModel = mainVm,
                currentUser = profile,
                onBack = { navController.popBackStack() },
                onGroupCreated = { gid ->
                    navController.navigate(KryptRoutes.groupChat(gid)) {
                        popUpTo(KryptRoutes.NEW_GROUP) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = KryptRoutes.CALL,
            arguments = listOf(navArgument("callId") { type = NavType.StringType })
        ) { entry ->
            val callId = entry.arguments?.getString("callId") ?: return@composable
            KryptCallScreen(
                callId = callId,
                viewModel = mainVm,
                currentUser = profile,
                onExitCall = { navController.popBackStack() }
            )
        }

        composable(KryptRoutes.NOTIFICATIONS) {
            KryptNotificationsScreen(viewModel = mainVm, onBack = { navController.popBackStack() })
        }

        composable(KryptRoutes.SETTINGS) {
            KryptSettingsScreen(
                viewModel = mainVm,
                currentUser = profile,
                onBack = { navController.popBackStack() },
                onOpenRecovery = { navController.navigate(KryptRoutes.RECOVERY) }
            )
        }

        composable(KryptRoutes.PRIVACY_SETTINGS) {
            KryptPrivacySettingsScreen(
                viewModel = mainVm,
                currentUser = profile,
                onBack = { navController.popBackStack() }
            )
        }

        composable(KryptRoutes.BLOCKED_USERS) {
            KryptBlockedUsersScreen(viewModel = mainVm, onBack = { navController.popBackStack() })
        }

        composable(KryptRoutes.REPORTS) {
            KryptReportsScreen(viewModel = mainVm, onBack = { navController.popBackStack() })
        }

        composable(KryptRoutes.RECOVERY) {
            KryptAccountRecoveryScreen(onBack = { navController.popBackStack() })
        }

        // --- PROTECTED OWNER CONTROL CENTER ROUTES ---
        if (isOwnerOrAdmin && ownerVm != null) {
            composable(KryptRoutes.OWNER_DASHBOARD) {
                KryptOwnerDashboardScreen(
                    ownerVm = ownerVm,
                    onBack = { navController.popBackStack() },
                    onNavigateSection = { route -> navController.navigate(route) }
                )
            }
            composable(KryptRoutes.OWNER_USERS) {
                KryptOwnerUserManagementScreen(ownerVm = ownerVm, onBack = { navController.popBackStack() })
            }
            composable(KryptRoutes.OWNER_GROUPS) {
                KryptOwnerGroupManagementScreen(ownerVm = ownerVm, onBack = { navController.popBackStack() })
            }
            composable(KryptRoutes.OWNER_REVIEW) {
                KryptAuthorizedMessageReviewScreen(ownerVm = ownerVm, onBack = { navController.popBackStack() })
            }
            composable(KryptRoutes.OWNER_PASSES) {
                KryptAccessPassManagementScreen(ownerVm = ownerVm, onBack = { navController.popBackStack() })
            }
            composable(KryptRoutes.OWNER_MODERATION) {
                KryptModerationQueueScreen(ownerVm = ownerVm, onBack = { navController.popBackStack() })
            }
            composable(KryptRoutes.OWNER_AUDIT) {
                KryptAuditLogsScreen(ownerVm = ownerVm, onBack = { navController.popBackStack() })
            }
            composable(KryptRoutes.OWNER_PLATFORM) {
                KryptPlatformSettingsScreen(
                    ownerVm = ownerVm,
                    currentSettings = platformSettings,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun KryptAccountSuspendedGate(
    profile: UserProfile,
    mainVm: KryptMainViewModel,
    onSignOut: () -> Unit
) {
    val actions by mainVm.myModerationActions.collectAsStateWithLifecycle()
    var appealText by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.widthIn(max = 560.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            Icon(Icons.Filled.Gavel, "Account moderation", tint = KryptCrimson, modifier = Modifier.size(40.dp))
            KryptBadge(label = "ACCOUNT ${profile.accountStatus}", color = KryptCrimson)
            Text(
                text = "Access Restricted",
                style = MaterialTheme.typography.headlineLarge,
                color = KryptWhite
            )
            Text(
                text = "Your KRYPT membership (@${profile.username}) is currently ${profile.accountStatus.lowercase()}. Review the moderation notice below or submit an official appeal.",
                style = MaterialTheme.typography.bodyLarge,
                color = KryptSilver
            )

            actions.firstOrNull()?.let { latest ->
                KryptSurfaceCard(borderColor = KryptAmber) {
                    Text("MODERATION REASON", style = MaterialTheme.typography.labelSmall, color = KryptAmber)
                    Spacer(Modifier.height(6.dp))
                    Text(latest.reason, style = MaterialTheme.typography.bodyLarge, color = KryptWhite)
                }
            }

            if (profile.accountStatus == AccountStatus.SUSPENDED.name && !submitted) {
                KryptSurfaceCard {
                    Text("SUBMIT MODERATION APPEAL", style = MaterialTheme.typography.labelSmall, color = KryptSilver)
                    Spacer(Modifier.height(8.dp))
                    KryptTextField(
                        value = appealText,
                        onValueChange = { appealText = it },
                        label = "EXPLANATION",
                        placeholder = "Provide context for the Platform Owner",
                        singleLine = false,
                        maxLines = 4
                    )
                    Spacer(Modifier.height(12.dp))
                    KryptPrimaryButton(
                        text = "SUBMIT APPEAL",
                        enabled = appealText.isNotBlank(),
                        onClick = {
                            val reportId = actions.firstOrNull()?.reportId.orEmpty()
                            if (reportId.isNotBlank()) {
                                mainVm.submitAppeal(reportId, appealText)
                            } else {
                                mainVm.submitSafetyReport(
                                    targetType = "USER",
                                    targetId = profile.userId,
                                    targetUserId = profile.userId,
                                    reason = "Account Suspension Appeal",
                                    details = appealText
                                )
                            }
                            submitted = true
                        }
                    )
                }
            } else if (submitted) {
                KryptSurfaceCard {
                    Text("Appeal Logged", style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                    Text("The Platform Owner will review your submission in the Moderation Queue.", style = MaterialTheme.typography.bodyMedium, color = KryptSilver)
                }
            }

            KryptOutlinedButton(
                text = "SIGN OUT",
                accentColor = KryptCrimson,
                onClick = onSignOut
            )
        }
    }
}
