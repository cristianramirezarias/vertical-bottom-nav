package com.krystalshard.lifemelodyapp//appgallery

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.krystalshard.lifemelodyapp.databinding.FragmentPlaylistsBinding
import kotlinx.coroutines.launch

class PlaylistsFragment : Fragment() {

    private var _binding: FragmentPlaylistsBinding? = null
    private val binding get() = _binding!!

    private lateinit var appDatabase: AppDatabase
    private lateinit var adapter: PlaylistAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPlaylistsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appDatabase = AppDatabase.getInstance(requireContext())

        setupRecyclerView()
        setupSelectionLogic()
        setupBackPressed()
        loadPlaylists()
    }

    override fun onResume() {
        super.onResume()
        loadPlaylists()
        applyThemeColor()
    }

    private fun setupRecyclerView() {
        adapter = PlaylistAdapter(
            emptyList(),
            onClick = { playlist ->
                if (adapter.isInMultiSelectMode()) {
                    toggleSelection(playlist)
                } else {
                    val intent = Intent(requireContext(), PlaylistDetailActivity::class.java).apply {
                        putExtra("PLAYLIST_ID", playlist.id)
                        putExtra("PLAYLIST_NAME", playlist.name)
                    }
                    startActivity(intent)
                }
            },
            onLongClick = { playlist ->
                if (!adapter.isInMultiSelectMode()) {
                    adapter.setMultiSelectMode(true)
                    binding.selectionBarPlaylists.visibility = View.VISIBLE
                    toggleSelection(playlist)
                    true
                } else false
            }
        )

        binding.rvPlaylists.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvPlaylists.adapter = adapter
    }

    private fun setupSelectionLogic() {
        binding.btnCancelSelectionPlaylists.setOnClickListener { exitMultiSelectMode() }

        binding.btnDeleteSelectedPlaylists.setOnClickListener {
            val selected = adapter.getSelectedPlaylists()
            if (selected.isNotEmpty()) {
                deleteMultiplePlaylists(selected)
            }
        }

        binding.btnEditPlaylist.setOnClickListener {
            val selected = adapter.getSelectedPlaylists()
            if (selected.size == 1) {
                val intent = Intent(requireContext(), EditPlaylistActivity::class.java).apply {
                    putExtra("PLAYLIST_ID", selected[0].id)
                }
                exitMultiSelectMode()
                startActivity(intent)
            }
        }
    }

    private fun setupBackPressed() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (adapter.isInMultiSelectMode()) {
                    exitMultiSelectMode()
                } else {
                    isEnabled = false
                    requireActivity().onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun toggleSelection(playlist: Playlist) {
        adapter.toggleSelection(playlist)
        val count = adapter.getSelectedCount()
        binding.btnEditPlaylist.visibility = if (count == 1) View.VISIBLE else View.GONE
        binding.tvSelectionCountPlaylists.text = if (count > 0) count.toString() else ""
    }

    private fun exitMultiSelectMode() {
        adapter.setMultiSelectMode(false)
        binding.selectionBarPlaylists.visibility = View.GONE
    }

    fun loadPlaylists() {
        viewLifecycleOwner.lifecycleScope.launch {
            // Asegúrate de usar context de forma segura
            val context = context ?: return@launch
            val list = appDatabase.playlistDao().getAllPlaylists()
            if (list.isEmpty()) {
                binding.tvEmptyPlaylists.visibility = View.VISIBLE
                binding.rvPlaylists.visibility = View.GONE
                binding.tvHintLongClick.visibility = View.GONE
            } else {
                binding.tvEmptyPlaylists.visibility = View.GONE
                binding.rvPlaylists.visibility = View.VISIBLE
                binding.tvHintLongClick.visibility = View.VISIBLE
                adapter.updateData(list)
            }
        }
    }

    private fun deleteMultiplePlaylists(playlists: List<Playlist>) {
        viewLifecycleOwner.lifecycleScope.launch {
            playlists.forEach { appDatabase.playlistDao().deletePlaylist(it) }
            exitMultiSelectMode()
            loadPlaylists()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun applyThemeColor() {
        val sharedPref = requireActivity().getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
        val colorString = sharedPref.getString("theme_color", "#C34B92")
        val themeMode = sharedPref.getString("theme_mode", "solid")

        val colorInt = try {
            android.graphics.Color.parseColor(colorString)
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#C34B92")
        }

        if (themeMode == "gradient") {
            applyGradientBackground(colorInt)
        } else {
            val backgroundColor = if (isDarkTheme()) android.graphics.Color.BLACK else android.graphics.Color.WHITE
            binding.root.setBackgroundColor(backgroundColor)
        }
    }

    private fun applyGradientBackground(themeColor: Int) {
        val backgroundColor = if (isDarkTheme()) android.graphics.Color.BLACK else android.graphics.Color.WHITE

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
}