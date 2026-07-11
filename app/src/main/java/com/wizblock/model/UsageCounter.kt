package com.wizblock.model

data class UsageCounter(
    val id: String,
    val localDate: String,
    val targetType: TargetType,
    val targetValue: String,
    val usedSeconds: Int,
    val openCount: Int,
    val updatedAt: Long
)
