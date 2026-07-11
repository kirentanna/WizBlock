package com.wizblock.ui

import com.wizblock.model.TargetType
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TargetSummaryLine(
    val label: String,
    val value: String
)

object BlockedTargetUiFormatter {
    fun scheduleSummary(target: BlockedTargetSettingsItem): TargetSummaryLine {
        val schedule = target.schedule ?: return TargetSummaryLine("Schedule", "Always blocked")
        val dayText = describeDays(schedule.daysMask)
        val value = if (schedule.startMinute == schedule.endMinute) {
            "$dayText, all day"
        } else {
            "$dayText, ${formatMinuteOfDay(schedule.startMinute)} - ${formatMinuteOfDay(schedule.endMinute)}"
        }
        return TargetSummaryLine("Schedule", value)
    }

    fun usageLimitSummary(target: BlockedTargetSettingsItem): TargetSummaryLine {
        val limit = target.usageLimit ?: return TargetSummaryLine("Daily limit", "No limit")
        val value = when {
            limit.minutesPerDay > 0 -> "${limit.minutesPerDay} min per day"
            limit.opensPerDay > 0 -> "${limit.opensPerDay} opens per day"
            else -> "No limit"
        }
        return TargetSummaryLine("Daily limit", value)
    }

    fun targetTypeLabel(targetType: TargetType): String {
        return when (targetType) {
            TargetType.APP_PACKAGE -> "App"
            TargetType.DOMAIN -> "Website"
            TargetType.KEYWORD -> "Keyword"
        }
    }

    private fun describeDays(daysMask: Int): String {
        return when (daysMask) {
            ALL_DAYS_MASK -> "Every day"
            WEEKDAYS_MASK -> "Weekdays"
            WEEKEND_MASK -> "Weekends"
            else -> DAY_OPTIONS.filter { daysMask and it.mask != 0 }.joinToString { it.longLabel }
        }
    }

    private fun formatMinuteOfDay(minuteOfDay: Int): String {
        val safe = minuteOfDay.coerceIn(0, 1439)
        val localTime = LocalTime.of(safe / 60, safe % 60)
        return localTime.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    }

    private data class DayOption(val longLabel: String, val mask: Int)

    private val DAY_OPTIONS = listOf(
        DayOption("Sun", 1 shl 6),
        DayOption("Mon", 1 shl 0),
        DayOption("Tue", 1 shl 1),
        DayOption("Wed", 1 shl 2),
        DayOption("Thu", 1 shl 3),
        DayOption("Fri", 1 shl 4),
        DayOption("Sat", 1 shl 5)
    )

    private const val ALL_DAYS_MASK = 0b1111111
    private const val WEEKDAYS_MASK = (1 shl 0) or (1 shl 1) or (1 shl 2) or (1 shl 3) or (1 shl 4)
    private const val WEEKEND_MASK = (1 shl 5) or (1 shl 6)
}
