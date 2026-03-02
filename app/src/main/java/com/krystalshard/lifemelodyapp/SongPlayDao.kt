package com.krystalshard.lifemelodyapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface SongPlayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongPlayEntity)

    @Update
    suspend fun update(song: SongPlayEntity)

    @Query("SELECT * FROM song_plays WHERE id = :songId")
    suspend fun getSongPlayById(songId: Long): SongPlayEntity?

    @Query("SELECT * FROM song_plays ORDER BY playCount DESC LIMIT 10")
    suspend fun getTop10Songs(): List<SongPlayEntity>
}