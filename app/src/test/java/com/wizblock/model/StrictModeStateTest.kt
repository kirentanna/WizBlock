package com.wizblock.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StrictModeStateTest {
    @Test
    fun inactiveDefault_hasUninstallProtectionOff() {
        assertThat(StrictModeState.Inactive.uninstallProtectionEnabled).isFalse()
    }

    @Test
    fun timerExpired_stillActiveButCanBeManuallyDisabled() {
        val now = 10_000L
        val state = StrictModeState(
            active = true,
            mode = StrictModeMode.TIMER,
            timerExpiresAtMs = now - 1L,
            cooldownDurationMs = 0L,
            cooldownStartedAtMs = 0L,
            blockDeviceSettings = true
        )

        assertThat(state.active).isTrue()
        assertThat(state.canManuallyDisable(now)).isTrue()
        assertThat(state.remainingMs(now)).isEqualTo(0L)
    }

    @Test
    fun timerRunning_cannotBeManuallyDisabled() {
        val now = 10_000L
        val state = StrictModeState(
            active = true,
            mode = StrictModeMode.TIMER,
            timerExpiresAtMs = now + 30_000L,
            cooldownDurationMs = 0L,
            cooldownStartedAtMs = 0L,
            blockDeviceSettings = true
        )

        assertThat(state.canManuallyDisable(now)).isFalse()
        assertThat(state.remainingMs(now)).isEqualTo(30_000L)
    }

}
