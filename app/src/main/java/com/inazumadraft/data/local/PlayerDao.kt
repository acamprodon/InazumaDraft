package com.inazumadraft.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players")
    suspend fun getAll(): List<PlayerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(players: List<PlayerEntity>)

    @Query("DELETE FROM players WHERE id = :playerId")
    suspend fun deleteById(playerId: Long)

    @Query("SELECT COUNT(*) FROM players")
    suspend fun count(): Int
}