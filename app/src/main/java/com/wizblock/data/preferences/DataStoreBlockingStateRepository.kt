package com.wizblock.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.wizblock.data.repository.BlockingStateRepository
import com.wizblock.model.OnboardingStep
import com.wizblock.model.StrictModeMode
import com.wizblock.model.StrictModeState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreBlockingStateRepository(
    context: Context
) : BlockingStateRepository {

    private val store = context.appDataStore

    override val blockingEnabled: Flow<Boolean> = store.data.map { prefs ->
        prefs[Keys.BLOCKING_ENABLED] ?: false
    }

    override val onboardingStep: Flow<OnboardingStep> = store.data.map { prefs ->
        val raw = prefs[Keys.ONBOARDING_STEP] ?: OnboardingStep.Accessibility.name
        runCatching { OnboardingStep.valueOf(raw) }.getOrDefault(OnboardingStep.Accessibility)
    }

    override val serviceHeartbeatMs: Flow<Long> = store.data.map { prefs ->
        prefs[Keys.SERVICE_HEARTBEAT] ?: 0L
    }

    override val lastPolicyEvaluationMs: Flow<Long> = store.data.map { prefs ->
        prefs[Keys.LAST_POLICY_EVAL] ?: 0L
    }

    override val quickBlockUntilMs: Flow<Long> = store.data.map { prefs ->
        prefs[Keys.QUICK_BLOCK_UNTIL] ?: 0L
    }

    override val lastBlockedTarget: Flow<String> = store.data.map { prefs ->
        prefs[Keys.LAST_BLOCKED_TARGET].orEmpty()
    }

    override val blockNewlyInstalledApps: Flow<Boolean> = store.data.map { prefs ->
        prefs[Keys.BLOCK_NEWLY_INSTALLED_APPS] ?: false
    }

    override val blockUnsupportedBrowsers: Flow<Boolean> = store.data.map { prefs ->
        prefs[Keys.BLOCK_UNSUPPORTED_BROWSERS] ?: false
    }

    override val enabledCategoryIds: Flow<Set<String>> = store.data.map { prefs ->
        prefs[Keys.ENABLED_CATEGORY_IDS].orEmpty()
    }

    override val starterBlocklistsSeeded: Flow<Boolean> = store.data.map { prefs ->
        prefs[Keys.STARTER_BLOCKLISTS_SEEDED] ?: false
    }

    override val strictModeState: Flow<StrictModeState> = store.data.map { prefs ->
        val active = prefs[Keys.STRICT_MODE_ACTIVE] ?: false
        val blockDeviceSettings = prefs[Keys.STRICT_MODE_BLOCK_DEVICE_SETTINGS] ?: true
        val uninstallProtectionEnabled = prefs[Keys.STRICT_MODE_UNINSTALL_PROTECTION_ENABLED] ?: false
        val mode = run {
            val raw = prefs[Keys.STRICT_MODE_MODE] ?: StrictModeMode.TIMER.name
            runCatching { StrictModeMode.valueOf(raw) }.getOrDefault(StrictModeMode.TIMER)
        }
        if (mode == StrictModeMode.COOLDOWN) {
            return@map StrictModeState.Inactive.copy(
                blockDeviceSettings = blockDeviceSettings,
                uninstallProtectionEnabled = uninstallProtectionEnabled
            )
        }
        StrictModeState(
            active = active,
            mode = mode,
            timerExpiresAtMs = prefs[Keys.STRICT_MODE_TIMER_EXPIRES_AT] ?: 0L,
            cooldownDurationMs = prefs[Keys.STRICT_MODE_COOLDOWN_DURATION] ?: 0L,
            cooldownStartedAtMs = prefs[Keys.STRICT_MODE_COOLDOWN_STARTED_AT] ?: 0L,
            blockDeviceSettings = blockDeviceSettings,
            uninstallProtectionEnabled = uninstallProtectionEnabled
        )
    }

    override val homeDurationPreset1Ms: Flow<Long> = store.data.map { prefs ->
        prefs[Keys.HOME_DURATION_PRESET_1_MS] ?: DEFAULT_PRESET_1_MS
    }

    override val homeDurationPreset2Ms: Flow<Long> = store.data.map { prefs ->
        prefs[Keys.HOME_DURATION_PRESET_2_MS] ?: DEFAULT_PRESET_2_MS
    }

    override val homeDurationPreset3Ms: Flow<Long> = store.data.map { prefs ->
        prefs[Keys.HOME_DURATION_PRESET_3_MS] ?: DEFAULT_PRESET_3_MS
    }

    override val lastCustomDurationMs: Flow<Long> = store.data.map { prefs ->
        prefs[Keys.LAST_CUSTOM_DURATION_MS] ?: DEFAULT_LAST_CUSTOM_DURATION_MS
    }

    override suspend fun setBlockingEnabled(enabled: Boolean) {
        store.edit { prefs ->
            prefs[Keys.BLOCKING_ENABLED] = enabled
        }
    }

    override suspend fun setOnboardingStep(step: OnboardingStep) {
        store.edit { prefs ->
            prefs[Keys.ONBOARDING_STEP] = step.name
        }
    }

    override suspend fun updatePermissionCache(overlayGranted: Boolean, accessibilityGranted: Boolean) {
        store.edit { prefs ->
            prefs[Keys.OVERLAY_PERMISSION] = overlayGranted
            prefs[Keys.ACCESSIBILITY_PERMISSION] = accessibilityGranted
        }
    }

    override suspend fun setLastBlockedTarget(target: String) {
        store.edit { prefs ->
            prefs[Keys.LAST_BLOCKED_TARGET] = target
        }
    }

    override suspend fun setServiceHeartbeat(timestamp: Long) {
        store.edit { prefs ->
            prefs[Keys.SERVICE_HEARTBEAT] = timestamp
        }
    }

    override suspend fun setLastPolicyEvaluation(timestamp: Long) {
        store.edit { prefs ->
            prefs[Keys.LAST_POLICY_EVAL] = timestamp
        }
    }

    override suspend fun setQuickBlockUntil(timestamp: Long) {
        store.edit { prefs ->
            prefs[Keys.QUICK_BLOCK_UNTIL] = timestamp
        }
    }

    override suspend fun setBlockNewlyInstalledApps(enabled: Boolean) {
        store.edit { prefs ->
            prefs[Keys.BLOCK_NEWLY_INSTALLED_APPS] = enabled
        }
    }

    override suspend fun setBlockUnsupportedBrowsers(enabled: Boolean) {
        store.edit { prefs ->
            prefs[Keys.BLOCK_UNSUPPORTED_BROWSERS] = enabled
        }
    }

    override suspend fun setCategoryEnabled(categoryId: String, enabled: Boolean) {
        val safeCategoryId = categoryId.trim().lowercase()
        if (safeCategoryId.isBlank()) return
        store.edit { prefs ->
            val categories = prefs[Keys.ENABLED_CATEGORY_IDS].orEmpty()
            prefs[Keys.ENABLED_CATEGORY_IDS] = if (enabled) {
                categories + safeCategoryId
            } else {
                categories - safeCategoryId
            }
        }
    }

    override suspend fun setStarterBlocklistsSeeded(seeded: Boolean) {
        store.edit { prefs ->
            prefs[Keys.STARTER_BLOCKLISTS_SEEDED] = seeded
        }
    }

    override suspend fun activateStrictModeTimer(durationMs: Long, blockDeviceSettings: Boolean) {
        val now = System.currentTimeMillis()
        store.edit { prefs ->
            prefs[Keys.STRICT_MODE_ACTIVE] = true
            prefs[Keys.STRICT_MODE_MODE] = StrictModeMode.TIMER.name
            prefs[Keys.STRICT_MODE_TIMER_EXPIRES_AT] = now + durationMs.coerceAtLeast(1L)
            prefs[Keys.STRICT_MODE_COOLDOWN_DURATION] = 0L
            prefs[Keys.STRICT_MODE_COOLDOWN_STARTED_AT] = 0L
            prefs[Keys.STRICT_MODE_BLOCK_DEVICE_SETTINGS] = blockDeviceSettings
            prefs[Keys.BLOCKING_ENABLED] = true
        }
    }

    override suspend fun clearLegacyStrictModeCooldown() {
        store.edit { prefs ->
            if (prefs[Keys.STRICT_MODE_MODE] == StrictModeMode.COOLDOWN.name) {
                prefs[Keys.STRICT_MODE_ACTIVE] = false
                prefs[Keys.STRICT_MODE_MODE] = StrictModeMode.TIMER.name
                prefs[Keys.STRICT_MODE_TIMER_EXPIRES_AT] = 0L
                prefs[Keys.STRICT_MODE_COOLDOWN_DURATION] = 0L
                prefs[Keys.STRICT_MODE_COOLDOWN_STARTED_AT] = 0L
                prefs[Keys.BLOCKING_ENABLED] = false
            }
        }
    }

    override suspend fun setStrictModeBlockDeviceSettings(enabled: Boolean) {
        store.edit { prefs ->
            prefs[Keys.STRICT_MODE_BLOCK_DEVICE_SETTINGS] = enabled
        }
    }

    override suspend fun setStrictModeUninstallProtectionEnabled(enabled: Boolean) {
        store.edit { prefs ->
            prefs[Keys.STRICT_MODE_UNINSTALL_PROTECTION_ENABLED] = enabled
        }
    }

    override suspend fun setHomeDurationPreset(index: Int, durationMs: Long) {
        val key = when (index) {
            0 -> Keys.HOME_DURATION_PRESET_1_MS
            1 -> Keys.HOME_DURATION_PRESET_2_MS
            2 -> Keys.HOME_DURATION_PRESET_3_MS
            else -> return
        }
        store.edit { prefs ->
            prefs[key] = durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        }
    }

    override suspend fun setLastCustomDuration(durationMs: Long) {
        store.edit { prefs ->
            prefs[Keys.LAST_CUSTOM_DURATION_MS] = durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        }
    }

    override suspend fun deactivateStrictMode() {
        store.edit { prefs ->
            prefs[Keys.STRICT_MODE_ACTIVE] = false
            prefs[Keys.STRICT_MODE_MODE] = StrictModeMode.TIMER.name
            prefs[Keys.STRICT_MODE_TIMER_EXPIRES_AT] = 0L
            prefs[Keys.STRICT_MODE_COOLDOWN_DURATION] = 0L
            prefs[Keys.STRICT_MODE_COOLDOWN_STARTED_AT] = 0L
        }
    }

    object Keys {
        val BLOCKING_ENABLED = booleanPreferencesKey("blocking_enabled")
        val ONBOARDING_STEP = stringPreferencesKey("onboarding_step")
        val OVERLAY_PERMISSION = booleanPreferencesKey("overlay_permission_granted_cached")
        val ACCESSIBILITY_PERMISSION = booleanPreferencesKey("accessibility_granted_cached")
        val LAST_BLOCKED_TARGET = stringPreferencesKey("last_blocked_target")
        val SERVICE_HEARTBEAT = longPreferencesKey("service_heartbeat_ms")
        val LAST_POLICY_EVAL = longPreferencesKey("service_last_policy_eval_ms")
        val QUICK_BLOCK_UNTIL = longPreferencesKey("quick_block_until_ms")
        val BLOCK_NEWLY_INSTALLED_APPS = booleanPreferencesKey("block_newly_installed_apps")
        val BLOCK_UNSUPPORTED_BROWSERS = booleanPreferencesKey("block_unsupported_browsers")
        val ENABLED_CATEGORY_IDS = stringSetPreferencesKey("enabled_category_ids")
        val STARTER_BLOCKLISTS_SEEDED = booleanPreferencesKey("starter_blocklists_seeded")
        val STRICT_MODE_ACTIVE = booleanPreferencesKey("strict_mode_active")
        val STRICT_MODE_MODE = stringPreferencesKey("strict_mode_mode")
        val STRICT_MODE_TIMER_EXPIRES_AT = longPreferencesKey("strict_mode_timer_expires_at_ms")
        val STRICT_MODE_COOLDOWN_DURATION = longPreferencesKey("strict_mode_cooldown_duration_ms")
        val STRICT_MODE_COOLDOWN_STARTED_AT = longPreferencesKey("strict_mode_cooldown_started_at_ms")
        val STRICT_MODE_BLOCK_DEVICE_SETTINGS = booleanPreferencesKey("strict_mode_block_device_settings")
        val STRICT_MODE_UNINSTALL_PROTECTION_ENABLED = booleanPreferencesKey("strict_mode_uninstall_protection_enabled")
        val HOME_DURATION_PRESET_1_MS = longPreferencesKey("home_duration_preset_1_ms")
        val HOME_DURATION_PRESET_2_MS = longPreferencesKey("home_duration_preset_2_ms")
        val HOME_DURATION_PRESET_3_MS = longPreferencesKey("home_duration_preset_3_ms")
        val LAST_CUSTOM_DURATION_MS = longPreferencesKey("last_custom_duration_ms")
    }

    private companion object {
        const val DEFAULT_PRESET_1_MS = 25L * 60_000L
        const val DEFAULT_PRESET_2_MS = 60L * 60_000L
        const val DEFAULT_PRESET_3_MS = 3L * 60L * 60_000L
        const val DEFAULT_LAST_CUSTOM_DURATION_MS = 2L * 60L * 60_000L
        const val MIN_DURATION_MS = 5L * 60_000L
        const val MAX_DURATION_MS = 14L * 24L * 60L * 60_000L
    }
}
