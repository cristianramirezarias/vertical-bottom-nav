package com.krystalshard.lifemelodyapp

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.krystalshard.lifemelodyapp.data.VideoItem
import com.krystalshard.lifemelodyapp.databinding.ActivityVideosBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.krystalshard.lifemelodyapp.R
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class VideosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVideosBinding
    private val REQUEST_CODE_ADD_VIDEOS = 1001

    private lateinit var videoAdapter: VideoAdapter
    private var videoList: MutableList<VideoItem> = mutableListOf()

    private val PREFS_NAME = "VideoPrefs"
    private val VIDEO_LIST_KEY = "video_list"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityVideosBinding.inflate(layoutInflater)
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

        stopMusicService()

        loadVideoListFromPrefs()

        videoAdapter = VideoAdapter(videoList, clickListener = { video ->
            val index = videoList.indexOf(video)
            if (index != -1) {
                val intent = Intent(this, MusicService::class.java).apply {
                    putExtra("ACTION", "PLAY_MEDIA")
                    putParcelableArrayListExtra("VIDEOS_LIST", ArrayList(videoList))
                    putExtra("MEDIA_INDEX", index)
                }
                startService(intent)
            }
        }, activity = this)

        binding.btnCancelSelection.setOnClickListener { exitMultiSelectMode() }

        binding.btnDeleteSelected.setOnClickListener {
            val selected = videoAdapter.getSelectedVideos()
            if (selected.isNotEmpty()) {
                removeMultipleVideos(selected)
                exitMultiSelectMode()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (videoAdapter.isInMultiSelectMode()) {
                    exitMultiSelectMode()
                } else {
                    isEnabled = false
                    onBackPressed()
                }
            }
        })

        binding.recyclerViewVideos.apply {
            layoutManager = LinearLayoutManager(this@VideosActivity)
            adapter = videoAdapter
        }

        binding.btnAddVideo.setOnClickListener {
            openVideoSelection()
        }

        applyThemeColorToButton()
        updateEmptyViewVisibility()
    }


    fun startMultiSelectMode(video: VideoItem) {
        binding.selectionBar.visibility = View.VISIBLE
        videoAdapter.selectInitialVideo(video)
    }

    fun updateSelectionCount(count: Int) {
        if (count > 0) {
            binding.tvSelectionCount.text = resources.getQuantityString(
                R.plurals.songs_selected_count,
                count,
                count
            )
        } else {
            binding.tvSelectionCount.text = getString(R.string.text_select)
        }
    }

    private fun exitMultiSelectMode() {
        videoAdapter.setMultiSelectMode(false)
        binding.selectionBar.visibility = View.GONE
        binding.tvSelectionCount.text = getString(R.string.text_select)
    }

    private fun removeMultipleVideos(videos: List<VideoItem>) {
        videos.forEach { video ->
            videoList.remove(video)
            try {
                contentResolver.releasePersistableUriPermission(video.getUri(), Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                Log.e("VideosActivity", "Error al liberar URI: ${video.uriString}")
            }
        }
        saveVideoList()
        videoAdapter.updateData(videoList)
        updateEmptyViewVisibility()
    }

    private fun stopMusicService() {
        val stopIntent = Intent(this, MusicService::class.java).apply {
            putExtra("ACTION", "STOP_SERVICE")
        }
        startService(stopIntent)
    }

    private fun applyThemeColorToButton() {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val colorString = sharedPref.getString("theme_color", "#C34B92")

        try {
            val colorInt = Color.parseColor(colorString)
            binding.btnAddVideo.setColorFilter(colorInt)

        } catch (e: IllegalArgumentException) {
            val defaultColor = Color.parseColor("#C34B92")
            binding.btnAddVideo.setColorFilter(defaultColor)
        }
    }

    private fun openVideoSelection() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "video/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(intent, REQUEST_CODE_ADD_VIDEOS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_ADD_VIDEOS && resultCode == Activity.RESULT_OK) {
            data?.let {
                val uriList = mutableListOf<Uri>()
                if (it.data != null) {
                    uriList.add(it.data!!)
                }
                if (it.clipData != null) {
                    for (i in 0 until it.clipData!!.itemCount) {
                        uriList.add(it.clipData!!.getItemAt(i).uri)
                    }
                }
                uriList.forEach { uri -> handleVideoUri(uri) }
            }
        }
    }

    private fun handleVideoUri(uri: Uri) {
        val contentResolver: ContentResolver = contentResolver
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION

        try {
            contentResolver.takePersistableUriPermission(uri, takeFlags)

            var title = "Título Desconocido"
            var artist = "Archivo Local"
            var duration = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (displayNameIndex != -1) {
                        title = cursor.getString(displayNameIndex)
                        if (title.contains('.')) {
                            title = title.substring(0, title.lastIndexOf('.'))
                        }
                    }
                }
            }

            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(this, uri)
                val durationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                duration = durationString?.toLongOrNull() ?: 0L
            } catch (e: Exception) {
            } finally {
                retriever.release()
            }

            val newVideo = VideoItem(
                id = System.currentTimeMillis(),
                title = title,
                artist = artist,
                durationMs = duration,
                uriString = uri.toString()
            )

            videoList.add(newVideo)
            videoAdapter.updateData(videoList)
            saveVideoList()
            updateEmptyViewVisibility()

        } catch (e: SecurityException) {
            Log.e("VideosActivity", "Error al solicitar permiso de URI persistente", e)
        }
    }

    private fun saveVideoList() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = gson.toJson(videoList)

        with(prefs.edit()) {
            putString(VIDEO_LIST_KEY, json)
            apply()
        }
    }

    private fun loadVideoListFromPrefs() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(VIDEO_LIST_KEY, null)

        if (json != null) {
            val gson = Gson()
            val type = object : TypeToken<MutableList<VideoItem>>() {}.type
            videoList = gson.fromJson(json, type) ?: mutableListOf()
        } else {
            videoList = mutableListOf()
        }
    }

    private fun updateEmptyViewVisibility() {
        if (videoList.isEmpty()) {
            binding.textViewEmpty.visibility = View.VISIBLE
            binding.recyclerViewVideos.visibility = View.GONE
        } else {
            binding.textViewEmpty.visibility = View.GONE
            binding.recyclerViewVideos.visibility = View.VISIBLE
        }
    }
}