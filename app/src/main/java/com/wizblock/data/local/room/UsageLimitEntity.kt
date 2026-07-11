package com.wizblock.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_limits")
data class UsageLimitEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "target_type") val targetType: String,
    @ColumnInfo(name = "target_value") val targetValue: String,
    @ColumnInfo(name = "profile_id") val profileId: String?,
    @ColumnInfo(name = "minutes_per_day") val minutesPerDay: Int,
    @ColumnInfo(name = "opens_per_day") val opensPerDay: Int,
    @ColumnInfo(name = "enabled") val enabled: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
