package com.krystalshard.lifemelodyapp

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteSongDao {

    @Query("SELECT * FROM favorite_songs ORDER BY title ASC")
    fun getAllFavorites(): Flow<List<FavoriteSongEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_songs WHERE id = :songId LIMIT 1)")
    suspend fun isFavorite(songId: Long): Boolean

    @Insert
    suspend fun insert(favoriteSong: FavoriteSongEntity)

    @Delete
    suspend fun delete(favoriteSong: FavoriteSongEntity)
}