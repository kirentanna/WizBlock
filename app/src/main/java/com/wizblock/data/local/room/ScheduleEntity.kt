package com.wizblock.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedules")
data class ScheduleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "target_type") val targetType: String,
    @ColumnInfo(name = "target_value") val targetValue: String,
    @ColumnInfo(name = "profile_id") val profileId: String?,
    @ColumnInfo(name = "start_minute") val startMinute: Int,
    @ColumnInfo(name = "end_minute") val endMinute: Int,
    @ColumnInfo(name = "days_mask") val daysMask: Int,
    @ColumnInfo(name = "enabled") val enabled: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
