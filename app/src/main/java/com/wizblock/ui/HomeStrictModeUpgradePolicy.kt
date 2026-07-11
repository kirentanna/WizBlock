package com.wizblock.ui

sealed interface HomeStrictModeUpgradeRequest {
    data class StrictTimer(val durationMs: Long) : HomeStrictModeUpgradeRequest
    data object InvalidNoTimedFocus : HomeStrictModeUpgradeRequest
}

object HomeStrictModeUpgradePolicy {
    fun resolve(
        blockingEnabled: Boolean,
        focusUntilMs: Long,
        selectedDurationMs: Long?,
        nowMs: Long
    ): HomeStrictModeUpgradeRequest {
        val remainingMs = focusUntilMs - nowMs
        return when {
            focusUntilMs > 0L && remainingMs > 0L -> {
                HomeStrictModeUpgradeRequest.StrictTimer(remainingMs.coerceAtLeast(60_000L))
            }

            blockingEnabled && selectedDurationMs != null -> {
                HomeStrictModeUpgradeRequest.StrictTimer(selectedDurationMs.coerceAtLeast(60_000L))
            }

            else -> HomeStrictModeUpgradeRequest.InvalidNoTimedFocus
        }
    }
}
