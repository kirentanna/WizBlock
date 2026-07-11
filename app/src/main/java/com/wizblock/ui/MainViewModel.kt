package com.wizblock.ui

import android.app.Application
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wizblock.ServiceLocator
import com.wizblock.data.repository.AddRuleResult
import com.wizblock.data.repository.BlocklistTargetDraft
import com.wizblock.data.repository.CreateBlocklistResult
import com.wizblock.domain.toTargetType
import com.wizblock.model.BlockEvent
import com.wizblock.model.DailyBlockSummary
import com.wizblock.model.Profile
import com.wizblock.model.MatchOperator
import com.wizblock.model.OnboardingStep
import com.wizblock.model.Rule
import com.wizblock.model.RuleAction
import com.wizblock.model.RuleKind
import com.wizblock.model.ScheduleWindow
import com.wizblock.model.StrictModeEditPolicy
import com.wizblock.model.StrictModeMode
import com.wizblock.model.StrictModeState
import com.wizblock.model.TargetType
import com.wizblock.model.TopBlockedTarget
import com.wizblock.model.UsageLimit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val ruleRepository = ServiceLocator.ruleRepository
    private val blockingStateRepository = ServiceLocator.blockingStateRepository
    private val scheduleRepository = ServiceLocator.scheduleRepository
    private val usageLimitRepository = ServiceLocator.usageLimitRepository
    private val profileRepository = ServiceLocator.profileRepository
    private val blocklistCreator = ServiceLocator.blocklistCreator
    private val blockEventRepository = ServiceLocator.blockEventRepository
    private val targetNormalizer = ServiceLocator.targetNormalizer
    private val targetDisplayResolver = ServiceLocator.targetDisplayResolver
    private val deviceAdminController = ServiceLocator.deviceAdminController
    private val zoneId = ZoneId.systemDefault()
    private val todayRange = currentDayRangeMillis()

    val blockingEnabled: StateFlow<Boolean> = blockingStateRepository.blockingEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val focusUntilMs: StateFlow<Long> = blockingStateRepository.quickBlockUntilMs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val homeDurationPresetMs: StateFlow<List<Long>> = combine(
        blockingStateRepository.homeDurationPreset1Ms,
        blockingStateRepository.homeDurationPreset2Ms,
        blockingStateRepository.homeDurationPreset3Ms
    ) { preset1, preset2, preset3 ->
        HomeDurationSessionPolicy.normalizePresetDurationsMs(listOf(preset1, preset2, preset3))
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeDurationSessionPolicy.defaultPresetDurationsMs()
    )

    val lastCustomDurationMs: StateFlow<Long> = blockingStateRepository.lastCustomDurationMs
        .map(HomeDurationSessionPolicy::normalizeDurationMs)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HomeDurationSessionPolicy.defaultLastCustomDurationMs()
        )

    val onboardingStep: StateFlow<OnboardingStep> = blockingStateRepository.onboardingStep
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OnboardingStep.Accessibility)

    val allRules: StateFlow<List<Rule>> = ruleRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val websiteRules: StateFlow<List<Rule>> = allRules
        .map { rules ->
            BlocklistUiModel.adHocBlockRules(rules).filter { it.kind == RuleKind.DOMAIN }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val keywordRules: StateFlow<List<Rule>> = allRules
        .map { rules ->
            BlocklistUiModel.adHocBlockRules(rules).filter { it.kind == RuleKind.KEYWORD }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val blockedAppPackages: StateFlow<Set<String>> = allRules
        .map { rules ->
            BlocklistUiModel.adHocBlockRules(rules)
                .filter { it.kind == RuleKind.APP_PACKAGE && it.enabled }
                .map { it.value }
                .toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val profiles: StateFlow<List<Profile>> = profileRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf(Profile.Default))

    private val activeBlockCounts: StateFlow<BlockRuleCounts> = combine(
        profiles,
        allRules
    ) { currentProfiles, rules ->
        BlocklistUiModel.activeBlockCounts(currentProfiles, rules)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BlockRuleCounts(0, 0, 0))

    val appsBlockedCount: StateFlow<Int> = activeBlockCounts
        .map { it.appCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val websitesBlockedCount: StateFlow<Int> = activeBlockCounts
        .map { it.websiteCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val keywordsBlockedCount: StateFlow<Int> = activeBlockCounts
        .map { it.keywordCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val blockNewlyInstalledApps: StateFlow<Boolean> = blockingStateRepository.blockNewlyInstalledApps
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val blockUnsupportedBrowsers: StateFlow<Boolean> = blockingStateRepository.blockUnsupportedBrowsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val categoryPacks = ServiceLocator.categoryMatcher.categoryPacks

    val enabledCategoryIds: StateFlow<Set<String>> = blockingStateRepository.enabledCategoryIds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    val blocklistSummaries: StateFlow<List<BlocklistSummaryItem>> = combine(
        profiles,
        allRules
    ) { currentProfiles, rules ->
        BlocklistUiModel.summarizeBlocklists(currentProfiles, rules)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val persistedStrictModeState: StateFlow<StrictModeState> = blockingStateRepository.strictModeState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StrictModeState.Inactive)

    private val _deviceAdminActive = MutableStateFlow(false)

    val strictModeState: StateFlow<StrictModeState> = combine(
        persistedStrictModeState,
        _deviceAdminActive
    ) { state, adminActive ->
        state.copy(uninstallProtectionEnabled = state.uninstallProtectionEnabled && adminActive)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StrictModeState.Inactive)

    val schedules: StateFlow<List<ScheduleWindow>> = scheduleRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val usageLimits: StateFlow<List<UsageLimit>> = usageLimitRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentEvents: StateFlow<List<BlockEvent>> = blockEventRepository.observeRecent()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentEventsDisplay: StateFlow<List<RecentBlockDisplayItem>> = recentEvents
        .map { events ->
            events.map { event ->
                val display = targetDisplayResolver.resolve(event.targetType, event.targetValue)
                RecentBlockDisplayItem(
                    id = event.id,
                    title = display.title,
                    subtitle = display.subtitle.ifBlank { targetTypeLabel(event.targetType) },
                    reason = blockReasonLabel(event.reason.name),
                    blockedAt = event.blockedAt
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val dailySummary: StateFlow<DailyBlockSummary> = blockEventRepository
        .observeDailySummary(todayRange.first, todayRange.second)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DailyBlockSummary(total = 0, appBlocks = 0, domainBlocks = 0, keywordBlocks = 0)
        )

    val topBlocked: StateFlow<List<TopBlockedTarget>> = blockEventRepository
        .observeTopBlocked(todayRange.first, todayRange.second, limit = 10)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val topBlockedDisplay: StateFlow<List<TopBlockedDisplayItem>> = topBlocked
        .map { targets ->
            targets.map { target ->
                val display = targetDisplayResolver.resolve(target.targetType, target.targetValue)
                TopBlockedDisplayItem(
                    title = display.title,
                    subtitle = display.subtitle,
                    count = target.count
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    val blockedTargets: StateFlow<List<BlockedTargetSettingsItem>> = combine(
        allRules,
        schedules,
        usageLimits,
        installedApps,
        profiles
    ) { rules, currentSchedules, currentLimits, apps, currentProfiles ->
        BlocklistUiModel.adHocBlockRules(rules)
            .distinctBy { it.kind to it.value.lowercase() }
            .map { rule ->
                val targetType = rule.kind.toTargetType()
                val schedule = currentSchedules.firstOrNull {
                    it.enabled &&
                        it.targetType == targetType &&
                        it.targetValue.equals(rule.value, ignoreCase = true)
                }
                val usageLimit = currentLimits.firstOrNull {
                    it.enabled &&
                        it.targetType == targetType &&
                        it.targetValue.equals(rule.value, ignoreCase = true)
                }
                val displayInfo = targetDisplayResolver.resolve(targetType, rule.value)
                BlockedTargetSettingsItem(
                    ruleId = rule.id,
                    profileId = rule.profileId,
                    enabled = rule.enabled,
                    blocklistNames = BlocklistUiModel.blocklistNamesForTarget(
                        targetKind = rule.kind,
                        targetValue = rule.value,
                        profiles = currentProfiles,
                        rules = rules
                    ),
                    targetType = targetType,
                    targetValue = displayInfo.targetKey.targetValue,
                    title = displayInfo.title,
                    subtitle = displayInfo.subtitle,
                    schedule = schedule,
                    usageLimit = usageLimit
                )
            }
            .sortedWith(compareBy<BlockedTargetSettingsItem>({ it.targetType.name }, { it.title.lowercase() }))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _permissionState = MutableStateFlow(PermissionState())
    val permissionState: StateFlow<PermissionState> = _permissionState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages = _messages.asSharedFlow()

    private var latestHeartbeat = 0L
    private var latestPolicyEvaluation = 0L

    init {
        loadInstalledApps()
        refreshDeviceAdminState()
        refreshPermissions()

        viewModelScope.launch {
            blockingStateRepository.clearLegacyStrictModeCooldown()
        }

        viewModelScope.launch {
            while (true) {
                updateServiceHealth()
                delay(3_000)
            }
        }

        viewModelScope.launch {
            blockingStateRepository.serviceHeartbeatMs.collectLatest {
                latestHeartbeat = it
                updateServiceHealth()
            }
        }

        viewModelScope.launch {
            blockingStateRepository.lastPolicyEvaluationMs.collectLatest {
                latestPolicyEvaluation = it
                updateServiceHealth()
            }
        }

        viewModelScope.launch {
            blockingStateRepository.quickBlockUntilMs.collectLatest { focusUntil ->
                if (focusUntil <= 0L) return@collectLatest
                val waitMs = focusUntil - System.currentTimeMillis()
                if (waitMs > 0L) {
                    delay(waitMs)
                }
                if (focusUntilMs.value == focusUntil) {
                    blockingStateRepository.setQuickBlockUntil(0L)
                    blockingStateRepository.setBlockingEnabled(false)
                    _messages.emit("Focus ended")
                }
            }
        }

        viewModelScope.launch {
            persistedStrictModeState.collectLatest { state ->
                if (!state.active || state.mode != StrictModeMode.TIMER || state.timerExpiresAtMs <= 0L) {
                    return@collectLatest
                }
                val waitMs = state.timerExpiresAtMs - System.currentTimeMillis()
                if (waitMs > 0L) {
                    delay(waitMs)
                }
                val current = persistedStrictModeState.value
                if (
                    current.active &&
                    current.mode == StrictModeMode.TIMER &&
                    current.timerExpiresAtMs == state.timerExpiresAtMs &&
                    current.remainingMs(System.currentTimeMillis()) <= 0L
                ) {
                    blockingStateRepository.deactivateStrictMode()
                    blockingStateRepository.setBlockingEnabled(false)
                    _messages.emit("Strict Mode ended")
                }
            }
        }
    }

    fun refreshPermissions() {
        val context = getApplication<Application>()
        val accessibilityGranted = PermissionUtils.isAccessibilityEnabled(context)
        val overlayGranted = PermissionUtils.canDrawOverlays(context)

        _permissionState.value = _permissionState.value.copy(
            accessibilityGranted = accessibilityGranted,
            overlayGranted = overlayGranted
        )

        viewModelScope.launch {
            blockingStateRepository.updatePermissionCache(overlayGranted, accessibilityGranted)

            val step = when {
                !accessibilityGranted -> OnboardingStep.Accessibility
                !overlayGranted -> OnboardingStep.Overlay
                else -> OnboardingStep.Complete
            }
            blockingStateRepository.setOnboardingStep(step)

            if (!accessibilityGranted || !overlayGranted) {
                blockingStateRepository.setBlockingEnabled(false)
            }
        }
    }

    fun setBlockingEnabled(enabled: Boolean) {
        if (!enabled && isStrictModeLocked()) {
            emitStrictModeMessage()
            return
        }
        if (enabled) {
            val permissions = permissionState.value
            if (!permissions.accessibilityGranted || !permissions.overlayGranted) {
                viewModelScope.launch {
                    _messages.emit("Enable Accessibility and Overlay permissions first")
                }
                return
            }
        }
        viewModelScope.launch {
            blockingStateRepository.setQuickBlockUntil(0L)
            blockingStateRepository.setBlockingEnabled(enabled)
        }
    }

    fun startProtection(durationMs: Long?, strictMode: Boolean) {
        viewModelScope.launch {
            when (val request = HomeStartProtectionPolicy.resolve(durationMs, strictMode)) {
                HomeStartProtectionRequest.InvalidStrictWithoutDuration -> {
                    _messages.emit(STRICT_MODE_DURATION_REQUIRED_MESSAGE)
                }

                HomeStartProtectionRequest.NormalNoLimit -> {
                    if (!hasProtectionPermissions()) return@launch
                    blockingStateRepository.setQuickBlockUntil(0L)
                    blockingStateRepository.setBlockingEnabled(true)
                    _messages.emit("Protection started")
                }

                is HomeStartProtectionRequest.TimedFocus -> {
                    if (!hasProtectionPermissions()) return@launch
                    blockingStateRepository.setBlockingEnabled(true)
                    blockingStateRepository.setQuickBlockUntil(System.currentTimeMillis() + request.durationMs)
                    _messages.emit("Focus started for ${formatFocusDuration(request.durationMs)}")
                }

                is HomeStartProtectionRequest.StrictTimer -> {
                    if (!hasProtectionPermissions()) return@launch
                    blockingStateRepository.setQuickBlockUntil(0L)
                    blockingStateRepository.activateStrictModeTimer(
                        durationMs = request.durationMs,
                        blockDeviceSettings = strictModeState.value.blockDeviceSettings
                    )
                    _messages.emit("Strict Mode locked for ${formatFocusDuration(request.durationMs)}")
                }
            }
        }
    }

    fun startFocus(durationMs: Long) {
        startProtection(durationMs = durationMs, strictMode = false)
    }

    fun upgradeTimedFocusToStrictMode() {
        switchRunningProtectionToStrictMode(selectedDurationMs = null)
    }

    fun switchRunningProtectionToStrictMode(selectedDurationMs: Long?) {
        viewModelScope.launch {
            if (!hasProtectionPermissions()) return@launch
            val now = System.currentTimeMillis()
            when (
                val request = HomeStrictModeUpgradePolicy.resolve(
                    blockingEnabled = blockingEnabled.value,
                    focusUntilMs = focusUntilMs.value,
                    selectedDurationMs = selectedDurationMs,
                    nowMs = now
                )
            ) {
                HomeStrictModeUpgradeRequest.InvalidNoTimedFocus -> {
                    _messages.emit(STRICT_MODE_DURATION_REQUIRED_MESSAGE)
                }

                is HomeStrictModeUpgradeRequest.StrictTimer -> {
                    blockingStateRepository.setQuickBlockUntil(0L)
                    blockingStateRepository.activateStrictModeTimer(
                        durationMs = request.durationMs,
                        blockDeviceSettings = strictModeState.value.blockDeviceSettings
                    )
                    _messages.emit("Strict Mode locked for ${formatFocusDuration(request.durationMs)}")
                }
            }
        }
    }

    fun showStrictModeDurationRequired() {
        viewModelScope.launch {
            _messages.emit(STRICT_MODE_DURATION_REQUIRED_MESSAGE)
        }
    }

    fun setHomeDurationPreset(index: Int, durationMs: Long) {
        viewModelScope.launch {
            blockingStateRepository.setHomeDurationPreset(
                index = index,
                durationMs = HomeDurationSessionPolicy.normalizeDurationMs(durationMs)
            )
        }
    }

    fun setLastCustomDuration(durationMs: Long) {
        viewModelScope.launch {
            blockingStateRepository.setLastCustomDuration(HomeDurationSessionPolicy.normalizeDurationMs(durationMs))
        }
    }

    fun setBlockNewlyInstalledApps(enabled: Boolean) {
        if (!enabled && isStrictModeLocked()) {
            emitStrictModeMessage()
            return
        }
        viewModelScope.launch { blockingStateRepository.setBlockNewlyInstalledApps(enabled) }
    }

    fun setBlockUnsupportedBrowsers(enabled: Boolean) {
        if (!enabled && isStrictModeLocked()) {
            emitStrictModeMessage()
            return
        }
        viewModelScope.launch { blockingStateRepository.setBlockUnsupportedBrowsers(enabled) }
    }

    fun setCategoryEnabled(categoryId: String, enabled: Boolean) {
        if (!enabled && isStrictModeLocked()) {
            emitStrictModeMessage()
            return
        }
        viewModelScope.launch { blockingStateRepository.setCategoryEnabled(categoryId, enabled) }
    }

    fun setProfileEnabled(profileId: String, enabled: Boolean) {
        if (!enabled && isStrictModeLocked()) {
            emitStrictModeMessage()
            return
        }
        viewModelScope.launch { profileRepository.setEnabled(profileId, enabled) }
    }

    fun createBlocklist(name: String, targets: List<BlocklistTargetDraft>) {
        viewModelScope.launch {
            when (blocklistCreator.create(name, targets)) {
                is CreateBlocklistResult.Success -> _messages.emit("Blocklist created")
                CreateBlocklistResult.InvalidName -> _messages.emit("Name the Blocklist first")
                CreateBlocklistResult.EmptySelection -> _messages.emit("Select items first")
            }
        }
    }

    fun updateBlocklist(profile: Profile, name: String, enabled: Boolean) {
        if (!enabled && isStrictModeLocked()) {
            emitStrictModeMessage()
            return
        }
        val safeName = name.trim()
        if (safeName.isBlank()) {
            viewModelScope.launch { _messages.emit("Name the Blocklist first") }
            return
        }
        viewModelScope.launch {
            profileRepository.upsert(profile.copy(name = safeName, enabled = enabled))
            _messages.emit("Blocklist saved")
        }
    }

    fun deleteBlocklist(profileId: String) {
        if (profileId == Profile.Default.id) {
            viewModelScope.launch { _messages.emit("Default Blocklist cannot be deleted") }
            return
        }
        if (isStrictModeLocked()) {
            emitStrictModeMessage()
            return
        }
        viewModelScope.launch {
            ruleRepository.deleteByProfileId(profileId)
            scheduleRepository.deleteByProfileId(profileId)
            usageLimitRepository.deleteByProfileId(profileId)
            profileRepository.delete(profileId)
            _messages.emit("Blocklist deleted")
        }
    }

    fun addRuleToBlocklist(profileId: String, kind: RuleKind, rawValue: String) {
        addRule(
            kind = kind,
            action = RuleAction.BLOCK,
            operator = defaultOperator(kind),
            value = rawValue,
            profileId = profileId,
            successMessage = "Added to Blocklist"
        )
    }

    fun deleteRule(id: String) {
        if (isStrictModeLocked()) {
            emitStrictModeMessage()
            return
        }
        viewModelScope.launch {
            ruleRepository.delete(id)
            _messages.emit("Removed")
        }
    }

    fun setStrictModeBlockDeviceSettings(enabled: Boolean) {
        if (!enabled && isStrictModeLocked()) {
            emitStrictModeMessage()
            return
        }
        viewModelScope.launch { blockingStateRepository.setStrictModeBlockDeviceSettings(enabled) }
    }

    fun createDeviceAdminEnableIntent(): Intent {
        return deviceAdminController.createEnableIntent()
    }

    fun refreshDeviceAdminState() {
        val active = deviceAdminController.isAdminActive()
        _deviceAdminActive.value = active
        if (!active && persistedStrictModeState.value.uninstallProtectionEnabled) {
            viewModelScope.launch {
                blockingStateRepository.setStrictModeUninstallProtectionEnabled(false)
            }
        }
    }

    fun handleDeviceAdminEnableResult() {
        val active = deviceAdminController.isAdminActive()
        _deviceAdminActive.value = active
        viewModelScope.launch {
            blockingStateRepository.setStrictModeUninstallProtectionEnabled(active)
            _messages.emit(
                if (active) {
                    "Uninstall protection enabled"
                } else {
                    "Uninstall protection was not enabled"
                }
            )
        }
    }

    fun disableStrictModeUninstallProtection() {
        if (!StrictModeEditPolicy.canDisableUninstallProtection(strictModeState.value.active)) {
            emitStrictModeMessage()
            return
        }
        viewModelScope.launch {
            val removed = deviceAdminController.removeActiveAdminIfAllowed()
            refreshDeviceAdminState()
            if (removed) {
                blockingStateRepository.setStrictModeUninstallProtectionEnabled(false)
                _messages.emit("Uninstall protection disabled")
            } else {
                _messages.emit("Could not disable uninstall protection")
            }
        }
    }

    fun activateStrictModeTimer(durationMs: Long) {
        viewModelScope.launch {
            blockingStateRepository.setQuickBlockUntil(0L)
            blockingStateRepository.activateStrictModeTimer(
                durationMs = durationMs,
                blockDeviceSettings = strictModeState.value.blockDeviceSettings
            )
            _messages.emit("Strict Mode timer started")
        }
    }

    fun deactivateStrictModeManually() {
        val state = strictModeState.value
        if (!state.canManuallyDisable(System.currentTimeMillis())) {
            emitStrictModeMessage()
            return
        }
        viewModelScope.launch {
            blockingStateRepository.deactivateStrictMode()
            _messages.emit("Strict Mode turned off")
        }
    }

    fun addWebsite(domain: String) {
        addRule(
            kind = RuleKind.DOMAIN,
            action = RuleAction.BLOCK,
            operator = MatchOperator.SUBDOMAIN,
            value = domain,
            profileId = Profile.Default.id,
            successMessage = "Website added"
        )
    }

    fun addKeyword(keyword: String) {
        addRule(
            kind = RuleKind.KEYWORD,
            action = RuleAction.BLOCK,
            operator = MatchOperator.CONTAINS,
            value = keyword,
            profileId = Profile.Default.id,
            successMessage = "Keyword added"
        )
    }

    fun toggleRule(id: String, enabled: Boolean) {
        if (!enabled && isStrictModeLocked()) {
            emitStrictModeMessage()
            return
        }
        viewModelScope.launch { ruleRepository.setEnabled(id, enabled) }
    }

    fun saveSchedule(targetType: TargetType, targetValue: String, startMinute: Int, endMinute: Int, daysMask: Int) {
        if (isStrictModeLocked() && hasExistingSchedule(targetType, targetValue)) {
            emitStrictModeMessage()
            return
        }
        viewModelScope.launch {
            val added = scheduleRepository.upsert(
                targetType = targetType,
                targetValue = targetValue,
                startMinute = startMinute,
                endMinute = endMinute,
                daysMask = daysMask
            )
            _messages.emit(if (added) "Schedule saved" else "Invalid schedule")
        }
    }

    fun deleteSchedule(id: String) {
        if (isStrictModeLocked()) {
            emitStrictModeMessage()
            return
        }
        viewModelScope.launch {
            scheduleRepository.delete(id)
            _messages.emit("Schedule deleted")
        }
    }

    fun saveUsageLimit(targetType: TargetType, targetValue: String, minutesPerDay: Int, opensPerDay: Int) {
        if (isStrictModeLocked() && !isUsageLimitChangeStrictEnough(targetType, targetValue, minutesPerDay, opensPerDay)) {
            emitStrictModeMessage()
            return
        }
        viewModelScope.launch {
            val added = usageLimitRepository.upsert(
                targetType = targetType,
                targetValue = targetValue,
                minutesPerDay = minutesPerDay,
                opensPerDay = opensPerDay
            )
            _messages.emit(if (added) "Usage limit saved" else "Invalid usage limit")
        }
    }

    fun deleteUsageLimit(id: String) {
        if (isStrictModeLocked()) {
            emitStrictModeMessage()
            return
        }
        viewModelScope.launch {
            usageLimitRepository.delete(id)
            _messages.emit("Usage limit deleted")
        }
    }

    fun toggleAppBlocked(packageName: String, blocked: Boolean) {
        if (!blocked && isStrictModeLocked()) {
            emitStrictModeMessage()
            return
        }
        viewModelScope.launch {
            val canonicalPackage = installedApps.value.firstOrNull {
                it.packageName.equals(packageName, ignoreCase = true)
            }?.packageName ?: packageName
            val normalized = targetNormalizer.normalize(TargetType.APP_PACKAGE, canonicalPackage)?.targetValue ?: return@launch
            val existing = allRules.value.firstOrNull {
                it.kind == RuleKind.APP_PACKAGE &&
                    it.action == RuleAction.BLOCK &&
                    it.value.equals(normalized, ignoreCase = true) &&
                    (it.profileId ?: Profile.Default.id) == Profile.Default.id
            }
            if (existing != null) {
                ruleRepository.setEnabled(existing.id, blocked)
                return@launch
            }
            if (blocked) {
                when (ruleRepository.add(RuleKind.APP_PACKAGE, RuleAction.BLOCK, MatchOperator.EXACT, normalized)) {
                    is AddRuleResult.Success -> _messages.emit("App added")
                    AddRuleResult.Duplicate -> _messages.emit("App already exists")
                    AddRuleResult.Invalid -> _messages.emit("Invalid app")
                }
            }
        }
    }

    private fun addRule(
        kind: RuleKind,
        action: RuleAction,
        operator: MatchOperator,
        value: String,
        profileId: String = Profile.Default.id,
        successMessage: String
    ) {
        viewModelScope.launch {
            when (ruleRepository.add(kind, action, operator, value, profileId)) {
                is AddRuleResult.Success -> _messages.emit(successMessage)
                AddRuleResult.Duplicate -> _messages.emit("Already exists")
                AddRuleResult.Invalid -> _messages.emit("Invalid value")
            }
        }
    }

    private fun defaultOperator(kind: RuleKind): MatchOperator {
        return when (kind) {
            RuleKind.APP_PACKAGE -> MatchOperator.EXACT
            RuleKind.DOMAIN -> MatchOperator.SUBDOMAIN
            RuleKind.KEYWORD -> MatchOperator.CONTAINS
        }
    }

    private fun isStrictModeLocked(): Boolean {
        return strictModeState.value.active
    }

    private fun emitStrictModeMessage() {
        viewModelScope.launch {
            _messages.emit("Strict Mode is active")
        }
    }

    private suspend fun hasProtectionPermissions(): Boolean {
        val permissions = permissionState.value
        if (permissions.accessibilityGranted && permissions.overlayGranted) {
            return true
        }
        _messages.emit("Enable Accessibility and Overlay permissions first")
        return false
    }

    private fun hasExistingSchedule(targetType: TargetType, targetValue: String): Boolean {
        val normalized = targetNormalizer.normalize(targetType, targetValue)?.targetValue ?: return false
        return schedules.value.any {
            it.enabled &&
                it.targetType == targetType &&
                it.targetValue.equals(normalized, ignoreCase = true)
        }
    }

    private fun isUsageLimitChangeStrictEnough(
        targetType: TargetType,
        targetValue: String,
        minutesPerDay: Int,
        opensPerDay: Int
    ): Boolean {
        val normalized = targetNormalizer.normalize(targetType, targetValue)?.targetValue ?: return false
        val existing = usageLimits.value.firstOrNull {
            it.enabled &&
                it.targetType == targetType &&
                it.targetValue.equals(normalized, ignoreCase = true)
        }
        return StrictModeEditPolicy.canChangeUsageLimit(
            strictModeActive = true,
            existing = existing,
            minutesPerDay = minutesPerDay,
            opensPerDay = opensPerDay
        )
    }

    private fun updateServiceHealth() {
        val now = System.currentTimeMillis()
        val health = when {
            latestHeartbeat <= 0L || now - latestHeartbeat > 15_000L -> ServiceHealth.OFFLINE
            latestPolicyEvaluation <= 0L || now - latestPolicyEvaluation > 15_000L -> ServiceHealth.DEGRADED
            else -> ServiceHealth.HEALTHY
        }
        _permissionState.value = _permissionState.value.copy(serviceHealth = health)
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            val pm = context.packageManager
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val launcherActivities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(
                    launcherIntent,
                    PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
                )
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(launcherIntent, PackageManager.MATCH_ALL)
            }
            val apps = launcherActivities
                .asSequence()
                .mapNotNull { resolveInfo ->
                    val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                    val appInfo = activityInfo.applicationInfo ?: return@mapNotNull null
                    if (activityInfo.packageName == context.packageName) return@mapNotNull null
                    if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0) return@mapNotNull null
                    InstalledAppItem(
                        packageName = activityInfo.packageName,
                        label = runCatching {
                            pm.getApplicationLabel(appInfo).toString()
                        }.getOrDefault(activityInfo.packageName)
                    )
                }
                .distinctBy { it.packageName.lowercase() }
                .sortedBy { it.label.lowercase() }
                .toList()
            Log.d(TAG, "Loaded ${apps.size} launchable apps for picker")
            _installedApps.value = apps
        }
    }

    private fun currentDayRangeMillis(): Pair<Long, Long> {
        val today = LocalDate.now(zoneId)
        val start = today.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return start to end
    }

    private companion object {
        const val TAG = "MainViewModel"
        const val STRICT_MODE_DURATION_REQUIRED_MESSAGE = "Choose a duration to use Strict Mode"
    }
}

private fun formatFocusDuration(durationMs: Long): String {
    val minutes = (durationMs / 60_000L).coerceAtLeast(1L)
    return if (minutes >= 60L) {
        val hours = minutes / 60L
        val remainder = minutes % 60L
        if (remainder == 0L) "${hours}h" else "${hours}h ${remainder}m"
    } else {
        "${minutes}m"
    }
}

private fun targetTypeLabel(targetType: TargetType): String {
    return when (targetType) {
        TargetType.APP_PACKAGE -> "App"
        TargetType.DOMAIN -> "Website"
        TargetType.KEYWORD -> "Keyword"
    }
}

private fun blockReasonLabel(reasonName: String): String {
    return reasonName
        .lowercase(Locale.US)
        .split("_")
        .joinToString(" ") { word -> word.replaceFirstChar { it.titlecase(Locale.US) } }
}
