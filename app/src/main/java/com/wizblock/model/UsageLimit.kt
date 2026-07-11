package com.wizblock.model

data class UsageLimit(
    val id: String,
    val targetType: TargetType,
    val targetValue: String,
    val profileId: String? = DEFAULT_PROFILE_ID,
    val minutesPerDay: Int,
    val opensPerDay: Int,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
