package com.wizblock.model

object StrictModeEditPolicy {
    fun canWeakenProtection(strictModeActive: Boolean): Boolean {
        return !strictModeActive
    }

    fun canDisableUninstallProtection(strictModeActive: Boolean): Boolean {
        return !strictModeActive
    }

    fun canChangeUsageLimit(
        strictModeActive: Boolean,
        existing: UsageLimit?,
        minutesPerDay: Int,
        opensPerDay: Int
    ): Boolean {
        if (!strictModeActive) return true
        if (existing == null) return minutesPerDay > 0 || opensPerDay > 0

        if (existing.minutesPerDay > 0) {
            if (minutesPerDay <= 0 || minutesPerDay > existing.minutesPerDay) return false
        }
        if (existing.opensPerDay > 0) {
            if (opensPerDay <= 0 || opensPerDay > existing.opensPerDay) return false
        }
        return minutesPerDay > 0 || opensPerDay > 0
    }
}
