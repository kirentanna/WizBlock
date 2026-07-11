package com.wizblock.ui

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class HomeStrictModeControlPolicyTest {
    @Test
    fun selectedDurationBeforeStartAllowsStrictModeSelection() {
        val state = HomeStrictModeControlPolicy.resolve(
            strictModeActive = false,
            selectedDurationMs = 25 * 60_000L,
            blockingEnabled = false,
            focusUntilMs = 0L,
            nowMs = 1_000L,
            localStrictModeSelected = true
        )

        assertThat(state.display).isEqualTo(HomeStrictModeControlDisplay.Selectable)
        assertThat(state.strictSelected).isTrue()
        assertThat(state.action).isEqualTo(HomeStrictModeControlAction.SetLocalSelection)
    }

    @Test
    fun noLimitStrictModeAttemptShowsDurationRequiredFeedback() {
        val state = HomeStrictModeControlPolicy.resolve(
            strictModeActive = false,
            selectedDurationMs = null,
            blockingEnabled = false,
            focusUntilMs = 0L,
            nowMs = 1_000L,
            localStrictModeSelected = false
        )

        assertThat(state.display).isEqualTo(HomeStrictModeControlDisplay.DurationRequired)
        assertThat(state.strictSelected).isFalse()
        assertThat(state.action).isEqualTo(HomeStrictModeControlAction.ShowDurationRequired)
    }

    @Test
    fun runningTimedFocusShowsUpgradeAction() {
        val state = HomeStrictModeControlPolicy.resolve(
            strictModeActive = false,
            selectedDurationMs = null,
            blockingEnabled = true,
            focusUntilMs = 2_000L,
            nowMs = 1_000L,
            localStrictModeSelected = false
        )

        assertThat(state.display).isEqualTo(HomeStrictModeControlDisplay.UpgradeTimedFocus)
        assertThat(state.strictSelected).isFalse()
        assertThat(state.action).isEqualTo(HomeStrictModeControlAction.UpgradeTimedFocus)
    }

    @Test
    fun runningNoLimitProtectionWithSelectedDurationShowsUpgradeAction() {
        val state = HomeStrictModeControlPolicy.resolve(
            strictModeActive = false,
            selectedDurationMs = 25 * 60_000L,
            blockingEnabled = true,
            focusUntilMs = 0L,
            nowMs = 1_000L,
            localStrictModeSelected = false
        )

        assertThat(state.display).isEqualTo(HomeStrictModeControlDisplay.UpgradeSelectedDuration)
        assertThat(state.strictSelected).isFalse()
        assertThat(state.action).isEqualTo(HomeStrictModeControlAction.UpgradeSelectedDuration)
    }

    @Test
    fun runningNoLimitProtectionWithoutSelectedDurationShowsDurationRequiredFeedback() {
        val state = HomeStrictModeControlPolicy.resolve(
            strictModeActive = false,
            selectedDurationMs = null,
            blockingEnabled = true,
            focusUntilMs = 0L,
            nowMs = 1_000L,
            localStrictModeSelected = false
        )

        assertThat(state.display).isEqualTo(HomeStrictModeControlDisplay.DurationRequired)
        assertThat(state.strictSelected).isFalse()
        assertThat(state.action).isEqualTo(HomeStrictModeControlAction.ShowDurationRequired)
    }

    @Test
    fun activeStrictModeShowsLockedStatusInsteadOfDisabledControl() {
        val state = HomeStrictModeControlPolicy.resolve(
            strictModeActive = true,
            selectedDurationMs = null,
            blockingEnabled = true,
            focusUntilMs = 0L,
            nowMs = 1_000L,
            localStrictModeSelected = false
        )

        assertThat(state.display).isEqualTo(HomeStrictModeControlDisplay.Locked)
        assertThat(state.strictSelected).isTrue()
        assertThat(state.action).isEqualTo(HomeStrictModeControlAction.None)
    }
}
