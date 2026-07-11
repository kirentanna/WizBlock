package com.wizblock.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "block_events")
data class BlockEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "target_type") val targetType: String,
    @ColumnInfo(name = "target_value") val targetValue: String,
    @ColumnInfo(name = "browser_package") val browserPackage: String,
    @ColumnInfo(name = "reason") val reason: String,
    @ColumnInfo(name = "matched_rule_id") val matchedRuleId: String?,
    @ColumnInfo(name = "session_id") val sessionId: String?,
    @ColumnInfo(name = "blocked_at") val blockedAt: Long
)
