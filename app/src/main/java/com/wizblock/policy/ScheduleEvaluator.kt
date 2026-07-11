package com.wizblock.policy

import com.wizblock.model.ScheduleWindow
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

class ScheduleEvaluator(
    private val zoneIdProvider: () -> ZoneId = { ZoneId.systemDefault() }
) {
    fun isPolicyActive(schedules: List<ScheduleWindow>, nowMs: Long): Boolean {
        val enabled = schedules.filter { it.enabled }
        if (enabled.isEmpty()) return true
        return enabled.any { isWindowActive(it, nowMs) }
    }

    fun isWindowActive(schedule: ScheduleWindow, nowMs: Long): Boolean {
        val zone = zoneIdProvider()
        val now = Instant.ofEpochMilli(nowMs).atZone(zone)
        val minuteOfDay = now.hour * 60 + now.minute
        val dayBit = dayToBit(now.dayOfWeek)

        if (schedule.startMinute == schedule.endMinute) {
            return hasDay(schedule.daysMask, dayBit)
        }

        return if (schedule.startMinute < schedule.endMinute) {
            hasDay(schedule.daysMask, dayBit) &&
                minuteOfDay in schedule.startMinute until schedule.endMinute
        } else {
            val previousDayBit = dayToBit(now.minusDays(1).dayOfWeek)
            val activeLate = hasDay(schedule.daysMask, dayBit) && minuteOfDay >= schedule.startMinute
            val activeEarly = hasDay(schedule.daysMask, previousDayBit) && minuteOfDay < schedule.endMinute
            activeLate || activeEarly
        }
    }

    private fun hasDay(mask: Int, bit: Int): Boolean = (mask and bit) != 0

    private fun dayToBit(dayOfWeek: DayOfWeek): Int {
        return when (dayOfWeek) {
            DayOfWeek.MONDAY -> 1 shl 0
            DayOfWeek.TUESDAY -> 1 shl 1
            DayOfWeek.WEDNESDAY -> 1 shl 2
            DayOfWeek.THURSDAY -> 1 shl 3
            DayOfWeek.FRIDAY -> 1 shl 4
            DayOfWeek.SATURDAY -> 1 shl 5
            DayOfWeek.SUNDAY -> 1 shl 6
        }
    }
}
