package com.example.ui.components

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.GhostModeState
import com.example.ui.theme.KryptAmber
import com.example.ui.theme.KryptAmberSurface
import com.example.ui.theme.KryptAsh
import com.example.ui.theme.KryptBlack
import com.example.ui.theme.KryptBorder
import com.example.ui.theme.KryptBorderBright
import com.example.ui.theme.KryptCharcoal
import com.example.ui.theme.KryptCharcoalDeep
import com.example.ui.theme.KryptCharcoalElevated
import com.example.ui.theme.KryptEmerald
import com.example.ui.theme.KryptPlatinum
import com.example.ui.theme.KryptSilver
import com.example.ui.theme.KryptWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun KryptTopBar(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KryptBlack)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("top_bar_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = KryptWhite
                    )
                }
            } else {
                Spacer(Modifier.width(12.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = KryptWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = KryptSilver,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        }
        HorizontalDivider(color = KryptBorder, thickness = 1.dp)
    }
}

@Composable
fun GhostModeBanner(
    ghostMode: GhostModeState,
    onDisable: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val active = ghostMode.isEffectivelyActive()
    AnimatedVisibility(visible = active) {
        val remainingText = remember(ghostMode) {
            if (ghostMode.expiresAtEpochMs <= 0L) {
                "Manual"
            } else {
                val mins = ((ghostMode.expiresAtEpochMs - System.currentTimeMillis()) / 60_000L).coerceAtLeast(1L)
                if (mins >= 60L) "${mins / 60L}h ${mins % 60L}m left" else "${mins}m left"
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(KryptAmberSurface)
                .border(width = 0.5.dp, color = KryptAmber.copy(alpha = 0.35f))
                .clickable(onClick = onOpenSettings)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("ghost_mode_banner"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.VisibilityOff,
                contentDescription = "Ghost mode active",
                tint = KryptAmber,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "GHOST MODE ACTIVE · $remainingText",
                    style = MaterialTheme.typography.labelMedium,
                    color = KryptAmber
                )
                Text(
                    text = "Online status & read receipts suppressed",
                    style = MaterialTheme.typography.labelSmall,
                    color = KryptSilver
                )
            }
            Text(
                text = "EXIT",
                style = MaterialTheme.typography.labelMedium,
                color = KryptAmber,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .border(1.dp, KryptAmber.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                    .clickable(onClick = onDisable)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .testTag("ghost_mode_quick_exit")
            )
        }
    }
}

@Composable
fun KryptSurfaceCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = KryptBorder,
    containerColor: Color = KryptCharcoal,
    content: @Composable ColumnScope.() -> Unit
) {
    val mod = if (onClick != null) {
        modifier.clickable(onClick = onClick)
    } else modifier
    Surface(
        modifier = mod.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
fun KryptPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = KryptWhite,
            contentColor = KryptBlack,
            disabledContainerColor = KryptCharcoalElevated,
            disabledContentColor = KryptAsh
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun KryptOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accentColor: Color = KryptWhite
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, KryptBorderBright),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = KryptCharcoalDeep,
            contentColor = accentColor
        )
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun KryptTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
    isMonospace: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder, color = KryptAsh) }
        } else null,
        singleLine = singleLine,
        maxLines = maxLines,
        textStyle = if (isMonospace) MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp, color = KryptWhite)
        else MaterialTheme.typography.bodyLarge.copy(color = KryptWhite),
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = KryptWhite,
            unfocusedBorderColor = KryptBorder,
            focusedContainerColor = KryptCharcoal,
            unfocusedContainerColor = KryptCharcoal,
            focusedLabelColor = KryptPlatinum,
            unfocusedLabelColor = KryptSilver,
            cursorColor = KryptWhite
        ),
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
fun KryptAvatar(
    displayName: String,
    avatarBase64: String = "",
    size: Dp = 46.dp,
    showOnlineDot: Boolean = false,
    isOnline: Boolean = false
) {
    val bitmap = remember(avatarBase64) {
        if (avatarBase64.isNotBlank()) {
            runCatching {
                val bytes = Base64.decode(avatarBase64, Base64.NO_WRAP)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        } else null
    }
    val initials = remember(displayName) {
        displayName.trim().split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifBlank { "K" }
    }
    Box(modifier = Modifier.size(size)) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "$displayName avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .border(1.dp, KryptBorderBright, CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(KryptCharcoalElevated)
                    .border(1.dp, KryptBorderBright, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    style = MaterialTheme.typography.titleMedium,
                    color = KryptWhite
                )
            }
        }
        if (showOnlineDot && isOnline) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(KryptBlack)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(KryptEmerald)
            )
        }
    }
}

@Composable
fun KryptBadge(
    label: String,
    color: Color = KryptSilver,
    containerColor: Color = KryptCharcoalElevated
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = containerColor,
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.4f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun KryptE2eeBadge(label: String = "E2EE · ECDH P-256") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(KryptCharcoal)
            .border(0.5.dp, KryptBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Lock,
            contentDescription = "End-to-end encrypted",
            tint = KryptEmerald,
            modifier = Modifier.size(11.dp)
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = KryptSilver
        )
    }
}

@Composable
fun KryptEmptyState(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_empty_vault),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, KryptBorder, RoundedCornerShape(16.dp))
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = KryptWhite,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = KryptSilver,
            textAlign = TextAlign.Center
        )
    }
}

fun formatRelativeTime(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    return fmt.format(Date(epochMs))
}

fun formatDateTime(epochMs: Long): String {
    if (epochMs <= 0L) return ""
    val fmt = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
    return fmt.format(Date(epochMs))
}
