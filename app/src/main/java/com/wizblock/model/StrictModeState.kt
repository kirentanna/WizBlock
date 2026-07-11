package com.wizblock.model

data class StrictModeState(
    val active: Boolean,
    val mode: StrictModeMode,
    val timerExpiresAtMs: Long,
    val cooldownDurationMs: Long,
    val cooldownStartedAtMs: Long,
    val blockDeviceSettings: Boolean,
    val uninstallProtectionEnabled: Boolean = false
) {
    fun canManuallyDisable(nowMs: Long): Boolean {
        if (!active) return true
        return when (mode) {
            StrictModeMode.TIMER -> timerExpiresAtMs > 0L && nowMs >= timerExpiresAtMs
            StrictModeMode.COOLDOWN -> true
        }
    }

    fun remainingMs(nowMs: Long): Long {
        if (!active) return 0L
        val unlockAt = when (mode) {
            StrictModeMode.TIMER -> timerExpiresAtMs
            StrictModeMode.COOLDOWN -> 0L
        }
        if (unlockAt <= 0L) return 0L
        return (unlockAt - nowMs).coerceAtLeast(0L)
    }

    companion object {
        val Inactive = StrictModeState(
            active = false,
            mode = StrictModeMode.TIMER,
            timerExpiresAtMs = 0L,
            cooldownDurationMs = 0L,
            cooldownStartedAtMs = 0L,
            blockDeviceSettings = true,
            uninstallProtectionEnabled = false
        )
    }
}
