package com.wizblock.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wizblock.R
import com.wizblock.model.RuleKind
import com.wizblock.ui.HomeDurationSessionPolicy
import com.wizblock.ui.HomeActions
import com.wizblock.ui.HomeHealthCardUi
import com.wizblock.ui.HomeHealthUiFormatter
import com.wizblock.ui.HomeProtectionVisualMode
import com.wizblock.ui.HomeProtectionVisualPolicy
import com.wizblock.ui.HomeProtectionUiFormatter
import com.wizblock.ui.HomeStrictModeControlAction
import com.wizblock.ui.HomeStrictModeControlDisplay
import com.wizblock.ui.HomeStrictModeControlPolicy
import com.wizblock.ui.HomeStrictModeControlState
import com.wizblock.ui.HomeUiState
import com.wizblock.ui.PermissionState
import kotlinx.coroutines.delay

private val ScreenBackground = Color(0xFFF4F6FB)
private val CardShape = RoundedCornerShape(10.dp)
private val AccentBlue = Color(0xFF2E90FF)
private val TextPrimary = Color(0xFF1A202C)
private val TextMuted = Color(0xFF667085)
private val RowSurface = Color(0xFFF8FAFF)
private val SuccessGreen = Color(0xFF16A34A)
private const val STRICT_ACTION_START = "start"
private const val STRICT_ACTION_UPGRADE = "upgrade"

@Composable
fun HomeScreen(
    state: HomeUiState,
    actions: HomeActions
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.wizblockicon),
                        contentDescription = null,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = "WizBlock",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                Text(
                    text = "Private, local app and website blocking.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
        }

        item {
            ProtectionActionCard(
                state = state,
                onToggleBlocking = actions.onToggleBlocking,
                onStartProtection = actions.onStartProtection,
                onSwitchRunningProtectionToStrictMode = actions.onSwitchRunningProtectionToStrictMode,
                onInvalidStrictModeNoLimit = actions.onInvalidStrictModeNoLimit,
                onSetHomeDurationPreset = actions.onSetHomeDurationPreset,
                onSetLastCustomDuration = actions.onSetLastCustomDuration,
                onOpenLock = actions.onOpenLock
            )
        }

        HomeHealthUiFormatter.card(state.permissionState)?.let { healthCard ->
            item {
                HealthCard(card = healthCard, onClick = actions.onOpenPermissions)
            }
        }

        item {
            TodaySummaryCard(state = state, onOpenHistory = actions.onOpenHistory)
        }

        item {
            BlockedItemsPreviewCard(
                state = state,
                onManage = { actions.onOpenBlocklistTab(RuleKind.DOMAIN) }
            )
        }

        item {
            PrivacyCard()
        }
    }
}

@Composable
private fun ProtectionActionCard(
    state: HomeUiState,
    onToggleBlocking: (Boolean) -> Unit,
    onStartProtection: (Long?, Boolean) -> Unit,
    onSwitchRunningProtectionToStrictMode: (Long?) -> Unit,
    onInvalidStrictModeNoLimit: () -> Unit,
    onSetHomeDurationPreset: (Int, Long) -> Unit,
    onSetLastCustomDuration: (Long) -> Unit,
    onOpenLock: () -> Unit
) {
    var showCustom by rememberSaveable { mutableStateOf(false) }
    var selectedDurationMs by rememberSaveable { mutableStateOf<Long?>(null) }
    var customDurationMs by rememberSaveable { mutableStateOf(state.lastCustomDurationMs) }
    var customMinutes by rememberSaveable { mutableStateOf(HomeDurationSessionPolicy.partsFromDuration(state.lastCustomDurationMs).minutes) }
    var customHours by rememberSaveable { mutableStateOf(HomeDurationSessionPolicy.partsFromDuration(state.lastCustomDurationMs).hours) }
    var customDays by rememberSaveable { mutableStateOf(HomeDurationSessionPolicy.partsFromDuration(state.lastCustomDurationMs).days) }
    var strictModeSelected by rememberSaveable { mutableStateOf(false) }
    var pendingStrictAction by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingStrictDurationMs by rememberSaveable { mutableStateOf<Long?>(null) }
    var showPresetEditor by rememberSaveable { mutableStateOf(false) }
    var editingPresetIndex by rememberSaveable { mutableStateOf(0) }
    var editingPresetMinutes by rememberSaveable { mutableStateOf(25) }
    var editingPresetHours by rememberSaveable { mutableStateOf(0) }
    var editingPresetDays by rememberSaveable { mutableStateOf(0) }
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(state.blockingEnabled, state.focusUntilMs, state.strictModeState.timerExpiresAtMs) {
        while (
            state.blockingEnabled &&
            (
                state.focusUntilMs > System.currentTimeMillis() ||
                    state.strictModeState.remainingMs(System.currentTimeMillis()) > 0L
                )
        ) {
            nowMs = System.currentTimeMillis()
            delay(1_000L)
        }
        nowMs = System.currentTimeMillis()
    }
    val protectionStatus = HomeProtectionUiFormatter.status(
        blockingEnabled = state.blockingEnabled,
        focusUntilMs = state.focusUntilMs,
        strictModeState = state.strictModeState,
        nowMs = nowMs
    )
    val protectionVisualMode = HomeProtectionVisualPolicy.resolve(
        blockingEnabled = state.blockingEnabled,
        strictModeState = state.strictModeState,
        nowMs = nowMs
    )
    val strictModeControlState = HomeStrictModeControlPolicy.resolve(
        strictModeActive = state.strictModeState.active,
        selectedDurationMs = selectedDurationMs,
        blockingEnabled = state.blockingEnabled,
        focusUntilMs = state.focusUntilMs,
        nowMs = nowMs,
        localStrictModeSelected = strictModeSelected
    )
    val effectiveStrictMode = strictModeControlState.strictSelected && !state.strictModeState.active
    val selectedDurationLabel = selectedDurationMs?.let(HomeDurationSessionPolicy::labelForDuration)
    val statusTitle = when {
        state.blockingEnabled -> protectionStatus.title
        selectedDurationMs != null -> "Ready to block"
        else -> "Start protection"
    }
    val statusSubtitle = when {
        state.blockingEnabled -> protectionStatus.subtitle
        selectedDurationMs == null -> "No-limit normal session"
        effectiveStrictMode -> "Strict session for $selectedDurationLabel"
        else -> "Normal session for $selectedDurationLabel"
    }
    val startOrStop = {
        if (state.blockingEnabled) {
            onToggleBlocking(false)
        } else if (effectiveStrictMode) {
            pendingStrictAction = STRICT_ACTION_START
            pendingStrictDurationMs = selectedDurationMs
        } else {
            onStartProtection(selectedDurationMs, false)
        }
    }

    LaunchedEffect(state.lastCustomDurationMs) {
        val normalized = HomeDurationSessionPolicy.normalizeDurationMs(state.lastCustomDurationMs)
        customDurationMs = normalized
        val parts = HomeDurationSessionPolicy.partsFromDuration(normalized)
        customMinutes = parts.minutes
        customHours = parts.hours
        customDays = parts.days
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        ProtectionPlayButton(
            mode = protectionVisualMode,
            onClick = startOrStop
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                statusTitle,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = statusSubtitle,
                color = TextMuted,
                style = MaterialTheme.typography.bodySmall
            )
        }

        SessionCard(
            state = strictModeControlState,
            blockingEnabled = state.blockingEnabled,
            selectedDurationMs = selectedDurationMs,
            presetDurationsMs = state.homeDurationPresetMs,
            customDurationMs = customDurationMs,
            showCustom = showCustom,
            customMinutes = customMinutes,
            customHours = customHours,
            customDays = customDays,
            onOpenLock = onOpenLock,
            onSelectNoLimit = {
                selectedDurationMs = null
                strictModeSelected = false
                showCustom = false
            },
            onSelectPreset = { durationMs ->
                selectedDurationMs = durationMs
                showCustom = false
            },
            onToggleCustom = {
                showCustom = !showCustom
                if (!showCustom) {
                    selectedDurationMs = customDurationMs
                }
            },
            onCustomMinutesChange = { customMinutes = it.coerceIn(0, 55) },
            onCustomHoursChange = { customHours = it.coerceIn(0, 23) },
            onCustomDaysChange = { customDays = it.coerceIn(0, HomeDurationSessionPolicy.MAX_CUSTOM_DAYS) },
            onUseCustom = {
                val durationMs = HomeDurationSessionPolicy.customDurationMs(customMinutes, customHours, customDays)
                customDurationMs = durationMs
                selectedDurationMs = durationMs
                showCustom = false
                onSetLastCustomDuration(durationMs)
            },
            onEditPresets = {
                editingPresetIndex = 0
                val parts = HomeDurationSessionPolicy.partsFromDuration(state.homeDurationPresetMs[0])
                editingPresetMinutes = parts.minutes
                editingPresetHours = parts.hours
                editingPresetDays = parts.days
                showPresetEditor = true
            },
            onSelectNormal = { strictModeSelected = false },
            onSelectStrict = {
                when (strictModeControlState.action) {
                    HomeStrictModeControlAction.SetLocalSelection -> strictModeSelected = true
                    HomeStrictModeControlAction.ShowDurationRequired -> onInvalidStrictModeNoLimit()
                    HomeStrictModeControlAction.UpgradeTimedFocus,
                    HomeStrictModeControlAction.UpgradeSelectedDuration -> {
                        pendingStrictAction = STRICT_ACTION_UPGRADE
                        pendingStrictDurationMs = selectedDurationMs
                    }
                    HomeStrictModeControlAction.None -> Unit
                }
            },
            onStartOrStop = startOrStop
        )

        if (pendingStrictAction != null) {
            StrictModeConfirmDialog(
                durationLabel = pendingStrictDurationMs?.let(HomeDurationSessionPolicy::labelForDuration),
                onDismiss = {
                    pendingStrictAction = null
                    pendingStrictDurationMs = null
                },
                onConfirm = {
                    when (pendingStrictAction) {
                        STRICT_ACTION_START -> onStartProtection(pendingStrictDurationMs, true)
                        STRICT_ACTION_UPGRADE -> onSwitchRunningProtectionToStrictMode(pendingStrictDurationMs)
                    }
                    pendingStrictAction = null
                    pendingStrictDurationMs = null
                }
            )
        }

        if (showPresetEditor) {
            PresetEditorDialog(
                presetDurationsMs = state.homeDurationPresetMs,
                selectedIndex = editingPresetIndex,
                minutes = editingPresetMinutes,
                hours = editingPresetHours,
                days = editingPresetDays,
                onSelectPreset = { index ->
                    editingPresetIndex = index
                    val parts = HomeDurationSessionPolicy.partsFromDuration(state.homeDurationPresetMs[index])
                    editingPresetMinutes = parts.minutes
                    editingPresetHours = parts.hours
                    editingPresetDays = parts.days
                },
                onMinutesChange = { editingPresetMinutes = it.coerceIn(0, 55) },
                onHoursChange = { editingPresetHours = it.coerceIn(0, 23) },
                onDaysChange = { editingPresetDays = it.coerceIn(0, HomeDurationSessionPolicy.MAX_CUSTOM_DAYS) },
                onDismiss = { showPresetEditor = false },
                onSave = {
                    val durationMs = HomeDurationSessionPolicy.customDurationMs(
                        editingPresetMinutes,
                        editingPresetHours,
                        editingPresetDays
                    )
                    onSetHomeDurationPreset(editingPresetIndex, durationMs)
                    if (selectedDurationMs == state.homeDurationPresetMs[editingPresetIndex]) {
                        selectedDurationMs = durationMs
                    }
                    showPresetEditor = false
                }
            )
        }
    }
}

@Composable
private fun SessionCard(
    state: HomeStrictModeControlState,
    blockingEnabled: Boolean,
    selectedDurationMs: Long?,
    presetDurationsMs: List<Long>,
    customDurationMs: Long,
    showCustom: Boolean,
    customMinutes: Int,
    customHours: Int,
    customDays: Int,
    onOpenLock: () -> Unit,
    onSelectNoLimit: () -> Unit,
    onSelectPreset: (Long) -> Unit,
    onToggleCustom: () -> Unit,
    onCustomMinutesChange: (Int) -> Unit,
    onCustomHoursChange: (Int) -> Unit,
    onCustomDaysChange: (Int) -> Unit,
    onUseCustom: () -> Unit,
    onEditPresets: () -> Unit,
    onSelectNormal: () -> Unit,
    onSelectStrict: () -> Unit,
    onStartOrStop: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = CardShape,
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE5EAF3))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text("SESSION", color = TextMuted, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelSmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Mode", color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ModeChoiceButton(label = "Normal", selected = !state.strictSelected, onClick = onSelectNormal)
                    ModeChoiceButton(label = "Strict", selected = state.strictSelected, onClick = onSelectStrict)
                }
            }

            SectionDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Duration", color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = onEditPresets) { Text("Edit presets") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FocusDurationButton(
                    label = "No limit",
                    selected = selectedDurationMs == null,
                    onClick = onSelectNoLimit,
                    modifier = Modifier.weight(1f)
                )
                presetDurationsMs.take(2).forEach { durationMs ->
                    FocusDurationButton(
                        label = HomeDurationSessionPolicy.labelForDuration(durationMs),
                        selected = selectedDurationMs == durationMs,
                        onClick = { onSelectPreset(durationMs) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FocusDurationButton(
                    label = HomeDurationSessionPolicy.labelForDuration(presetDurationsMs.getOrElse(2) { 3L * 60L * 60_000L }),
                    selected = selectedDurationMs == presetDurationsMs.getOrElse(2) { 3L * 60L * 60_000L },
                    onClick = { onSelectPreset(presetDurationsMs.getOrElse(2) { 3L * 60L * 60_000L }) },
                    modifier = Modifier.weight(1f)
                )
                FocusDurationButton(
                    label = "Custom ${HomeDurationSessionPolicy.labelForDuration(customDurationMs)}",
                    selected = showCustom || selectedDurationMs == customDurationMs,
                    onClick = onToggleCustom,
                    modifier = Modifier.weight(2f)
                )
            }

            if (showCustom) {
                CustomFocusSelector(
                    minutes = customMinutes,
                    hours = customHours,
                    days = customDays,
                    onMinutesChange = onCustomMinutesChange,
                    onHoursChange = onCustomHoursChange,
                    onDaysChange = onCustomDaysChange,
                    onUse = onUseCustom
                )
            }

            SectionDivider()

            StrictBehaviorRow(state = state, onOpenLock = onOpenLock)

            Button(
                onClick = onStartOrStop,
                modifier = Modifier.fillMaxWidth(),
                shape = CardShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.strictSelected) SuccessGreen else AccentBlue
                )
            ) {
                Text(sessionActionLabel(blockingEnabled, state.strictSelected, selectedDurationMs))
            }
        }
    }
}

private fun strictModeControlSubtitle(state: HomeStrictModeControlState): String {
    return when (state.display) {
        HomeStrictModeControlDisplay.Selectable -> "Strict sessions cannot be stopped until the timer ends"
        HomeStrictModeControlDisplay.DurationRequired -> "Choose a duration to use Strict Mode"
        HomeStrictModeControlDisplay.UpgradeTimedFocus -> "Make the current timed session harder to bypass"
        HomeStrictModeControlDisplay.UpgradeSelectedDuration -> "Lock the current session for the selected time"
        HomeStrictModeControlDisplay.Locked -> "Locked until the countdown ends"
    }
}

@Composable
private fun SectionDivider() {
    Surface(modifier = Modifier.fillMaxWidth().height(1.dp), color = Color(0xFFE5EAF3)) {}
}

@Composable
private fun StrictBehaviorRow(
    state: HomeStrictModeControlState,
    onOpenLock: () -> Unit
) {
    when (state.display) {
        HomeStrictModeControlDisplay.Locked -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFEAF8EF),
                shape = CardShape,
                border = BorderStroke(1.dp, Color(0xFFBFE8CC))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = SuccessGreen) {}
                    Text("Strict Mode locked", color = SuccessGreen, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        else -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Strict behavior", color = TextPrimary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                    Text(strictModeControlSubtitle(state), color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
                TextButton(onClick = onOpenLock) { Text("Rules") }
            }
        }
    }
}

private fun sessionActionLabel(
    blockingEnabled: Boolean,
    strictSelected: Boolean,
    selectedDurationMs: Long?
): String {
    if (blockingEnabled && strictSelected) return "Locked until timer ends"
    if (blockingEnabled) return "Stop protection"
    val durationLabel = selectedDurationMs?.let(HomeDurationSessionPolicy::labelForDuration)
    return when {
        strictSelected && durationLabel != null -> "Start Strict $durationLabel"
        durationLabel != null -> "Start $durationLabel protection"
        else -> "Start protection"
    }
}

@Composable
private fun ModeChoiceButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = if (selected) AccentBlue else Color.White,
        shape = CardShape,
        border = BorderStroke(1.dp, if (selected) AccentBlue else Color(0xFFD5DCE8))
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) Color.White else TextPrimary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun ProtectionPlayButton(
    mode: HomeProtectionVisualMode,
    onClick: () -> Unit
) {
    val active = mode != HomeProtectionVisualMode.Off
    val treatment = HomeProtectionVisualPolicy.treatmentFor(mode)
    val buttonDescription = when (mode) {
        HomeProtectionVisualMode.Off -> "Start protection"
        HomeProtectionVisualMode.NormalActive -> "Stop protection"
        HomeProtectionVisualMode.StrictActive -> "Strict Mode running"
    }
    val transition = rememberInfiniteTransition(label = "protectionPulse")
    val pulseScale by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shieldPulse"
    )
    val activeColor = if (active) SuccessGreen else AccentBlue
    val ringColor = when (mode) {
        HomeProtectionVisualMode.Off -> Color(0xFFD9E7FF)
        HomeProtectionVisualMode.NormalActive -> Color(0xFFBFE8CC)
        HomeProtectionVisualMode.StrictActive -> Color(0xFFBFE8CC)
    }
    Box(
        modifier = Modifier
            .size(126.dp)
            .semantics {
                contentDescription = buttonDescription
                role = Role.Button
            }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (active) {
                drawCircle(
                    color = activeColor.copy(alpha = 0.13f),
                    radius = size.minDimension * 0.44f * pulseScale
                )
            }
            drawArc(
                color = ringColor,
                startAngle = 130f,
                sweepAngle = 280f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Surface(
            modifier = Modifier.size(86.dp),
            shape = CircleShape,
            color = if (active) activeColor else AccentBlue
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (treatment.showWizBlockIcon) {
                    Image(
                        painter = painterResource(id = R.drawable.wizblockicon),
                        contentDescription = null,
                        modifier = Modifier.size(66.dp)
                    )
                }
                Canvas(modifier = Modifier.fillMaxSize()) {
                    when (mode) {
                        HomeProtectionVisualMode.Off -> {
                            val play = Path().apply {
                                moveTo(size.width * 0.42f, size.height * 0.33f)
                                lineTo(size.width * 0.42f, size.height * 0.67f)
                                lineTo(size.width * 0.70f, size.height * 0.50f)
                                close()
                            }
                            drawPath(play, Color.White)
                        }

                        HomeProtectionVisualMode.NormalActive -> {
                            drawCenteredLock(
                                centerXFraction = treatment.lockCenterXFraction,
                                centerYFraction = treatment.lockCenterYFraction,
                                visualCenterYFraction = treatment.lockVisualCenterYFraction
                            )
                        }

                        HomeProtectionVisualMode.StrictActive -> {
                            drawCenteredLock(
                                centerXFraction = treatment.lockCenterXFraction,
                                centerYFraction = treatment.lockCenterYFraction,
                                visualCenterYFraction = treatment.lockVisualCenterYFraction
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCenteredLock(
    centerXFraction: Float,
    centerYFraction: Float,
    visualCenterYFraction: Float
) {
    val badgeCenter = androidx.compose.ui.geometry.Offset(
        size.width * centerXFraction,
        size.height * centerYFraction
    )
    val lockVisualCenterY = size.height * visualCenterYFraction
    val lockTop = lockVisualCenterY - size.height * 0.1175f
    val lockBottom = lockVisualCenterY + size.height * 0.1175f
    val bodyHeight = size.height * 0.135f
    val bodyTop = lockBottom - bodyHeight
    drawCircle(Color.White, radius = size.width * 0.20f, center = badgeCenter)
    drawRoundRect(
        color = SuccessGreen,
        topLeft = androidx.compose.ui.geometry.Offset(
            badgeCenter.x - size.width * 0.085f,
            bodyTop
        ),
        size = androidx.compose.ui.geometry.Size(size.width * 0.17f, bodyHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx())
    )
    drawArc(
        color = SuccessGreen,
        startAngle = 200f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = androidx.compose.ui.geometry.Offset(
            badgeCenter.x - size.width * 0.065f,
            lockTop
        ),
        size = androidx.compose.ui.geometry.Size(size.width * 0.13f, size.height * 0.17f),
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
    )
}

@Composable
private fun FocusDurationButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = if (selected) AccentBlue else Color.White,
        shape = CardShape,
        border = BorderStroke(1.dp, if (selected) AccentBlue else Color(0xFFD5DCE8))
    ) {
        Box(modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) Color.White else TextPrimary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun CustomFocusSelector(
    minutes: Int,
    hours: Int,
    days: Int,
    onMinutesChange: (Int) -> Unit,
    onHoursChange: (Int) -> Unit,
    onDaysChange: (Int) -> Unit,
    onUse: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), color = RowSurface, shape = CardShape) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Custom focus time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            DurationStepper("Minutes", minutes, onMinus = { onMinutesChange(minutes - 5) }, onPlus = { onMinutesChange(minutes + 5) })
            DurationStepper("Hours", hours, onMinus = { onHoursChange(hours - 1) }, onPlus = { onHoursChange(hours + 1) })
            DurationStepper("Days", days, onMinus = { onDaysChange(days - 1) }, onPlus = { onDaysChange(days + 1) })
            Button(onClick = onUse, modifier = Modifier.fillMaxWidth(), shape = CardShape) {
                Text("Use custom time")
            }
        }
    }
}

@Composable
private fun DurationStepper(
    label: String,
    value: Int,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StepButton("-", onMinus)
            Text(value.toString(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            StepButton("+", onPlus)
        }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(38.dp)
            .semantics { role = Role.Button }
            .clickable(onClick = onClick),
        color = Color.White,
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PresetEditorDialog(
    presetDurationsMs: List<Long>,
    selectedIndex: Int,
    minutes: Int,
    hours: Int,
    days: Int,
    onSelectPreset: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onHoursChange: (Int) -> Unit,
    onDaysChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit quick durations") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    repeat(HomeDurationSessionPolicy.PRESET_COUNT) { index ->
                        FocusDurationButton(
                            label = "Preset ${index + 1}",
                            selected = selectedIndex == index,
                            onClick = { onSelectPreset(index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Text(
                    text = "Current: ${HomeDurationSessionPolicy.labelForDuration(presetDurationsMs[selectedIndex])}",
                    color = TextMuted,
                    style = MaterialTheme.typography.bodySmall
                )
                DurationStepper("Minutes", minutes, onMinus = { onMinutesChange(minutes - 5) }, onPlus = { onMinutesChange(minutes + 5) })
                DurationStepper("Hours", hours, onMinus = { onHoursChange(hours - 1) }, onPlus = { onHoursChange(hours + 1) })
                DurationStepper("Days", days, onMinus = { onDaysChange(days - 1) }, onPlus = { onDaysChange(days + 1) })
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun StrictModeConfirmDialog(
    durationLabel: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start Strict Mode?") },
        text = {
            Text(
                text = if (durationLabel == null) {
                    "Strict Mode cannot be stopped until the current timer ends."
                } else {
                    "Strict Mode cannot be stopped until the $durationLabel timer ends."
                },
                color = TextMuted
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Lock timer") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun HealthCard(card: HomeHealthCardUi, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.White,
        shape = CardShape,
        border = BorderStroke(1.dp, Color(0xFFE5EAF3))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(12.dp),
                shape = CircleShape,
                color = if (card.healthy) Color(0xFF16A34A) else Color(0xFFDC2626)
            ) {}
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(card.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(card.subtitle, style = MaterialTheme.typography.bodySmall, color = TextMuted)
            }
            Text(">", color = TextMuted, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TodaySummaryCard(state: HomeUiState, onOpenHistory: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenHistory),
        color = Color.White,
        shape = CardShape
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Today", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("History", color = AccentBlue, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
            CompactMetricRow(
                values = listOf(
                    "Blocks" to state.dailySummary.total.toString(),
                    "Apps" to state.dailySummary.appBlocks.toString(),
                    "Sites" to state.dailySummary.domainBlocks.toString()
                )
            )
        }
    }
}

@Composable
private fun CompactMetricRow(values: List<Pair<String, String>>) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        values.forEachIndexed { index, (label, value) ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall)
            }
            if (index < values.lastIndex) {
                Surface(
                    modifier = Modifier
                        .size(width = 1.dp, height = 42.dp),
                    color = Color(0xFFE5EAF3)
                ) {}
            }
        }
    }
}

@Composable
private fun BlockedItemsPreviewCard(
    state: HomeUiState,
    onManage: () -> Unit
) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = CardShape, color = Color.White) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Blocked items", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(
                    "Manage",
                    modifier = Modifier.clickable(onClick = onManage),
                    color = AccentBlue,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                MetricPill("Apps", state.appsBlockedCount.toString(), Modifier.weight(1f))
                MetricPill("Websites", state.websitesBlockedCount.toString(), Modifier.weight(1f))
                MetricPill("Keywords", state.keywordsBlockedCount.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PrivacyCard() {
    Surface(modifier = Modifier.fillMaxWidth(), shape = CardShape, color = Color.White) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Privacy proof", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Text("No account. No internet permission. Rules stay on this device.", color = TextMuted, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun MetricPill(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = RowSurface, shape = CardShape) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(title, color = TextMuted, style = MaterialTheme.typography.labelLarge)
        }
    }
}
