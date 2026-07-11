package com.wizblock.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StrictModeEditPolicyTest {
    @Test
    fun weakeningProtection_isDeniedOnlyWhenStrictModeIsActive() {
        assertThat(StrictModeEditPolicy.canWeakenProtection(strictModeActive = true)).isFalse()
        assertThat(StrictModeEditPolicy.canWeakenProtection(strictModeActive = false)).isTrue()
    }

    @Test
    fun uninstallProtectionDisable_isDeniedOnlyWhenStrictModeIsActive() {
        assertThat(StrictModeEditPolicy.canDisableUninstallProtection(strictModeActive = true)).isFalse()
        assertThat(StrictModeEditPolicy.canDisableUninstallProtection(strictModeActive = false)).isTrue()
    }

    @Test
    fun newUsageLimit_isAllowedDuringStrictMode() {
        assertThat(
            StrictModeEditPolicy.canChangeUsageLimit(
                strictModeActive = true,
                existing = null,
                minutesPerDay = 15,
                opensPerDay = 0
            )
        ).isTrue()
    }

    @Test
    fun loweringExistingUsageLimit_isAllowedDuringStrictMode() {
        val existing = usageLimit(minutesPerDay = 30, opensPerDay = 0)

        assertThat(
            StrictModeEditPolicy.canChangeUsageLimit(
                strictModeActive = true,
                existing = existing,
                minutesPerDay = 15,
                opensPerDay = 0
            )
        ).isTrue()
    }

    @Test
    fun raisingExistingUsageLimit_isDeniedDuringStrictMode() {
        val existing = usageLimit(minutesPerDay = 30, opensPerDay = 0)

        assertThat(
            StrictModeEditPolicy.canChangeUsageLimit(
                strictModeActive = true,
                existing = existing,
                minutesPerDay = 60,
                opensPerDay = 0
            )
        ).isFalse()
    }

    @Test
    fun removingExistingUsageLimitDimension_isDeniedDuringStrictMode() {
        val existing = usageLimit(minutesPerDay = 30, opensPerDay = 0)

        assertThat(
            StrictModeEditPolicy.canChangeUsageLimit(
                strictModeActive = true,
                existing = existing,
                minutesPerDay = 0,
                opensPerDay = 5
            )
        ).isFalse()
    }

    private fun usageLimit(minutesPerDay: Int, opensPerDay: Int): UsageLimit {
        return UsageLimit(
            id = "limit",
            targetType = TargetType.APP_PACKAGE,
            targetValue = "com.example.app",
            minutesPerDay = minutesPerDay,
            opensPerDay = opensPerDay,
            enabled = true,
            createdAt = 1L,
            updatedAt = 1L
        )
    }
}
