package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.components.KryptAvatar
import com.example.ui.components.KryptBadge
import com.example.ui.components.KryptE2eeBadge
import com.example.ui.components.KryptOutlinedButton
import com.example.ui.components.KryptPrimaryButton
import com.example.ui.components.KryptSurfaceCard
import com.example.ui.components.KryptTextField
import com.example.ui.components.KryptTopBar
import com.example.ui.theme.KryptAmber
import com.example.ui.theme.KryptAsh
import com.example.ui.theme.KryptBlack
import com.example.ui.theme.KryptBorder
import com.example.ui.theme.KryptCharcoal
import com.example.ui.theme.KryptCrimson
import com.example.ui.theme.KryptEmerald
import com.example.ui.theme.KryptSilver
import com.example.ui.theme.KryptWhite
import com.example.ui.viewmodel.MediaAndVoiceHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (FirebaseAuth.getInstance().currentUser != null) {
        onAuthSuccess()
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build()
    scope.launch {
        try {
            val cred = credentialManager.getCredential(context, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                FirebaseAuth.getInstance()
                    .signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                    .await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
    scope.launch {
        try {
            val activity = context as? Activity
                ?: throw IllegalStateException("Activity context required for sign-in")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                FirebaseAuth.getInstance()
                    .signInWithCredential(GoogleAuthProvider.getCredential(token, null))
                    .await()
                onAuthSuccess()
            } else {
                onAuthError("Unexpected credential type returned")
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "Google Sign-In cancelled or dismissed: ${e.message}", e)
            onAuthCancelled()
        } catch (e: Exception) {
            Log.e("Auth", "Google Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Sign in failed")
        }
    }
}

@Composable
fun KryptSplashScreen(
    onResolvedUnauthenticated: () -> Unit,
    onResolvedAuthenticated: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    LaunchedEffect(Unit) {
        delay(650)
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onResolvedAuthenticated,
            onUnauthenticated = onResolvedUnauthenticated,
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .systemBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.img_krypt_icon),
                contentDescription = "KRYPT Monogram",
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, KryptBorder, RoundedCornerShape(20.dp))
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = "KRYPT",
                style = MaterialTheme.typography.displayLarge.copy(letterSpacing = 6.sp),
                color = KryptWhite
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "PRIVATE CRYPTOGRAPHIC COMMUNITY",
                style = MaterialTheme.typography.labelSmall,
                color = KryptAsh
            )
            Spacer(Modifier.height(32.dp))
            CircularProgressIndicator(
                color = KryptWhite,
                strokeWidth = 1.5.dp,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun KryptWelcomeScreen(
    onOpenLogin: () -> Unit,
    onOpenRegistration: () -> Unit,
    onOpenRecovery: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.Center)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KRYPT",
                        style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 4.sp),
                        color = KryptWhite
                    )
                    KryptBadge(label = "INVITE ONLY", color = KryptSilver)
                }
                Spacer(Modifier.height(24.dp))
                Image(
                    painter = painterResource(id = R.drawable.img_krypt_hero),
                    contentDescription = "KRYPT Cryptographic Vault",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, KryptBorder, RoundedCornerShape(14.dp))
                )
                Spacer(Modifier.height(28.dp))
                Text(
                    text = "Silence the noise.\nOwn your privacy.",
                    style = MaterialTheme.typography.displayLarge,
                    color = KryptWhite
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "An exclusive, Special Access Pass gated messaging and community platform with ECDH P-256 end-to-end encryption, Ghost Mode, and audited safety governance.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KryptSilver
                )
                Spacer(Modifier.height(20.dp))
                KryptE2eeBadge("ECDH P-256 · HKDF-SHA256 · AES-256-GCM")
            }

            Column(modifier = Modifier.padding(top = 32.dp)) {
                KryptPrimaryButton(
                    text = "SIGN IN WITH GOOGLE",
                    onClick = onOpenLogin,
                    modifier = Modifier.testTag("welcome_login_button")
                )
                Spacer(Modifier.height(12.dp))
                KryptOutlinedButton(
                    text = "REDEEM SPECIAL ACCESS PASS",
                    onClick = onOpenRegistration,
                    modifier = Modifier.testTag("welcome_register_button")
                )
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Account Recovery & Pass Verification Help",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KryptSilver,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onOpenRecovery)
                        .padding(vertical = 12.dp)
                        .testTag("welcome_recovery_link")
                )
            }
        }
    }
}

@Composable
fun KryptLoginScreen(
    isRegistrationFlow: Boolean,
    onBack: () -> Unit,
    onAuthSuccess: () -> Unit,
    onOpenRecovery: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }
    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .systemBarsPadding()
    ) {
        KryptTopBar(
            title = if (isRegistrationFlow) "CREATE ACCOUNT" else "MEMBER SIGN IN",
            subtitle = "Verified Google Identity Required",
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KryptSurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (isRegistrationFlow) Icons.Filled.Key else Icons.Filled.VerifiedUser,
                        contentDescription = "Identity verification",
                        tint = KryptWhite,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (isRegistrationFlow) "Step 1 · Bind Hardware-Backed Google Identity"
                        else "Authenticate Existing KRYPT Session",
                        style = MaterialTheme.typography.titleMedium,
                        color = KryptWhite
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = if (isRegistrationFlow)
                        "KRYPT enforces private, pass-gated onboarding. Authenticate with your Google account first, then enter your Special Access Pass in the next step."
                    else
                        "Sign in with your Google Identity to synchronize your encrypted conversations, groups, and device sessions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KryptSilver
                )
            }

            if (errorText != null) {
                KryptSurfaceCard(borderColor = KryptCrimson) {
                    Text(
                        text = errorText!!,
                        style = MaterialTheme.typography.bodyMedium,
                        color = KryptCrimson
                    )
                }
            }

            KryptPrimaryButton(
                text = if (isLoading) "AUTHENTICATING..." else "CONTINUE WITH GOOGLE",
                enabled = !isLoading,
                onClick = {
                    isLoading = true
                    errorText = null
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorText = msg
                        },
                        scope = scope,
                        onAuthCancelled = { isLoading = false }
                    )
                },
                modifier = Modifier.testTag("google_signin_button")
            )

            KryptOutlinedButton(
                text = "ACCOUNT RECOVERY & SECURITY",
                onClick = onOpenRecovery,
                modifier = Modifier.testTag("login_recovery_button")
            )

            HorizontalDivider(color = KryptBorder)
            Text(
                text = "Security Policy: No anonymous or unverified public registration is permitted. Pass codes are validated atomically via SHA-256 transaction.",
                style = MaterialTheme.typography.labelSmall,
                color = KryptAsh
            )
        }
    }
}

@Composable
fun KryptSpecialPassAndProfileSetupScreen(
    userEmail: String,
    isBootstrappedOwner: Boolean,
    isBusy: Boolean,
    statusMessage: String?,
    cooldownUntilEpochMs: Long,
    onRedeemPass: (code: String, username: String, displayName: String, bio: String) -> Unit,
    onBootstrapOwner: (username: String, displayName: String, bio: String) -> Unit,
    onSignOut: () -> Unit
) {
    var step by remember { mutableStateOf(if (isBootstrappedOwner) 2 else 1) }
    var passCode by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf(if (isBootstrappedOwner) "KRYPT Owner" else "") }
    var bio by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    val remainingCooldown = (cooldownUntilEpochMs - System.currentTimeMillis()).coerceAtLeast(0L) / 1000L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .systemBarsPadding()
    ) {
        KryptTopBar(
            title = if (step == 1) "SPECIAL ACCESS PASS" else "PROFILE SETUP",
            subtitle = userEmail.ifBlank { "Verified Identity" }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isBootstrappedOwner) {
                KryptSurfaceCard(borderColor = KryptEmerald.copy(alpha = 0.5f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shield, "Owner recognized", tint = KryptEmerald)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Platform Owner Identity Verified",
                            style = MaterialTheme.typography.titleMedium,
                            color = KryptWhite
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "You are authenticated as the primary KRYPT Owner ($userEmail). Complete your profile below to activate the Owner Control Center.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KryptSilver
                    )
                }
            }

            if (step == 1) {
                KryptSurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, "Access Pass", tint = KryptWhite)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = "Private Community Gate",
                            style = MaterialTheme.typography.titleMedium,
                            color = KryptWhite
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Enter the Special Access Pass issued by the KRYPT Owner. Passes are single-use or limited-use, time-bounded, and verified atomically.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KryptSilver
                    )
                }

                KryptTextField(
                    value = passCode,
                    onValueChange = { passCode = it.uppercase(); localError = null },
                    label = "SPECIAL ACCESS PASS",
                    placeholder = "KRYPT-XXXX-XXXX-XXXX",
                    isMonospace = true,
                    modifier = Modifier.testTag("special_pass_input")
                )

                if (remainingCooldown > 0) {
                    Text(
                        text = "Rate limit active. Retry in ${remainingCooldown}s.",
                        style = MaterialTheme.typography.labelMedium,
                        color = KryptAmber
                    )
                }

                KryptPrimaryButton(
                    text = "CONTINUE TO PROFILE SETUP",
                    enabled = passCode.trim().length >= 6 && remainingCooldown == 0L,
                    onClick = { step = 2 },
                    modifier = Modifier.testTag("verify_pass_continue_button")
                )
            } else {
                KryptSurfaceCard {
                    Text("Configure Your Member Identity", style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Your local ECDH P-256 encryption keypair will be generated and bound to your account upon activation.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KryptSilver
                    )
                }

                KryptTextField(
                    value = username,
                    onValueChange = { username = it.lowercase(); localError = null },
                    label = "UNIQUE USERNAME",
                    placeholder = "cipher_architect",
                    isMonospace = true,
                    modifier = Modifier.testTag("setup_username_input")
                )
                KryptTextField(
                    value = displayName,
                    onValueChange = { displayName = it; localError = null },
                    label = "DISPLAY NAME",
                    placeholder = "Alex Mercer",
                    modifier = Modifier.testTag("setup_display_name_input")
                )
                KryptTextField(
                    value = bio,
                    onValueChange = { bio = it },
                    label = "SHORT BIO (OPTIONAL)",
                    placeholder = "Security & Distributed Systems",
                    singleLine = false,
                    maxLines = 3,
                    modifier = Modifier.testTag("setup_bio_input")
                )

                val displayErr = localError ?: statusMessage
                if (!displayErr.isNullOrBlank()) {
                    KryptSurfaceCard(borderColor = KryptCrimson) {
                        Text(displayErr, style = MaterialTheme.typography.bodyMedium, color = KryptCrimson)
                    }
                }

                KryptPrimaryButton(
                    text = if (isBusy) "PROVISIONING VAULT..." else if (isBootstrappedOwner) "ACTIVATE OWNER ACCOUNT" else "REDEEM PASS & ENTER KRYPT",
                    enabled = !isBusy,
                    onClick = {
                        val cleanU = username.trim()
                        if (cleanU.length < 3) {
                            localError = "Username must be at least 3 characters."
                            return@KryptPrimaryButton
                        }
                        if (displayName.trim().isEmpty()) {
                            localError = "Display name is required."
                            return@KryptPrimaryButton
                        }
                        if (isBootstrappedOwner && passCode.isBlank()) {
                            onBootstrapOwner(cleanU, displayName.trim(), bio.trim())
                        } else {
                            onRedeemPass(passCode, cleanU, displayName.trim(), bio.trim())
                        }
                    },
                    modifier = Modifier.testTag("complete_registration_button")
                )

                if (!isBootstrappedOwner) {
                    KryptOutlinedButton(
                        text = "CHANGE ACCESS PASS",
                        onClick = { step = 1 }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            KryptOutlinedButton(
                text = "SIGN OUT / SWITCH GOOGLE ACCOUNT",
                onClick = onSignOut,
                modifier = Modifier.testTag("pass_gate_sign_out_button")
            )
        }
    }
}

@Composable
fun KryptAccountRecoveryScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .systemBarsPadding()
    ) {
        KryptTopBar(
            title = "ACCOUNT RECOVERY",
            subtitle = "Passwordless Identity & Key Continuity",
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 560.dp)
                .align(Alignment.CenterHorizontally)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            KryptSurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Security, "Recovery", tint = KryptWhite)
                    Spacer(Modifier.width(10.dp))
                    Text("Google Identity & Passkey Recovery", style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "KRYPT binds authentication to your verified Google account via Android Credential Manager. Password resets and 2-Step Verification recovery are secured directly through Google Account Recovery.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KryptSilver
                )
            }
            KryptSurfaceCard {
                Text("E2EE Key Rotation & Device Loss", style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "If you switch devices, a fresh ECDH P-256 keypair is generated in your new device's hardware Keystore and published to your key bundle. Previous messages encrypted solely to a wiped device's private key cannot be decrypted retroactively—preserving forward secrecy.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KryptSilver
                )
            }
            KryptSurfaceCard {
                Text("Suspended or Locked Accounts", style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "If your account received a moderation suspension, sign in with your Google Identity to view the moderation reason and submit an official appeal directly to the Platform Owner.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = KryptSilver
                )
            }
        }
    }
}

@Composable
fun KryptProfileEditDialogContent(
    currentUsername: String,
    currentDisplayName: String,
    currentBio: String,
    currentAvatarBase64: String,
    onSave: (username: String, displayName: String, bio: String, avatarBase64: String?) -> Unit
) {
    val context = LocalContext.current
    var username by remember(currentUsername) { mutableStateOf(currentUsername) }
    var displayName by remember(currentDisplayName) { mutableStateOf(currentDisplayName) }
    var bio by remember(currentBio) { mutableStateOf(currentBio) }
    var avatarBase64 by remember(currentAvatarBase64) { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let {
            avatarBase64 = MediaAndVoiceHelper.encodeImageUriToBase64(context, it, maxDim = 256, quality = 65)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }
                .padding(vertical = 4.dp)
        ) {
            KryptAvatar(
                displayName = displayName.ifBlank { "K" },
                avatarBase64 = avatarBase64 ?: currentAvatarBase64,
                size = 56.dp
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PhotoCamera, "Update avatar", tint = KryptWhite, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Change Profile Photo", style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                }
                Text("Uses zero-permission Android Photo Picker", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
            }
        }
        KryptTextField(value = username, onValueChange = { username = it }, label = "USERNAME", isMonospace = true)
        KryptTextField(value = displayName, onValueChange = { displayName = it }, label = "DISPLAY NAME")
        KryptTextField(value = bio, onValueChange = { bio = it }, label = "BIO", singleLine = false, maxLines = 3)
        KryptPrimaryButton(
            text = "SAVE PROFILE",
            onClick = { onSave(username, displayName, bio, avatarBase64) }
        )
    }
}
