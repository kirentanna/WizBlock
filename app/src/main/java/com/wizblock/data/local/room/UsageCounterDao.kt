package com.wizblock.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageCounterDao {
    @Query("SELECT * FROM usage_counters WHERE local_date = :localDate")
    fun observeByDate(localDate: String): Flow<List<UsageCounterEntity>>

    @Query("SELECT * FROM usage_counters WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): UsageCounterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: UsageCounterEntity)
}
