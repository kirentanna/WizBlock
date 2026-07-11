package com.wizblock.ui

import com.wizblock.model.StrictModeState

data class HomeProtectionStatus(
    val title: String,
    val subtitle: String
)

object HomeProtectionUiFormatter {
    fun status(
        blockingEnabled: Boolean,
        focusUntilMs: Long,
        strictModeState: StrictModeState = StrictModeState.Inactive,
        nowMs: Long
    ): HomeProtectionStatus {
        if (!blockingEnabled) {
            return HomeProtectionStatus(
                title = "Start protection",
                subtitle = "Protection is off"
            )
        }
        val strictRemainingMs = strictModeState.remainingMs(nowMs)
        if (strictModeState.active && strictRemainingMs > 0L) {
            return HomeProtectionStatus(
                title = "Strict Mode running",
                subtitle = "${formatRemaining(strictRemainingMs)} remaining"
            )
        }
        val remainingMs = focusUntilMs - nowMs
        if (focusUntilMs > 0L && remainingMs > 0L) {
            return HomeProtectionStatus(
                title = "Protection running",
                subtitle = "${formatRemaining(remainingMs)} remaining"
            )
        }
        return HomeProtectionStatus(
            title = "Protection running",
            subtitle = "Protection is on"
        )
    }

    private fun formatRemaining(ms: Long): String {
        val totalSeconds = ((ms + 999L) / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        return when {
            hours > 0L -> "${hours}h ${minutes}m"
            minutes > 0L -> "${minutes}m"
            else -> "${totalSeconds}s"
        }
    }
}
