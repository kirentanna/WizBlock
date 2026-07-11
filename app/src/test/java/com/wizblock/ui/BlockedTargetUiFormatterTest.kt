package com.wizblock.ui

import com.google.common.truth.Truth.assertThat
import com.wizblock.model.ScheduleWindow
import com.wizblock.model.TargetType
import com.wizblock.model.UsageLimit
import org.junit.Test

class BlockedTargetUiFormatterTest {
    @Test
    fun scheduleSummaryWithoutScheduleShowsAlwaysBlocked() {
        val target = blockedTarget(schedule = null)

        val summary = BlockedTargetUiFormatter.scheduleSummary(target)

        assertThat(summary.label).isEqualTo("Schedule")
        assertThat(summary.value).isEqualTo("Always blocked")
    }

    @Test
    fun scheduleSummaryWithWeekdayWindowShowsReadableDaysAndTimes() {
        val target = blockedTarget(
            schedule = ScheduleWindow(
                id = "schedule-1",
                targetType = TargetType.DOMAIN,
                targetValue = "chess.com",
                startMinute = 9 * 60,
                endMinute = 17 * 60,
                daysMask = WEEKDAYS_MASK,
                enabled = true,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        val summary = BlockedTargetUiFormatter.scheduleSummary(target)

        assertThat(summary.value).contains("Weekdays")
        assertThat(summary.value).contains("9:00")
        assertThat(summary.value).contains("5:00")
    }

    @Test
    fun usageLimitSummaryShowsMinutesOrOpens() {
        val minutes = blockedTarget(
            usageLimit = UsageLimit(
                id = "limit-1",
                targetType = TargetType.DOMAIN,
                targetValue = "chess.com",
                minutesPerDay = 30,
                opensPerDay = 0,
                enabled = true,
                createdAt = 1L,
                updatedAt = 1L
            )
        )
        val opens = blockedTarget(
            usageLimit = UsageLimit(
                id = "limit-2",
                targetType = TargetType.DOMAIN,
                targetValue = "chess.com",
                minutesPerDay = 0,
                opensPerDay = 3,
                enabled = true,
                createdAt = 1L,
                updatedAt = 1L
            )
        )

        assertThat(BlockedTargetUiFormatter.usageLimitSummary(minutes).value).isEqualTo("30 min per day")
        assertThat(BlockedTargetUiFormatter.usageLimitSummary(opens).value).isEqualTo("3 opens per day")
    }

    @Test
    fun targetTypeLabelUsesUserVisibleNames() {
        assertThat(BlockedTargetUiFormatter.targetTypeLabel(TargetType.APP_PACKAGE)).isEqualTo("App")
        assertThat(BlockedTargetUiFormatter.targetTypeLabel(TargetType.DOMAIN)).isEqualTo("Website")
        assertThat(BlockedTargetUiFormatter.targetTypeLabel(TargetType.KEYWORD)).isEqualTo("Keyword")
    }

    private fun blockedTarget(
        schedule: ScheduleWindow? = null,
        usageLimit: UsageLimit? = null
    ): BlockedTargetSettingsItem {
        return BlockedTargetSettingsItem(
            targetType = TargetType.DOMAIN,
            targetValue = "chess.com",
            title = "chess.com",
            subtitle = "Website",
            schedule = schedule,
            usageLimit = usageLimit
        )
    }

    private companion object {
        const val WEEKDAYS_MASK = (1 shl 0) or (1 shl 1) or (1 shl 2) or (1 shl 3) or (1 shl 4)
    }
}
