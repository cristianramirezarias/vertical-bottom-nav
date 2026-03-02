package com.krystalshard.lifemelodyapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlist_song_info")
data class PlaylistSongInfoEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val duration: Int,
    val path: String
)