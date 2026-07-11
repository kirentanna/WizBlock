package com.wizblock.model

data class ScheduleWindow(
    val id: String,
    val targetType: TargetType,
    val targetValue: String,
    val profileId: String? = DEFAULT_PROFILE_ID,
    val startMinute: Int,
    val endMinute: Int,
    val daysMask: Int,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
