package com.wizblock.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeStrictModeUpgradePolicyTest {
    @Test
    fun runningTimedFocusUsesRemainingTimeForStrictMode() {
        val result = HomeStrictModeUpgradePolicy.resolve(
            blockingEnabled = true,
            focusUntilMs = 1_900_000L,
            selectedDurationMs = 25 * 60_000L,
            nowMs = 1_000_000L
        )

        assertThat(result).isEqualTo(HomeStrictModeUpgradeRequest.StrictTimer(900_000L))
    }

    @Test
    fun noLimitProtectionWithSelectedDurationUsesSelectedDuration() {
        val result = HomeStrictModeUpgradePolicy.resolve(
            blockingEnabled = true,
            focusUntilMs = 0L,
            selectedDurationMs = 25 * 60_000L,
            nowMs = 1_000_000L
        )

        assertThat(result).isEqualTo(HomeStrictModeUpgradeRequest.StrictTimer(25 * 60_000L))
    }

    @Test
    fun noLimitProtectionWithoutSelectedDurationCannotUpgradeToStrictMode() {
        val result = HomeStrictModeUpgradePolicy.resolve(
            blockingEnabled = true,
            focusUntilMs = 0L,
            selectedDurationMs = null,
            nowMs = 1_000_000L
        )

        assertThat(result).isEqualTo(HomeStrictModeUpgradeRequest.InvalidNoTimedFocus)
    }

    @Test
    fun expiredTimedFocusCannotUpgradeToStrictMode() {
        val result = HomeStrictModeUpgradePolicy.resolve(
            blockingEnabled = false,
            focusUntilMs = 999_999L,
            selectedDurationMs = 25 * 60_000L,
            nowMs = 1_000_000L
        )

        assertThat(result).isEqualTo(HomeStrictModeUpgradeRequest.InvalidNoTimedFocus)
    }
}
