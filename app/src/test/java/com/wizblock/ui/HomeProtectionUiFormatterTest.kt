package com.wizblock.ui

import com.google.common.truth.Truth.assertThat
import com.wizblock.model.StrictModeMode
import com.wizblock.model.StrictModeState
import org.junit.Test

class HomeProtectionUiFormatterTest {
    @Test
    fun timedFocusShowsCountdownWhenProtectionIsRunning() {
        val status = HomeProtectionUiFormatter.status(
            blockingEnabled = true,
            focusUntilMs = 1_900_000L,
            nowMs = 1_000_000L
        )

        assertThat(status.title).isEqualTo("Protection running")
        assertThat(status.subtitle).isEqualTo("15m remaining")
    }

    @Test
    fun strictTimerShowsCountdownWhenProtectionIsRunning() {
        val status = HomeProtectionUiFormatter.status(
            blockingEnabled = true,
            focusUntilMs = 0L,
            strictModeState = StrictModeState(
                active = true,
                mode = StrictModeMode.TIMER,
                timerExpiresAtMs = 1_900_000L,
                cooldownDurationMs = 0L,
                cooldownStartedAtMs = 0L,
                blockDeviceSettings = true
            ),
            nowMs = 1_000_000L
        )

        assertThat(status.title).isEqualTo("Strict Mode running")
        assertThat(status.subtitle).isEqualTo("15m remaining")
    }

    @Test
    fun untimedProtectionShowsNormalRunningState() {
        val status = HomeProtectionUiFormatter.status(
            blockingEnabled = true,
            focusUntilMs = 0L,
            strictModeState = StrictModeState.Inactive,
            nowMs = 1_000_000L
        )

        assertThat(status.subtitle).isEqualTo("Protection is on")
    }
}
