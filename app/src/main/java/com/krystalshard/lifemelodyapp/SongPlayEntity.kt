package com.krystalshard.lifemelodyapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_plays")
data class SongPlayEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val duration: Int,
    val path: String,
    var playCount: Int
)