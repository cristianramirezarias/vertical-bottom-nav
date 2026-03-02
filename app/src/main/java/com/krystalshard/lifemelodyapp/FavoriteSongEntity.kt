package com.krystalshard.lifemelodyapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_songs")
data class FavoriteSongEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val duration: Int,
    val path: String
)