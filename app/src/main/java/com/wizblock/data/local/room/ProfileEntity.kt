package com.wizblock.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "color_token") val colorToken: String,
    @ColumnInfo(name = "icon_name") val iconName: String,
    @ColumnInfo(name = "mode") val mode: String,
    @ColumnInfo(name = "enabled") val enabled: Boolean
)
