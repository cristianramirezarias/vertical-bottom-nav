package com.krystalshard.lifemelodyapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.krystalshard.lifemelodyapp.databinding.ActivityPlaylistDetailBinding
import kotlinx.coroutines.launch
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class PlaylistDetailActivity : AppCompatActivity(), OnSongClickListener, OnSongLongClickListener {

    private lateinit var binding: ActivityPlaylistDetailBinding
    private lateinit var appDatabase: AppDatabase
    private var songsList = mutableListOf<Song>()
    private var playlistId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityPlaylistDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                top = insets.top,
                bottom = insets.bottom,
                left = insets.left,
                right = insets.right
            )
            windowInsets
        }

        appDatabase = AppDatabase.getInstance(this)
        playlistId = intent.getLongExtra("PLAYLIST_ID", -1)
        val playlistName = intent.getStringExtra("PLAYLIST_NAME") ?: "Playlist"

        binding.toolbarDetail.title = playlistName
        binding.toolbarDetail.setNavigationOnClickListener { finish() }

        binding.btnCancelSelectionDetail.setOnClickListener { exitMultiSelectMode() }

        binding.btnRemoveFromPlaylist.setOnClickListener {
            val adapter = binding.rvPlaylistSongs.adapter as? MusicaAdapter
            val selected = adapter?.getSelectedSongs() ?: emptyList()
            if (selected.isNotEmpty()) {
                removeSongsFromPlaylist(selected)
            }
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val adapter = binding.rvPlaylistSongs.adapter as? MusicaAdapter
                if (adapter?.isInMultiSelectMode() == true) {
                    exitMultiSelectMode()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        binding.rvPlaylistSongs.layoutManager = LinearLayoutManager(this)
        loadPlaylistSongs(playlistId)
    }

    override fun onSongLongClick(song: Song): Boolean {
        val adapter = binding.rvPlaylistSongs.adapter as? MusicaAdapter
        adapter?.setMultiSelectMode(true)
        binding.selectionBarDetail.visibility = View.VISIBLE
        return true
    }

    private fun exitMultiSelectMode() {
        val adapter = binding.rvPlaylistSongs.adapter as? MusicaAdapter
        adapter?.setMultiSelectMode(false)
        binding.selectionBarDetail.visibility = View.GONE
    }

    private fun removeSongsFromPlaylist(selectedSongs: List<Song>) {
        lifecycleScope.launch {
            selectedSongs.forEach { song ->
                appDatabase.playlistDao().removeSongFromPlaylist(playlistId, song.id)
            }
            exitMultiSelectMode()
            loadPlaylistSongs(playlistId)
        }
    }

    private fun loadPlaylistSongs(id: Long) {
        lifecycleScope.launch {
            val entities = appDatabase.playlistDao().getSongsFromPlaylist(id)
            android.util.Log.d("DEBUG_PLAYLIST", "Canciones encontradas para ID $id: ${entities.size}")

            songsList = entities.map { entity ->
                Song(
                    id = entity.id,
                    title = entity.title,
                    artist = entity.artist,
                    duration = entity.duration,
                    path = entity.path
                )
            }.toMutableList()

            if (songsList.isEmpty()) {
                binding.tvEmptyPlaylist.visibility = View.VISIBLE
                binding.rvPlaylistSongs.visibility = View.GONE
            } else {
                binding.tvEmptyPlaylist.visibility = View.GONE
                binding.rvPlaylistSongs.visibility = View.VISIBLE

                binding.rvPlaylistSongs.adapter = MusicaAdapter(
                    songsList.map { ListItem.SongItem(it) },
                    this@PlaylistDetailActivity,
                    this@PlaylistDetailActivity
                )
            }
        }
    }

    override fun onSongClick(song: Song) {
        val songIndex = songsList.indexOf(song)
        if (songIndex != -1) {
            val intent = Intent(this, MusicService::class.java).apply {
                putExtra("ACTION", "PLAY_MEDIA")
                putExtra("SONGS_LIST", ArrayList(songsList))
                putExtra("MEDIA_INDEX", songIndex)
            }
            startService(intent)
        }
    }
}