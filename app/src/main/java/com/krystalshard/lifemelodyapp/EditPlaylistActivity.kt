package com.krystalshard.lifemelodyapp

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.krystalshard.lifemelodyapp.databinding.ActivityEditPlaylistBinding
import kotlinx.coroutines.launch
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding


class EditPlaylistActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditPlaylistBinding
    private lateinit var appDatabase: AppDatabase
    private var currentPlaylist: Playlist? = null
    private var selectedIcon: Int = R.drawable.ic_list



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditPlaylistBinding.inflate(layoutInflater)
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

        applyUserThemeColor()

        appDatabase = AppDatabase.getInstance(this)
        val playlistId = intent.getLongExtra("PLAYLIST_ID", -1)

        lifecycleScope.launch {
            currentPlaylist = appDatabase.playlistDao().getPlaylistById(playlistId)
            currentPlaylist?.let {
                binding.etPlaylistNameEdit.setText(it.name)
                selectedIcon = it.iconResId
                binding.imgLargePreview.setImageResource(selectedIcon)
                setupIconGrid()

            }
        }

        binding.toolbarEdit.setNavigationOnClickListener { finish() }
        binding.btnSavePlaylist.setOnClickListener {
            saveChanges()
        }
    }

    private fun setupIconGrid() {
        val icons = listOf(
            R.drawable.ic_list,
            R.drawable.ic_nota2,
            R.drawable.ic_ecg_heart,
            R.drawable.ic_favorite3,
            R.drawable.ic_music3,
            R.drawable.ic_fitness,
            R.drawable.ic_moon,
            R.drawable.ic_awesome,
            R.drawable.ic_music4,
            R.drawable.ic_celebration,
            R.drawable.ic_fire,
            R.drawable.ic_mood_heart,
            R.drawable.ic_a1,
            R.drawable.ic_a2,
            R.drawable.ic_a3,
            R.drawable.ic_a4,


            )

        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val savedColor = sharedPref.getString("theme_color", "#C34B92") ?: "#C34B92"
        val colorInt = android.graphics.Color.parseColor(savedColor)

        binding.rvIconsGrid.layoutManager = GridLayoutManager(this, 4)

        binding.rvIconsGrid.adapter = IconGridAdapter(icons, selectedIcon, colorInt) { icon ->
            selectedIcon = icon
            binding.imgLargePreview.setImageResource(icon)
        }
    }



    private fun saveChanges() {
        val newName = binding.etPlaylistNameEdit.text.toString()
        if (newName.isNotEmpty() && currentPlaylist != null) {
            lifecycleScope.launch {
                currentPlaylist!!.name = newName
                currentPlaylist!!.iconResId = selectedIcon
                appDatabase.playlistDao().updatePlaylist(currentPlaylist!!)
                finish()

            }
        }
    }

    private fun applyUserThemeColor() {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val savedColor = sharedPref.getString("theme_color", "#C34B92") ?: "#C34B92"
        val colorInt = android.graphics.Color.parseColor(savedColor)

        binding.divider.setBackgroundColor(colorInt)

        binding.btnSavePlaylist.backgroundTintList = android.content.res.ColorStateList.valueOf(colorInt)

        val bgDrawable = binding.previewContainer.background
        if (bgDrawable is android.graphics.drawable.GradientDrawable) {
            bgDrawable.setStroke(3.dpToPx(), colorInt)
        }
    }

    private fun Int.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

}