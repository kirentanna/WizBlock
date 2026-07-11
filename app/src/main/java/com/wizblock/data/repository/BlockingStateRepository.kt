package com.wizblock.data.repository

import com.wizblock.model.OnboardingStep
import com.wizblock.model.StrictModeState
import kotlinx.coroutines.flow.Flow

interface BlockingStateRepository {
    val blockingEnabled: Flow<Boolean>
    val onboardingStep: Flow<OnboardingStep>
    val serviceHeartbeatMs: Flow<Long>
    val lastPolicyEvaluationMs: Flow<Long>
    val quickBlockUntilMs: Flow<Long>
    val lastBlockedTarget: Flow<String>
    val blockNewlyInstalledApps: Flow<Boolean>
    val blockUnsupportedBrowsers: Flow<Boolean>
    val enabledCategoryIds: Flow<Set<String>>
    val starterBlocklistsSeeded: Flow<Boolean>
    val strictModeState: Flow<StrictModeState>
    val homeDurationPreset1Ms: Flow<Long>
    val homeDurationPreset2Ms: Flow<Long>
    val homeDurationPreset3Ms: Flow<Long>
    val lastCustomDurationMs: Flow<Long>

    suspend fun setBlockingEnabled(enabled: Boolean)
    suspend fun setOnboardingStep(step: OnboardingStep)
    suspend fun updatePermissionCache(overlayGranted: Boolean, accessibilityGranted: Boolean)
    suspend fun setLastBlockedTarget(target: String)
    suspend fun setServiceHeartbeat(timestamp: Long)
    suspend fun setLastPolicyEvaluation(timestamp: Long)
    suspend fun setQuickBlockUntil(timestamp: Long)
    suspend fun setBlockNewlyInstalledApps(enabled: Boolean)
    suspend fun setBlockUnsupportedBrowsers(enabled: Boolean)
    suspend fun setCategoryEnabled(categoryId: String, enabled: Boolean)
    suspend fun setStarterBlocklistsSeeded(seeded: Boolean)
    suspend fun activateStrictModeTimer(durationMs: Long, blockDeviceSettings: Boolean)
    suspend fun clearLegacyStrictModeCooldown()
    suspend fun setStrictModeBlockDeviceSettings(enabled: Boolean)
    suspend fun setStrictModeUninstallProtectionEnabled(enabled: Boolean)
    suspend fun setHomeDurationPreset(index: Int, durationMs: Long)
    suspend fun setLastCustomDuration(durationMs: Long)
    suspend fun deactivateStrictMode()
}
