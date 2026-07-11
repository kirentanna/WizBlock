package com.wizblock.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY name ASC")
    fun observeAll(): Flow<List<ProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProfileEntity)

    @Query("UPDATE profiles SET enabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteById(id: String)
}
