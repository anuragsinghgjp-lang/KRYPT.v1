package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.PlatformSettings
import com.example.ui.components.KryptBadge
import com.example.ui.components.KryptEmptyState
import com.example.ui.components.KryptOutlinedButton
import com.example.ui.components.KryptPrimaryButton
import com.example.ui.components.KryptSurfaceCard
import com.example.ui.components.KryptTextField
import com.example.ui.components.KryptTopBar
import com.example.ui.components.formatDateTime
import com.example.ui.theme.KryptAmber
import com.example.ui.theme.KryptAsh
import com.example.ui.theme.KryptBlack
import com.example.ui.theme.KryptBorder
import com.example.ui.theme.KryptCharcoal
import com.example.ui.theme.KryptCharcoalDeep
import com.example.ui.theme.KryptCrimson
import com.example.ui.theme.KryptEmerald
import com.example.ui.theme.KryptSilver
import com.example.ui.theme.KryptWhite
import com.example.ui.viewmodel.OwnerControlViewModel

@Composable
fun KryptOwnerDashboardScreen(
    ownerVm: OwnerControlViewModel,
    onBack: () -> Unit,
    onNavigateSection: (String) -> Unit
) {
    val stats by ownerVm.stats.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        KryptTopBar(
            title = "OWNER CONTROL CENTER",
            subtitle = "Governance & Platform Security",
            onBack = onBack,
            actions = {
                IconButton(onClick = { ownerVm.refreshStats() }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Refresh, "Refresh stats", tint = KryptWhite)
                }
            }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("OVERVIEW METRICS", style = MaterialTheme.typography.labelSmall, color = KryptAmber)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Total Members", stats.totalMembers.toString(), Modifier.weight(1f))
                StatTile("Active Members", stats.activeMembers.toString(), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Total Groups", stats.totalGroups.toString(), Modifier.weight(1f))
                StatTile("Active Passes", stats.activePasses.toString(), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Pending Reports", stats.pendingReports.toString(), Modifier.weight(1f))
                StatTile("Suspended", stats.suspendedAccounts.toString(), Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("Conversations", stats.totalConversations.toString(), Modifier.weight(1f))
                StatTile("Active Threads", stats.totalMessages.toString(), Modifier.weight(1f))
            }

            Spacer(Modifier.height(6.dp))
            Text("CONTROL MODULES", style = MaterialTheme.typography.labelSmall, color = KryptAsh)

            OwnerSectionTile(Icons.Filled.Key, "Access Passes", "Generate, limit, expire, and revoke entry passes") {
                onNavigateSection("owner_passes")
            }
            OwnerSectionTile(Icons.Filled.People, "All Users", "Warn, suspend, ban, restore, and manage administrators") {
                onNavigateSection("owner_users")
            }
            OwnerSectionTile(Icons.Filled.Groups, "All Groups", "Govern community groups without joining as member") {
                onNavigateSection("owner_groups")
            }
            OwnerSectionTile(Icons.Filled.Policy, "Authorized Message Review", "Audit-logged investigation of reported content") {
                onNavigateSection("owner_review")
            }
            OwnerSectionTile(Icons.Filled.Gavel, "Reports & Moderation Queue", "Review safety incidents & appeals") {
                onNavigateSection("owner_moderation")
            }
            OwnerSectionTile(Icons.Filled.History, "Audit Logs", "Immutable trail of administrative actions") {
                onNavigateSection("owner_audit")
            }
            OwnerSectionTile(Icons.Filled.Settings, "Platform Settings & Announcements", "Maintenance mode, registration & notices") {
                onNavigateSection("owner_platform")
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = KryptCharcoal,
        border = BorderStroke(1.dp, KryptBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium, color = KryptWhite)
            Text(label, style = MaterialTheme.typography.labelSmall, color = KryptSilver)
        }
    }
}

@Composable
private fun OwnerSectionTile(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = KryptCharcoal,
        border = BorderStroke(1.dp, KryptBorder),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, title, tint = KryptAmber, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = KryptSilver)
            }
        }
    }
}

@Composable
fun KryptOwnerUserManagementScreen(
    ownerVm: OwnerControlViewModel,
    onBack: () -> Unit
) {
    val users by ownerVm.allUsers.collectAsStateWithLifecycle()
    val admins by ownerVm.admins.collectAsStateWithLifecycle()
    val adminUids = remember(admins) { admins.map { it.userId }.toSet() }
    var search by remember { mutableStateOf("") }
    var actionReason by remember { mutableStateOf("Community safety enforcement") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        KryptTopBar(title = "USER MANAGEMENT", subtitle = "${users.size} registered members", onBack = onBack)
        Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            KryptTextField(value = search, onValueChange = { search = it }, label = "SEARCH USERS")
        }
        val filtered = users.filter {
            search.isBlank() || it.displayName.contains(search, ignoreCase = true) || it.username.contains(search, ignoreCase = true)
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered, key = { it.userId }) { user ->
                val isSafetyAdmin = user.userId in adminUids
                KryptSurfaceCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(user.displayName, style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                            Text("@${user.username}", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                        }
                        KryptBadge(
                            label = user.accountStatus,
                            color = if (user.accountStatus == "ACTIVE") KryptEmerald else KryptCrimson
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(onClick = { ownerVm.moderateUser(user.userId, "WARN", actionReason) }) {
                            Text("WARN", style = MaterialTheme.typography.labelSmall, color = KryptAmber)
                        }
                        TextButton(onClick = { ownerVm.moderateUser(user.userId, "SUSPEND", actionReason) }) {
                            Text("SUSPEND", style = MaterialTheme.typography.labelSmall, color = KryptCrimson)
                        }
                        TextButton(onClick = { ownerVm.moderateUser(user.userId, "BAN", actionReason) }) {
                            Text("BAN", style = MaterialTheme.typography.labelSmall, color = KryptCrimson)
                        }
                        TextButton(onClick = { ownerVm.moderateUser(user.userId, "RESTORE", "Account restored by Owner") }) {
                            Text("RESTORE", style = MaterialTheme.typography.labelSmall, color = KryptEmerald)
                        }
                    }
                    if (user.role != "OWNER") {
                        TextButton(onClick = {
                            ownerVm.toggleSafetyAdmin(user.userId, "${user.username}@krypt.member", !isSafetyAdmin, "Admin role update")
                        }) {
                            Text(
                                text = if (isSafetyAdmin) "REVOKE SAFETY ADMIN" else "ASSIGN SAFETY ADMIN",
                                style = MaterialTheme.typography.labelSmall,
                                color = KryptSilver
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KryptOwnerGroupManagementScreen(
    ownerVm: OwnerControlViewModel,
    onBack: () -> Unit
) {
    val groups by ownerVm.allGroups.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        KryptTopBar(title = "GROUP GOVERNANCE", subtitle = "${groups.size} community groups", onBack = onBack)
        if (groups.isEmpty()) {
            KryptEmptyState(title = "No Groups Created", subtitle = "Community groups will appear here for Owner oversight.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(groups, key = { it.id }) { grp ->
                    KryptSurfaceCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(grp.name, style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                            KryptBadge(
                                label = when {
                                    grp.suspended -> "SUSPENDED"
                                    grp.locked -> "LOCKED"
                                    else -> "ACTIVE"
                                },
                                color = if (grp.suspended) KryptCrimson else if (grp.locked) KryptAmber else KryptEmerald
                            )
                        }
                        Text("${grp.memberUids.size} members · Slow mode ${grp.slowModeSeconds}s", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                        if (grp.description.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(grp.description, style = MaterialTheme.typography.bodyMedium, color = KryptSilver)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TextButton(onClick = {
                                ownerVm.governGroup(grp.id, locked = !grp.locked, reason = "Owner toggled group lock")
                            }) {
                                Text(if (grp.locked) "UNLOCK" else "LOCK", color = KryptAmber)
                            }
                            TextButton(onClick = {
                                ownerVm.governGroup(grp.id, suspended = !grp.suspended, reason = "Owner toggled group suspension")
                            }) {
                                Text(if (grp.suspended) "RESTORE" else "SUSPEND", color = KryptCrimson)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KryptAuthorizedMessageReviewScreen(
    ownerVm: OwnerControlViewModel,
    onBack: () -> Unit
) {
    val groups by ownerVm.allGroups.collectAsStateWithLifecycle()
    val reviewedMessages by ownerVm.reviewedMessages.collectAsStateWithLifecycle()
    val reports by ownerVm.allReports.collectAsStateWithLifecycle()

    var selectedGroupId by remember { mutableStateOf("") }
    var auditReason by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        KryptTopBar(title = "AUTHORIZED REVIEW", subtitle = "Audit-Logged Safety Investigation", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                KryptSurfaceCard(borderColor = KryptAmber.copy(alpha = 0.5f)) {
                    Text("TRANSPARENCY & AUDIT POLICY", style = MaterialTheme.typography.labelSmall, color = KryptAmber)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Every safety review requires a mandatory reason recorded permanently in immutable Audit Logs. Unreported E2EE payloads remain encrypted; reported evidence snapshots and moderation contexts are inspected here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KryptSilver
                    )
                }
            }

            item {
                Text("REPORTED INCIDENT EVIDENCE", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
            }
            items(reports.filter { it.evidenceSnapshot.isNotBlank() }, key = { "ev_${it.id}" }) { rep ->
                KryptSurfaceCard {
                    KryptBadge(label = "${rep.targetType} · ${rep.status}", color = KryptAmber)
                    Spacer(Modifier.height(6.dp))
                    Text("Reason: ${rep.reason}", style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                    Text("Evidence Snapshot: ${rep.evidenceSnapshot}", style = MaterialTheme.typography.bodyMedium, color = KryptSilver)
                }
            }

            item {
                Spacer(Modifier.height(6.dp))
                Text("GROUP MODERATION CONTEXT INSPECTION", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                Spacer(Modifier.height(8.dp))
                KryptTextField(
                    value = auditReason,
                    onValueChange = { auditReason = it },
                    label = "MANDATORY AUDIT REASON (MIN 5 CHARS)",
                    placeholder = "Investigating report #..."
                )
                Spacer(Modifier.height(8.dp))
                groups.forEach { grp ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedGroupId = grp.id }
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(grp.name, color = if (selectedGroupId == grp.id) KryptAmber else KryptWhite)
                        TextButton(
                            enabled = auditReason.trim().length >= 5,
                            onClick = {
                                selectedGroupId = grp.id
                                ownerVm.inspectGroupMessages(grp.id, auditReason)
                            }
                        ) {
                            Text("INSPECT & LOG", color = KryptWhite)
                        }
                    }
                }
            }

            items(reviewedMessages, key = { it.id }) { msg ->
                KryptSurfaceCard {
                    Text("${msg.senderName} (${msg.senderId.take(8)})", style = MaterialTheme.typography.labelMedium, color = KryptSilver)
                    Text(
                        text = if (msg.deleted) "[Removed by Moderation]" else "Ciphertext (${msg.messageType}): ${msg.ciphertext.take(48)}...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = KryptWhite
                    )
                    if (!msg.deleted && selectedGroupId.isNotBlank()) {
                        TextButton(onClick = {
                            ownerVm.removeGroupMessage(selectedGroupId, msg.id, auditReason.ifBlank { "Harmful content removed" })
                        }) {
                            Text("REMOVE HARMFUL CONTENT", color = KryptCrimson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KryptAccessPassManagementScreen(
    ownerVm: OwnerControlViewModel,
    onBack: () -> Unit
) {
    val passes by ownerVm.accessPasses.collectAsStateWithLifecycle()
    val vault by ownerVm.passVault.collectAsStateWithLifecycle()
    val lastCode by ownerVm.lastGeneratedPass.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

    var label by remember { mutableStateOf("Founding Cohort") }
    var multiUse by remember { mutableStateOf(false) }
    var maxUsesText by remember { mutableStateOf("5") }
    var daysText by remember { mutableStateOf("14") }
    val codeMap = remember(vault) { vault.associateBy { it.passHash } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        KryptTopBar(title = "SPECIAL ACCESS PASSES", subtitle = "Hashed Server-Verified Invites", onBack = onBack)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                KryptSurfaceCard {
                    Text("ISSUE NEW SPECIAL PASS", style = MaterialTheme.typography.labelSmall, color = KryptAmber)
                    Spacer(Modifier.height(10.dp))
                    KryptTextField(value = label, onValueChange = { label = it }, label = "PASS LABEL")
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Multi-Use Pass", color = KryptWhite)
                        Switch(
                            checked = multiUse,
                            onCheckedChange = { multiUse = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = KryptBlack, checkedTrackColor = KryptWhite)
                        )
                    }
                    if (multiUse) {
                        Spacer(Modifier.height(8.dp))
                        KryptTextField(value = maxUsesText, onValueChange = { maxUsesText = it.filter(Char::isDigit) }, label = "MAX USES")
                    }
                    Spacer(Modifier.height(8.dp))
                    KryptTextField(value = daysText, onValueChange = { daysText = it.filter(Char::isDigit) }, label = "EXPIRES IN (DAYS)")
                    Spacer(Modifier.height(12.dp))
                    KryptPrimaryButton(
                        text = "GENERATE ACCESS PASS",
                        onClick = {
                            ownerVm.generateAccessPass(
                                label = label,
                                passType = if (multiUse) "MULTI_USE" else "SINGLE_USE",
                                maxUses = maxUsesText.toLongOrNull() ?: 1L,
                                validDays = daysText.toIntOrNull() ?: 14
                            )
                        },
                        modifier = Modifier.testTag("generate_pass_button")
                    )
                }
            }

            if (!lastCode.isNullOrBlank()) {
                item {
                    KryptSurfaceCard(borderColor = KryptEmerald) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("NEWLY GENERATED PASS", style = MaterialTheme.typography.labelSmall, color = KryptEmerald)
                                Text(lastCode!!, style = MaterialTheme.typography.titleLarge, color = KryptWhite)
                            }
                            IconButton(onClick = { clipboard.setText(AnnotatedString(lastCode!!)) }) {
                                Icon(Icons.Filled.ContentCopy, "Copy pass", tint = KryptWhite)
                            }
                        }
                    }
                }
            }

            item {
                Text("PASS LEDGER (${passes.size})", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
            }
            items(passes, key = { it.passHash }) { pass ->
                val rawCode = codeMap[pass.passHash]?.passCode
                KryptSurfaceCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(pass.label, style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                        KryptBadge(
                            label = if (pass.revoked) "REVOKED" else "${pass.usedCount}/${pass.maxUses} USED",
                            color = if (pass.revoked) KryptCrimson else KryptEmerald
                        )
                    }
                    if (!rawCode.isNullOrBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(rawCode, style = MaterialTheme.typography.labelMedium, color = KryptSilver)
                            IconButton(onClick = { clipboard.setText(AnnotatedString(rawCode)) }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.ContentCopy, "Copy", tint = KryptSilver, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    Text("Hash: ${pass.passHash.take(16)}...", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                    if (!pass.revoked) {
                        TextButton(onClick = { ownerVm.revokeAccessPass(pass.passHash, "Owner revoked") }) {
                            Text("REVOKE PASS", color = KryptCrimson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KryptModerationQueueScreen(
    ownerVm: OwnerControlViewModel,
    onBack: () -> Unit
) {
    val reports by ownerVm.allReports.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        KryptTopBar(title = "MODERATION QUEUE", subtitle = "${reports.count { it.status == "PENDING" || it.status == "APPEALED" }} open incidents", onBack = onBack)
        if (reports.isEmpty()) {
            KryptEmptyState(title = "Queue Empty", subtitle = "Community safety reports and appeals appear here.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reports, key = { it.id }) { rep ->
                    KryptSurfaceCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            KryptBadge(label = "${rep.targetType} · ${rep.status}", color = if (rep.status == "APPEALED") KryptAmber else KryptWhite)
                            Text(formatDateTime(rep.createdAt?.toDate()?.time ?: 0L), style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(rep.reason, style = MaterialTheme.typography.titleMedium, color = KryptWhite)
                        if (rep.evidenceSnapshot.isNotBlank()) {
                            Text("Evidence: ${rep.evidenceSnapshot}", style = MaterialTheme.typography.bodyMedium, color = KryptSilver)
                        }
                        if (rep.appealText.isNotBlank()) {
                            Text("Appeal: ${rep.appealText}", style = MaterialTheme.typography.bodyMedium, color = KryptAmber)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (rep.targetUserId.isNotBlank()) {
                                TextButton(onClick = { ownerVm.moderateUser(rep.targetUserId, "WARN", rep.reason, rep.id) }) {
                                    Text("WARN", style = MaterialTheme.typography.labelSmall, color = KryptAmber)
                                }
                                TextButton(onClick = { ownerVm.moderateUser(rep.targetUserId, "SUSPEND", rep.reason, rep.id) }) {
                                    Text("SUSPEND", style = MaterialTheme.typography.labelSmall, color = KryptCrimson)
                                }
                            }
                            TextButton(onClick = { ownerVm.resolveReport(rep.id, "RESOLVED", "Resolved by Owner") }) {
                                Text("RESOLVE", style = MaterialTheme.typography.labelSmall, color = KryptEmerald)
                            }
                            TextButton(onClick = { ownerVm.resolveReport(rep.id, "DISMISSED", "Dismissed") }) {
                                Text("DISMISS", style = MaterialTheme.typography.labelSmall, color = KryptSilver)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KryptAuditLogsScreen(
    ownerVm: OwnerControlViewModel,
    onBack: () -> Unit
) {
    val logs by ownerVm.auditLogs.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        KryptTopBar(title = "AUDIT LOGS", subtitle = "Immutable Administrative Ledger", onBack = onBack)
        if (logs.isEmpty()) {
            KryptEmptyState(title = "No Audit Entries Yet", subtitle = "All Owner & Safety Admin actions are recorded here immutably.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs, key = { it.id }) { log ->
                    KryptSurfaceCard {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            KryptBadge(label = log.action, color = KryptAmber)
                            Text(formatDateTime(log.createdAt?.toDate()?.time ?: 0L), style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Admin: ${log.adminEmail}", style = MaterialTheme.typography.labelMedium, color = KryptWhite)
                        Text("Target: ${log.targetType} (${log.targetId.take(16)})", style = MaterialTheme.typography.labelSmall, color = KryptSilver)
                        Text("Reason: ${log.reason}", style = MaterialTheme.typography.bodyMedium, color = KryptSilver)
                    }
                }
            }
        }
    }
}

@Composable
fun KryptPlatformSettingsScreen(
    ownerVm: OwnerControlViewModel,
    currentSettings: PlatformSettings,
    onBack: () -> Unit
) {
    var maintenance by remember(currentSettings) { mutableStateOf(currentSettings.maintenanceMode) }
    var regEnabled by remember(currentSettings) { mutableStateOf(currentSettings.registrationEnabled) }
    var requirePass by remember(currentSettings) { mutableStateOf(currentSettings.requirePassForSignup) }
    var annTitle by remember { mutableStateOf("") }
    var annBody by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(KryptBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        KryptTopBar(title = "PLATFORM GOVERNANCE", subtitle = "Global Controls & Notices", onBack = onBack)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            KryptSurfaceCard {
                Text("PLATFORM ACCESS CONTROLS", style = MaterialTheme.typography.labelSmall, color = KryptAmber)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Maintenance Mode", color = KryptWhite)
                    Switch(
                        checked = maintenance,
                        onCheckedChange = { maintenance = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = KryptBlack, checkedTrackColor = KryptCrimson)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Registration Enabled", color = KryptWhite)
                    Switch(
                        checked = regEnabled,
                        onCheckedChange = { regEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = KryptBlack, checkedTrackColor = KryptWhite)
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Require Special Access Pass", color = KryptWhite)
                    Switch(
                        checked = requirePass,
                        onCheckedChange = { requirePass = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = KryptBlack, checkedTrackColor = KryptWhite)
                    )
                }
                Spacer(Modifier.height(10.dp))
                KryptPrimaryButton(
                    text = "SAVE GOVERNANCE SETTINGS",
                    onClick = {
                        ownerVm.updatePlatformSettings(
                            currentSettings.copy(
                                maintenanceMode = maintenance,
                                registrationEnabled = regEnabled,
                                requirePassForSignup = requirePass
                            ),
                            reason = "Owner platform settings update"
                        )
                    }
                )
            }

            KryptSurfaceCard {
                Text("PUBLISH GLOBAL ANNOUNCEMENT", style = MaterialTheme.typography.labelSmall, color = KryptAsh)
                Spacer(Modifier.height(8.dp))
                KryptTextField(value = annTitle, onValueChange = { annTitle = it }, label = "ANNOUNCEMENT TITLE")
                Spacer(Modifier.height(8.dp))
                KryptTextField(value = annBody, onValueChange = { annBody = it }, label = "MESSAGE BODY", singleLine = false, maxLines = 4)
                Spacer(Modifier.height(12.dp))
                KryptOutlinedButton(
                    text = "BROADCAST ANNOUNCEMENT",
                    enabled = annTitle.isNotBlank() && annBody.isNotBlank(),
                    onClick = {
                        ownerVm.publishAnnouncement(annTitle, annBody, "NORMAL")
                        annTitle = ""
                        annBody = ""
                    }
                )
            }
        }
    }
}
