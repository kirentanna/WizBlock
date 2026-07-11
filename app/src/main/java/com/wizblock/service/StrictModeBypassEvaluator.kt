package com.wizblock.service

import com.wizblock.model.BlockReason
import com.wizblock.model.StrictModeState
import com.wizblock.model.TargetType
import com.wizblock.policy.PolicyDecision

object StrictModeBypassEvaluator {
    fun evaluate(
        packageName: String,
        className: String?,
        visibleText: String,
        state: StrictModeState
    ): PolicyDecision.Blocked? {
        if (!state.active) return null

        val normalizedPackage = packageName.lowercase()
        val blocksPackage = STRICT_MODE_BLOCKED_PACKAGES.any { normalizedPackage == it } ||
            STRICT_MODE_BLOCKED_PACKAGE_PREFIXES.any { normalizedPackage.startsWith(it) }
        if (!blocksPackage) return null

        val blockAdminDisable = state.uninstallProtectionEnabled &&
            StrictModeSurfaceMatcher.isDeviceAdminDisableSurface(
                packageName = packageName,
                className = className,
                visibleText = visibleText
            )
        val blockSettings = state.blockDeviceSettings &&
            StrictModeSurfaceMatcher.isBlockedSettingsSurface(
                className = className,
                visibleText = visibleText
            )
        if (!blockAdminDisable && !blockSettings) return null

        return PolicyDecision.Blocked(
            targetType = TargetType.APP_PACKAGE,
            targetValue = packageName,
            reason = BlockReason.RULE_BLOCK,
            matchedRuleId = null
        )
    }

    private val STRICT_MODE_BLOCKED_PACKAGES = setOf(
        "com.android.settings",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller",
        "com.google.android.apps.nbu.files",
        "com.google.android.gms",
        "com.android.vending"
    )

    private val STRICT_MODE_BLOCKED_PACKAGE_PREFIXES = listOf(
        "com.samsung.android.settings",
        "com.miui.securitycenter",
        "com.miui.packageinstaller",
        "com.coloros.safecenter",
        "com.oplus.safecenter",
        "com.vivo.permissionmanager",
        "com.huawei.systemmanager"
    )
}
