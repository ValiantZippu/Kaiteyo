package ua.syt0r.kanji.desktop.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ua.syt0r.kanji.desktop.appstate.AppState
import ua.syt0r.kanji.desktop.designsystem.DsChip
import ua.syt0r.kanji.desktop.designsystem.DsRadius
import ua.syt0r.kanji.desktop.designsystem.DsSpacing
import ua.syt0r.kanji.desktop.designsystem.DsType
import ua.syt0r.kanji.desktop.designsystem.accent
import ua.syt0r.kanji.desktop.designsystem.surfaceColors
import ua.syt0r.kanji.desktop.engine.account.AccountEngine
import ua.syt0r.kanji.desktop.engine.account.AuthFlowState
import ua.syt0r.kanji.desktop.model.ToastKind

// ============================================
// ACCOUNT — control center
// Adaptive layout: a section rail on desktop
// windows, a chip strip + single column on
// compact windows (tablet / phone). Every value
// is read live from the AccountEngine; the
// device-flow dialog is hosted at the root.
// ============================================

enum class AccountSection(val label: String, val icon: ImageVector) {
    Overview("Overview", Icons.Default.SpaceDashboard),
    Profile("Profile", Icons.Default.Person),
    Accounts("Connected accounts", Icons.Default.Link),
    Devices("Devices", Icons.Default.Devices),
    Sessions("Sessions", Icons.Default.History),
    Backups("Backups", Icons.Default.Backup),
    Sync("Sync", Icons.Default.Sync),
    Security("Security", Icons.Default.Security),
    Privacy("Privacy", Icons.Default.PrivacyTip),
    Notifications("Notifications", Icons.Default.Notifications),
    Developer("Developer", Icons.Default.BugReport)
}

@Composable
fun AccountView(state: AppState) {
    val engine = state.account
    var section by remember { mutableStateOf(AccountSection.Overview) }
    val authFlow by engine.authFlow.collectAsState()

    LaunchedEffect(Unit) { engine.refreshStorage() }

    BoxWithConstraints(Modifier.fillMaxSize().padding(DsSpacing.Lg)) {
        val desktop = maxWidth >= 860.dp
        if (desktop) {
            Row(
                Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
            ) {
                AccountRail(
                    selected = section,
                    onSelect = { section = it },
                    modifier = Modifier.width(240.dp).fillMaxHeight()
                )
                AccountContent(
                    state = state,
                    engine = engine,
                    section = section,
                    onOpenSection = { section = it },
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(DsSpacing.Xs)
                ) {
                    AccountSection.entries.forEach { s ->
                        DsChip(
                            text = s.label,
                            selected = s == section,
                            onClick = { section = s }
                        )
                    }
                }
                AccountContent(
                    state = state,
                    engine = engine,
                    section = section,
                    onOpenSection = { section = it },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Host the provider (OAuth device-flow) dialog.
    when (val flow = authFlow) {
        AuthFlowState.Idle -> Unit
        is AuthFlowState.Completed -> {
            LaunchedEffect(flow) {
                state.toastHost.show(
                    "Connected to ${flow.provider.displayName} as ${flow.accountName}",
                    kind = ToastKind.Success
                )
                engine.dismissAuthFlow()
            }
        }
        else -> ProviderConnectDialog(engine = engine, state = flow)
    }
}

@Composable
private fun AccountRail(
    selected: AccountSection,
    onSelect: (AccountSection) -> Unit,
    modifier: Modifier = Modifier
) {
    val sc = surfaceColors()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(DsSpacing.Md)) {
        Text(
            text = "Account",
            color = sc.textPrimary,
            fontSize = DsType.Title,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "Identity, security and cloud",
            color = sc.textMuted,
            fontSize = DsType.Caption
        )
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            AccountSection.entries.forEach { section ->
                AccountRailRow(
                    section = section,
                    selected = section == selected,
                    onClick = { onSelect(section) }
                )
            }
        }
    }
}

@Composable
private fun AccountRailRow(
    section: AccountSection,
    selected: Boolean,
    onClick: () -> Unit
) {
    val sc = surfaceColors()
    val ac = accent()
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DsRadius.Md))
            .background(
                when {
                    selected -> ac.primary.copy(alpha = 0.12f)
                    hovered -> sc.surfaceInteractive
                    else -> Color.Transparent
                }
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .hoverable(interaction)
            .padding(horizontal = DsSpacing.Md, vertical = DsSpacing.Sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(DsSpacing.Sm)
    ) {
        Icon(
            imageVector = section.icon,
            contentDescription = null,
            tint = if (selected) ac.primary else sc.textSecondary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = section.label,
            color = if (selected) sc.textPrimary else sc.textSecondary,
            fontSize = DsType.Body,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(ac.primary)
            )
        }
    }
}

@Composable
private fun AccountContent(
    state: AppState,
    engine: AccountEngine,
    section: AccountSection,
    onOpenSection: (AccountSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(DsSpacing.Lg)
    ) {
        when (section) {
            AccountSection.Overview -> AccountOverviewSection(state, engine, onOpenSection)
            AccountSection.Profile -> AccountProfileSection(state, engine)
            AccountSection.Accounts -> AccountProvidersSection(state, engine)
            AccountSection.Devices -> AccountDevicesSection(state, engine)
            AccountSection.Sessions -> AccountSessionsSection(state, engine)
            AccountSection.Backups -> AccountBackupsSection(state)
            AccountSection.Sync -> AccountSyncSection(state, engine)
            AccountSection.Security -> AccountSecuritySection(state, engine)
            AccountSection.Privacy -> AccountPrivacySection(state, engine)
            AccountSection.Notifications -> AccountNotificationsSection(state, engine)
            AccountSection.Developer -> AccountDeveloperSection(state, engine)
        }
        Spacer(Modifier.height(DsSpacing.Sm))
    }
}
