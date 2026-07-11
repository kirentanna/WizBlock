package com.wizblock.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageLimitDao {
    @Query("SELECT * FROM usage_limits ORDER BY created_at DESC")
    fun observeAll(): Flow<List<UsageLimitEntity>>

    @Query("SELECT * FROM usage_limits WHERE enabled = 1 ORDER BY created_at DESC")
    fun observeEnabled(): Flow<List<UsageLimitEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: UsageLimitEntity)

    @Query(
        """
        SELECT * FROM usage_limits
        WHERE target_type = :targetType
            AND LOWER(target_value) = LOWER(:targetValue)
            AND IFNULL(profile_id, 'default') = :profileId
        LIMIT 1
        """
    )
    suspend fun findByTarget(targetType: String, targetValue: String, profileId: String): UsageLimitEntity?

    @Query(
        """
        UPDATE usage_limits
        SET minutes_per_day = :minutesPerDay,
            opens_per_day = :opensPerDay,
            enabled = :enabled,
            updated_at = :updatedAt
        WHERE id = :id
        """
    )
    suspend fun updateValues(
        id: String,
        minutesPerDay: Int,
        opensPerDay: Int,
        enabled: Boolean,
        updatedAt: Long
    )

    @Query("UPDATE usage_limits SET enabled = :enabled, updated_at = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean, updatedAt: Long)

    @Query("DELETE FROM usage_limits WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM usage_limits WHERE IFNULL(profile_id, 'default') = :profileId")
    suspend fun deleteByProfileId(profileId: String)
}
