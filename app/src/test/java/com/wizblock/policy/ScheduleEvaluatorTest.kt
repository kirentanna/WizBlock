package com.wizblock.policy

import com.google.common.truth.Truth.assertThat
import com.wizblock.model.ScheduleWindow
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Test

class ScheduleEvaluatorTest {
    private val zoneId = ZoneId.of("Asia/Singapore")
    private val evaluator = ScheduleEvaluator { zoneId }

    @Test
    fun isPolicyActive_noSchedules_true() {
        val result = evaluator.isPolicyActive(emptyList(), now("2026-04-05T10:00:00"))
        assertThat(result).isTrue()
    }

    @Test
    fun isWindowActive_sameDayRange() {
        val schedule = schedule(startMinute = 9 * 60, endMinute = 17 * 60, daysMask = monToFriMask())
        assertThat(evaluator.isWindowActive(schedule, now("2026-04-06T10:00:00"))).isTrue() // Monday
        assertThat(evaluator.isWindowActive(schedule, now("2026-04-06T18:00:00"))).isFalse()
    }

    @Test
    fun isWindowActive_overnightRange() {
        val schedule = schedule(startMinute = 22 * 60, endMinute = 2 * 60, daysMask = mondayMask())
        assertThat(evaluator.isWindowActive(schedule, now("2026-04-06T23:00:00"))).isTrue() // Monday night
        assertThat(evaluator.isWindowActive(schedule, now("2026-04-07T01:00:00"))).isTrue() // Tuesday early from Monday window
        assertThat(evaluator.isWindowActive(schedule, now("2026-04-07T03:00:00"))).isFalse()
    }

    private fun schedule(startMinute: Int, endMinute: Int, daysMask: Int): ScheduleWindow {
        val now = System.currentTimeMillis()
        return ScheduleWindow(
            id = "s1",
            targetType = com.wizblock.model.TargetType.DOMAIN,
            targetValue = "example.com",
            startMinute = startMinute,
            endMinute = endMinute,
            daysMask = daysMask,
            enabled = true,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun monToFriMask(): Int {
        return (1 shl 0) or (1 shl 1) or (1 shl 2) or (1 shl 3) or (1 shl 4)
    }

    private fun mondayMask(): Int = 1 shl 0

    private fun now(isoDateTime: String): Long {
        return LocalDateTime.parse(isoDateTime).atZone(zoneId).toInstant().toEpochMilli()
    }
}
