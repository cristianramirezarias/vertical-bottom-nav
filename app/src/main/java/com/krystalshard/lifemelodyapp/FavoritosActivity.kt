package com.krystalshard.lifemelodyapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.krystalshard.lifemelodyapp.databinding.ActivityFavoritosBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class FavoritosActivity : AppCompatActivity(), OnSongClickListener, FavoritosAdapter.OnFavoriteRemoveListener {

    private lateinit var binding: ActivityFavoritosBinding
    private lateinit var adapter: FavoritosAdapter
    private val favoritosViewModel: FavoritosViewModel by viewModels()
    private lateinit var appDatabase: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityFavoritosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        appDatabase = AppDatabase.getInstance(this)
        adapter = FavoritosAdapter(emptyList(), this, this, this)
        binding.recyclerViewFavoritos.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewFavoritos.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                favoritosViewModel.allFavorites.collect { favoriteSongs ->
                    val songs = favoriteSongs.map { entity ->
                        Song(entity.id, entity.title, entity.artist, entity.duration, entity.path)
                    }

                    if (songs.isEmpty()) {
                        binding.recyclerViewFavoritos.visibility = android.view.View.GONE
                        binding.tvEmptyFavorites.visibility = android.view.View.VISIBLE
                    } else {
                        binding.recyclerViewFavoritos.visibility = android.view.View.VISIBLE
                        binding.tvEmptyFavorites.visibility = android.view.View.GONE
                        adapter.updateData(songs)
                    }
                }
            }
        }
    }

    override fun onSongClick(song: Song) {
        val songsList = adapter.getSongsList()
        val songIndex = songsList.indexOf(song)
        if (songIndex != -1) {
            val serviceIntent = Intent(this, MusicService::class.java).apply {
                putExtra("ACTION", "PLAY_NEW_SONG")
                putExtra("SONGS_LIST", ArrayList(songsList))
                putExtra("SONG_INDEX", songIndex)
            }
            startService(serviceIntent)
        }
    }

    override fun onRemoveFavorite(song: Song) {
        val favoriteEntity = FavoriteSongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            duration = song.duration,
            path = song.path
        )
        CoroutineScope(Dispatchers.IO).launch {
            appDatabase.favoriteSongDao().delete(favoriteEntity)
        }
    }
}