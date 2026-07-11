package com.wizblock.ui

object HomeDurationSessionPolicy {
    const val PRESET_COUNT = 3
    const val MIN_CUSTOM_MINUTES = 5
    const val MIN_CUSTOM_DURATION_MS = MIN_CUSTOM_MINUTES * 60_000L
    const val MAX_CUSTOM_DAYS = 14
    const val MAX_CUSTOM_DURATION_MS = MAX_CUSTOM_DAYS * 24L * 60L * 60_000L

    fun defaultPresetDurationsMs(): List<Long> {
        return listOf(
            25L * 60_000L,
            60L * 60_000L,
            3L * 60L * 60_000L
        )
    }

    fun defaultLastCustomDurationMs(): Long = 2L * 60L * 60_000L

    fun normalizePresetDurationsMs(durationsMs: List<Long>): List<Long> {
        val defaults = defaultPresetDurationsMs()
        return List(PRESET_COUNT) { index ->
            normalizeDurationMs(durationsMs.getOrNull(index) ?: defaults[index])
        }
    }

    fun normalizeDurationMs(durationMs: Long): Long {
        return durationMs.coerceIn(MIN_CUSTOM_DURATION_MS, MAX_CUSTOM_DURATION_MS)
    }

    fun customDurationMs(minutes: Int, hours: Int, days: Int): Long {
        val safeMinutes = minutes.coerceIn(0, 55)
        val safeHours = hours.coerceIn(0, 23)
        val safeDays = days.coerceIn(0, MAX_CUSTOM_DAYS)
        val totalMinutes = (safeDays * 24L * 60L) + (safeHours * 60L) + safeMinutes
        return normalizeDurationMs(totalMinutes * 60_000L)
    }

    fun partsFromDuration(durationMs: Long): DurationParts {
        val totalMinutes = (normalizeDurationMs(durationMs) / 60_000L).coerceAtLeast(MIN_CUSTOM_MINUTES.toLong())
        val days = (totalMinutes / (24L * 60L)).toInt().coerceIn(0, MAX_CUSTOM_DAYS)
        val remainingAfterDays = totalMinutes - (days * 24L * 60L)
        val hours = (remainingAfterDays / 60L).toInt().coerceIn(0, 23)
        val minutes = (remainingAfterDays % 60L).toInt().coerceIn(0, 55)
        return DurationParts(minutes = roundToFiveMinutes(minutes), hours = hours, days = days)
    }

    fun labelForDuration(durationMs: Long): String {
        val totalMinutes = (normalizeDurationMs(durationMs) / 60_000L).coerceAtLeast(1L)
        val days = totalMinutes / (24L * 60L)
        val hours = (totalMinutes % (24L * 60L)) / 60L
        val minutes = totalMinutes % 60L
        return when {
            days > 0L && hours > 0L -> "${days}d ${hours}h"
            days > 0L -> "${days}d"
            hours > 0L && minutes > 0L -> "${hours}h ${minutes}m"
            hours > 0L -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    private fun roundToFiveMinutes(minutes: Int): Int {
        return (minutes / 5 * 5).coerceIn(0, 55)
    }
}

data class DurationParts(
    val minutes: Int,
    val hours: Int,
    val days: Int
)
