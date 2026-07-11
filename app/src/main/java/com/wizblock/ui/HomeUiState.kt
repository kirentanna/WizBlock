package com.wizblock.ui

import com.wizblock.model.CategoryPack
import com.wizblock.model.DailyBlockSummary
import com.wizblock.model.Profile
import com.wizblock.model.RuleKind
import com.wizblock.model.ScheduleWindow
import com.wizblock.model.StrictModeState

data class TopBlockedDisplayItem(
    val title: String,
    val subtitle: String,
    val count: Int
)

data class RecentBlockDisplayItem(
    val id: Long,
    val title: String,
    val subtitle: String,
    val reason: String,
    val blockedAt: Long
)

data class HomeUiState(
    val blockingEnabled: Boolean,
    val focusUntilMs: Long,
    val permissionState: PermissionState,
    val appsBlockedCount: Int,
    val websitesBlockedCount: Int,
    val keywordsBlockedCount: Int,
    val blockNewlyInstalledApps: Boolean,
    val blockUnsupportedBrowsers: Boolean,
    val categoryPacks: List<CategoryPack>,
    val enabledCategoryIds: Set<String>,
    val profiles: List<Profile>,
    val strictModeState: StrictModeState,
    val homeDurationPresetMs: List<Long>,
    val lastCustomDurationMs: Long,
    val blockedTargets: List<BlockedTargetSettingsItem>,
    val schedules: List<ScheduleWindow>,
    val recentEvents: List<RecentBlockDisplayItem>,
    val dailySummary: DailyBlockSummary,
    val topBlocked: List<TopBlockedDisplayItem>
)

data class HomeActions(
    val onToggleBlocking: (Boolean) -> Unit,
    val onOpenBlocklistTab: (RuleKind) -> Unit,
    val onSetBlockNewlyInstalledApps: (Boolean) -> Unit,
    val onSetBlockUnsupportedBrowsers: (Boolean) -> Unit,
    val onSetCategoryEnabled: (String, Boolean) -> Unit,
    val onSetProfileEnabled: (String, Boolean) -> Unit,
    val onSetStrictModeBlockDeviceSettings: (Boolean) -> Unit,
    val onSetStrictModeUninstallProtection: (Boolean) -> Unit,
    val onStartProtection: (Long?, Boolean) -> Unit,
    val onSwitchRunningProtectionToStrictMode: (Long?) -> Unit,
    val onInvalidStrictModeNoLimit: () -> Unit,
    val onSetHomeDurationPreset: (Int, Long) -> Unit,
    val onSetLastCustomDuration: (Long) -> Unit,
    val onOpenLock: () -> Unit,
    val onOpenPermissions: () -> Unit,
    val onOpenHistory: () -> Unit
)
