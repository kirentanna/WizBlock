package com.wizblock.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "usage_counters")
data class UsageCounterEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "local_date") val localDate: String,
    @ColumnInfo(name = "target_type") val targetType: String,
    @ColumnInfo(name = "target_value") val targetValue: String,
    @ColumnInfo(name = "used_seconds") val usedSeconds: Int,
    @ColumnInfo(name = "open_count") val openCount: Int,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
