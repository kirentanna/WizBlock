package com.wizblock.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules ORDER BY created_at DESC")
    fun observeAll(): Flow<List<ScheduleEntity>>

    @Query("SELECT * FROM schedules WHERE enabled = 1 ORDER BY created_at DESC")
    fun observeEnabled(): Flow<List<ScheduleEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: ScheduleEntity)

    @Query(
        """
        SELECT * FROM schedules
        WHERE target_type = :targetType
            AND LOWER(target_value) = LOWER(:targetValue)
            AND IFNULL(profile_id, 'default') = :profileId
        LIMIT 1
        """
    )
    suspend fun findByTarget(targetType: String, targetValue: String, profileId: String): ScheduleEntity?

    @Query(
        """
        UPDATE schedules
        SET start_minute = :startMinute,
            end_minute = :endMinute,
            days_mask = :daysMask,
            enabled = :enabled,
            updated_at = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateWindow(
        id: String,
        startMinute: Int,
        endMinute: Int,
        daysMask: Int,
        enabled: Boolean,
        updatedAt: Long
    )

    @Query("UPDATE schedules SET enabled = :enabled, updated_at = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean, updatedAt: Long)

    @Query("DELETE FROM schedules WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM schedules WHERE IFNULL(profile_id, 'default') = :profileId")
    suspend fun deleteByProfileId(profileId: String)
}
