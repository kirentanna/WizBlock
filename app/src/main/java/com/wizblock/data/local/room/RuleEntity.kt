package com.wizblock.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "kind") val kind: String,
    @ColumnInfo(name = "action") val action: String,
    @ColumnInfo(name = "operator") val operator: String,
    @ColumnInfo(name = "value") val value: String,
    @ColumnInfo(name = "profile_id") val profileId: String?,
    @ColumnInfo(name = "enabled") val enabled: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "updated_at") val updatedAt: Long
)
