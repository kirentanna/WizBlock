package com.wizblock.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class BlockSummaryRow(
    val targetType: String,
    val count: Long
)

data class TopBlockedRow(
    val targetType: String,
    val targetValue: String,
    val count: Long
)

@Dao
interface BlockEventDao {
    @Query("SELECT * FROM block_events ORDER BY blocked_at DESC LIMIT 20")
    fun observeRecent(): Flow<List<BlockEventEntity>>

    @Query("SELECT target_type AS targetType, COUNT(*) AS count FROM block_events WHERE blocked_at BETWEEN :startMs AND :endMs GROUP BY target_type")
    fun observeDailySummary(startMs: Long, endMs: Long): Flow<List<BlockSummaryRow>>

    @Query("SELECT target_type AS targetType, target_value AS targetValue, COUNT(*) AS count FROM block_events WHERE blocked_at BETWEEN :startMs AND :endMs GROUP BY target_type, target_value ORDER BY count DESC LIMIT :limit")
    fun observeTopBlocked(startMs: Long, endMs: Long, limit: Int): Flow<List<TopBlockedRow>>

    @Query("SELECT COUNT(*) FROM block_events WHERE target_type = :targetType AND target_value = :targetValue AND blocked_at BETWEEN :startMs AND :endMs")
    suspend fun countTargetBlocks(targetType: String, targetValue: String, startMs: Long, endMs: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BlockEventEntity)

    @Query("DELETE FROM block_events WHERE id NOT IN (SELECT id FROM block_events ORDER BY blocked_at DESC LIMIT 200)")
    suspend fun trimToRecent200()
}
