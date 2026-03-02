package com.krystalshard.lifemelodyapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.krystalshard.lifemelodyapp.databinding.FragmentTopBinding
import kotlinx.coroutines.launch
import java.util.ArrayList

private const val TAG = "TopFragment"

class TopFragment : Fragment() {

    private lateinit var binding: FragmentTopBinding
    private lateinit var appDatabase: AppDatabase
    private lateinit var songAdapter: SongAdapter
    private var currentTopSongs: List<Song> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTopBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appDatabase = AppDatabase.getInstance(requireContext())

        val defaultColor = Color.parseColor("#C34B92")

        songAdapter = SongAdapter(emptyList(), object : OnSongClickListener {
            override fun onSongClick(song: Song) {
                val index = currentTopSongs.indexOf(song)
                if (index != -1) {
                    startMusicService(index)
                }
            }
        }, defaultColor)

        binding.recyclerViewTop.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTop.adapter = songAdapter
    }

    override fun onResume() {
        super.onResume()
        refreshData()
    }

    fun refreshData() {
        loadTopSongs()
        applyThemeColor()
    }

    private fun loadTopSongs() {
        lifecycleScope.launch {
            try {
                val topSongs = appDatabase.songPlayDao().getTop10Songs()

                if (topSongs.isEmpty()) {
                    binding.recyclerViewTop.visibility = View.GONE
                    binding.emptyTextView.visibility = View.VISIBLE
                    currentTopSongs = emptyList()
                    songAdapter.updateSongs(emptyList())
                } else {
                    binding.recyclerViewTop.visibility = View.VISIBLE
                    binding.emptyTextView.visibility = View.GONE

                    val topSongObjects = topSongs.map {
                        Song(it.id, it.title, it.artist, it.duration, it.path)
                    }
                    currentTopSongs = topSongObjects
                    songAdapter.updateSongs(topSongObjects)
                }
            } catch (e: Exception) {
                binding.recyclerViewTop.visibility = View.GONE
                binding.emptyTextView.visibility = View.VISIBLE
            }
        }
    }

    private fun applyThemeColor() {
        val sharedPref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val colorString = sharedPref.getString("theme_color", "#C34B92")
        val themeMode = sharedPref.getString("theme_mode", "solid")

        val colorInt = try {
            Color.parseColor(colorString)
        } catch (e: IllegalArgumentException) {
            Color.parseColor("#C34B92")
        }

        songAdapter.updateThemeColor(colorInt)

        if (themeMode == "gradient") {
            applyGradientBackground(colorInt)
        } else {
            val backgroundColor = if (isDarkTheme()) Color.BLACK else Color.WHITE
            binding.root.setBackgroundColor(backgroundColor)
        }
    }

    private fun applyGradientBackground(themeColor: Int) {
        val backgroundColor = if (isDarkTheme()) Color.BLACK else Color.WHITE

        val colors = intArrayOf(
            themeColor,
            backgroundColor,
            backgroundColor
        )

        val gradientDrawable = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.TL_BR,
            colors
        )

        binding.root.background = gradientDrawable
    }

    private fun isDarkTheme(): Boolean {
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }
    private fun startMusicService(startIndex: Int) {
        if (currentTopSongs.isEmpty()) return

        val intent = Intent(requireContext(), MusicService::class.java).apply {
            putExtra("ACTION", "PLAY_MEDIA")
            putParcelableArrayListExtra("SONGS_LIST", ArrayList(currentTopSongs))
            putExtra("MEDIA_INDEX", startIndex)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            requireContext().startForegroundService(intent)
        } else {
            requireContext().startService(intent)
        }
    }

}