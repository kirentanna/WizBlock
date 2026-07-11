package com.wizblock.model

data class UsageProgress(
    val targetType: TargetType,
    val targetValue: String,
    val usedSeconds: Int,
    val openCount: Int,
    val minutesPerDay: Int,
    val opensPerDay: Int
)
