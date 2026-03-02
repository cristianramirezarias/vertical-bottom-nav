package com.krystalshard.lifemelodyapp

import androidx.room.*

@Dao
interface PlaylistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: Playlist): Long

    @Update
    suspend fun updatePlaylist(playlist: Playlist)

    @Query("SELECT * FROM playlists ORDER BY name ASC")
    suspend fun getAllPlaylists(): List<Playlist>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): Playlist?

    @Delete
    suspend fun deletePlaylist(playlist: Playlist)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSongsToPlaylist(playlistSongs: List<PlaylistSong>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongInfo(song: PlaylistSongInfoEntity)

    @Query("""
        SELECT playlist_song_info.* FROM playlist_song_info 
        INNER JOIN playlist_songs ON playlist_song_info.id = playlist_songs.songId 
        WHERE playlist_songs.playlistId = :playlistId
    """)
    suspend fun getSongsFromPlaylist(playlistId: Long): List<PlaylistSongInfoEntity>

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
}