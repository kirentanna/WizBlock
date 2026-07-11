package com.wizblock.overlay

import com.wizblock.model.TargetDisplayInfo

data class OverlayDetailRow(
    val label: String,
    val value: String
)

object OverlayBlockDetailsFormatter {
    fun rows(
        target: TargetDisplayInfo,
        reason: String,
        details: OverlayBlockDetails
    ): List<OverlayDetailRow> {
        return buildList {
            add(OverlayDetailRow("Target", target.title))
            details.todayAttemptCount?.let { count ->
                add(OverlayDetailRow("Today", attemptCountText(count)))
            }
            add(OverlayDetailRow("Reason", "$reason - ${target.subtitle}"))
            add(OverlayDetailRow("Message", motivationalMessage(details.todayAttemptCount)))
            add(OverlayDetailRow("Source", "Local rule"))
        }
    }

    private fun attemptCountText(count: Int): String {
        return if (count == 1) {
            "Blocked 1 time"
        } else {
            "Blocked $count times"
        }
    }

    private fun motivationalMessage(todayAttemptCount: Int?): String {
        return when {
            todayAttemptCount == null -> "Good call. Stay with the task."
            todayAttemptCount >= 5 -> "Strong pattern noticed. Keep the boundary."
            todayAttemptCount >= 2 -> "Good call. Stay with the task."
            else -> "Good catch. Stay with the task."
        }
    }
}
