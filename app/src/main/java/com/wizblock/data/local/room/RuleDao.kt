package com.wizblock.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleDao {
    @Query("SELECT * FROM rules ORDER BY created_at DESC")
    fun observeAll(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE enabled = 1 ORDER BY created_at DESC")
    fun observeEnabled(): Flow<List<RuleEntity>>

    @Query("SELECT * FROM rules WHERE enabled = 1 ORDER BY created_at DESC")
    suspend fun listEnabled(): List<RuleEntity>

    @Query(
        """
        SELECT * FROM rules
        WHERE kind = :kind
            AND action = :action
            AND operator = :operator
            AND LOWER(value) = LOWER(:value)
            AND IFNULL(profile_id, 'default') = :profileId
        LIMIT 1
        """
    )
    suspend fun findByIdentity(
        kind: String,
        action: String,
        operator: String,
        value: String,
        profileId: String
    ): RuleEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: RuleEntity)

    @Query("UPDATE rules SET enabled = :enabled, updated_at = :updatedAt WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean, updatedAt: Long)

    @Query("DELETE FROM rules WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM rules WHERE IFNULL(profile_id, 'default') = :profileId")
    suspend fun deleteByProfileId(profileId: String)
}
