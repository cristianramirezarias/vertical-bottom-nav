package com.krystalshard.lifemelodyapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import android.provider.MediaStore
import androidx.recyclerview.widget.LinearLayoutManager
import com.krystalshard.lifemelodyapp.databinding.FragmentMusicaBinding
import android.net.Uri
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.huawei.hms.ads.AdListener
import com.huawei.hms.ads.AdParam
import com.huawei.hms.ads.HwAds
import com.huawei.hms.ads.nativead.NativeAdLoader
import com.huawei.hms.ads.RequestOptions
import com.huawei.hms.ads.it
import kotlinx.coroutines.launch
import kotlin.collections.ArrayList
import com.facebook.ads.*

interface OnSongLongClickListener {
    fun onSongLongClick(song: Song): Boolean
}

class MusicaFragment : Fragment(), OnSongClickListener, OnSongLongClickListener {

    private lateinit var binding: FragmentMusicaBinding
    private lateinit var appDatabase: AppDatabase
    private val songsList = mutableListOf<Song>()
    private val combinedList = mutableListOf<ListItem>()
    private val HIDDEN_SONGS_KEY = "hidden_songs_ids"
    private var adsPersonalized: Boolean = false
    private var isPermissionRequested = false
    private var isSortedByDate = true

    companion object {
        private const val STORAGE_PERMISSION_CODE = 101
        fun newInstance(adsPersonalized: Boolean): MusicaFragment {
            val fragment = MusicaFragment()
            val args = Bundle()
            args.putBoolean("ADS_PERSONALIZED", adsPersonalized)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            adsPersonalized = it.getBoolean("ADS_PERSONALIZED")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMusicaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        appDatabase = AppDatabase.getInstance(requireContext())
        val sharedPref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        isSortedByDate = sharedPref.getBoolean("sort_by_date", true)

        binding.btnSortSelection.setOnClickListener {
            isSortedByDate = !isSortedByDate
            sharedPref.edit().putBoolean("sort_by_date", isSortedByDate).apply()

            loadSongs(sortByDate = isSortedByDate)

            val toastMsg = if (isSortedByDate) {
                getString(R.string.sort_by_recent)
            } else {
                getString(R.string.sort_by_alpha)
            }
            android.widget.Toast.makeText(requireContext(), toastMsg, android.widget.Toast.LENGTH_SHORT).show()
        }

        checkPermissions()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val adapter = binding.recyclerViewMusica.adapter as? MusicaAdapter
                if (adapter?.isInMultiSelectMode() == true) exitMultiSelectMode() else {
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        })

        binding.btnCancelSelection.setOnClickListener { exitMultiSelectMode() }

        binding.btnDeleteSelected.setOnClickListener {
            val adapter = binding.recyclerViewMusica.adapter as? MusicaAdapter
            val selected = adapter?.getSelectedSongs() ?: emptyList()
            if (selected.isNotEmpty()) removeMultipleSongs(selected)
            exitMultiSelectMode()
        }

        binding.btnAddToPlaylist.setOnClickListener {
            val adapter = binding.recyclerViewMusica.adapter as? MusicaAdapter
            val selected = adapter?.getSelectedSongs() ?: emptyList()
            if (selected.isNotEmpty()) {
                showPlaylistSelector(selected)
            }
        }

        binding.fabFavoritos.setOnClickListener {
            startActivity(Intent(requireContext(), FavoritosActivity::class.java))
        }

        binding.btnNavigateToVideos.setOnClickListener {
            startActivity(Intent(requireContext(), VideosActivity::class.java))
        }
    }

    override fun onSongLongClick(song: Song): Boolean {
        val adapter = binding.recyclerViewMusica.adapter as? MusicaAdapter
        adapter?.setMultiSelectMode(true)
        binding.selectionBar.visibility = View.VISIBLE
        return true
    }

    private fun exitMultiSelectMode() {
        val adapter = binding.recyclerViewMusica.adapter as? MusicaAdapter
        adapter?.setMultiSelectMode(false)
        binding.selectionBar.visibility = View.GONE
        binding.tvSelectionCount.text = getString(R.string.text_select)
    }

    private fun removeMultipleSongs(songs: List<Song>) {
        val sharedPref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val currentHidden = sharedPref.getStringSet(HIDDEN_SONGS_KEY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        songs.forEach { song ->
            songsList.remove(song)
            currentHidden.add(song.id.toString())
        }
        with(sharedPref.edit()) {
            putStringSet(HIDDEN_SONGS_KEY, currentHidden)
            apply()
        }
        insertAdsAndSetupAdapter()
    }

    override fun onResume() {
        super.onResume()
        applyThemeColor()
        (binding.recyclerViewMusica.adapter as? MusicaAdapter)?.actualizarColores()
    }

    private fun checkPermissions() {
        val permisoRequerido = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permisoRequerido) != PackageManager.PERMISSION_GRANTED) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.recyclerViewMusica.visibility = View.GONE
            binding.tvHintLongClick.visibility = View.GONE

            if (!isPermissionRequested) {
                isPermissionRequested = true
                requestPermissions(arrayOf(permisoRequerido), STORAGE_PERMISSION_CODE)
            }

            binding.tvEmptyState.setOnClickListener {
                if (shouldShowRequestPermissionRationale(permisoRequerido)) {
                    requestPermissions(arrayOf(permisoRequerido), STORAGE_PERMISSION_CODE)
                } else {
                    val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package", requireContext().packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }
            }
        } else {
            if (songsList.isEmpty()) {
                loadSongs(sortByDate = isSortedByDate)
            } else {
                binding.tvEmptyState.visibility = View.GONE
                binding.recyclerViewMusica.visibility = View.VISIBLE
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                binding.tvEmptyState.visibility = View.GONE
                binding.recyclerViewMusica.visibility = View.VISIBLE
                loadSongs(sortByDate = isSortedByDate)
            }
        }
    }

    private fun loadSongs(sortByDate: Boolean = true) {
        val uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        val selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0"
        val sortOrder = if (sortByDate) "${MediaStore.Audio.Media.DATE_ADDED} DESC" else null

        val cursor = requireContext().contentResolver.query(uri, projection, selection, null, sortOrder)
        val hiddenSongIds = getHiddenSongIds()

        songsList.clear()
        cursor?.use {
            val idColumnIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumnIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumnIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumnIndex = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idColumnIndex)
                if (id !in hiddenSongIds) {
                    songsList.add(Song(
                        id,
                        it.getString(titleColumnIndex),
                        it.getString(artistColumnIndex),
                        it.getInt(durationColumnIndex),
                        Uri.withAppendedPath(uri, id.toString()).toString()
                    ))
                }
            }
        }

        if (!sortByDate) {
            val collator = java.text.Collator.getInstance(java.util.Locale.getDefault())
            songsList.sortWith(compareBy(collator) { it.title })
        }

        if (songsList.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.recyclerViewMusica.visibility = View.GONE
            binding.tvHintLongClick.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.recyclerViewMusica.visibility = View.VISIBLE
            binding.tvHintLongClick.visibility = View.VISIBLE
            insertAdsAndSetupAdapter()
        }
    }

    private fun actualizarInterfaz() {
        if (songsList.isEmpty()) {
            binding.tvEmptyState.visibility = View.VISIBLE
            binding.recyclerViewMusica.visibility = View.GONE
        } else {
            binding.tvEmptyState.visibility = View.GONE
            binding.recyclerViewMusica.visibility = View.VISIBLE
            insertAdsAndSetupAdapter()
        }
    }

    //AppGallery ID PRUEBA: testy63txaom86 | ID REAL: i8jf6jyetp

    private fun insertAdsAndSetupAdapter() {
        val sharedPref = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentConsent = sharedPref.getBoolean("ads_consent_given", true)

        combinedList.clear()
        songsList.forEach { combinedList.add(ListItem.SongItem(it)) }

        if (binding.recyclerViewMusica.adapter == null) {
            binding.recyclerViewMusica.layoutManager = LinearLayoutManager(requireContext())
            binding.recyclerViewMusica.adapter = MusicaAdapter(combinedList, this, this)
        } else {
            binding.recyclerViewMusica.adapter?.notifyDataSetChanged()
        }

        val requestOptions = RequestOptions.Builder()
            .setNonPersonalizedAd(if (currentConsent)
                com.huawei.hms.ads.NonPersonalizedAd.ALLOW_ALL
            else com.huawei.hms.ads.NonPersonalizedAd.ALLOW_NON_PERSONALIZED)
            .build()
        HwAds.setRequestOptions(requestOptions)

        val adLoader = NativeAdLoader.Builder(requireContext(), "i8jf6jyetp")
            .setNativeAdLoadedListener { nativeAd ->
                if (combinedList.isNotEmpty() && combinedList[0] is ListItem.AdItem) {
                    combinedList.removeAt(0)
                }
                combinedList.add(0, ListItem.AdItem(nativeAd))
                binding.recyclerViewMusica.adapter?.notifyDataSetChanged()
            }
            .setAdListener(object : AdListener() {
                override fun onAdFailed(errorCode: Int) {
                    super.onAdFailed(errorCode)
                    loadMetaNativeAd()
                }
            })
            .build()

        adLoader.loadAd(AdParam.Builder().build())
    }

    private val META_PLACEMENT_ID = "1952918715645443_1952918785645436"

    private fun loadMetaNativeAd() {
        val nativeAd = NativeAd(requireContext(), META_PLACEMENT_ID)
        val nativeAdListener = object : NativeAdListener {
            override fun onMediaDownloaded(ad: Ad) {}
            override fun onError(ad: Ad, adError: AdError) {
                loadAdMobNativeAd()
            }
            override fun onAdLoaded(ad: Ad) {
                if (ad != nativeAd) return
                if (combinedList.isNotEmpty() && combinedList[0] is ListItem.AdItem) {
                    combinedList.removeAt(0)
                }
                combinedList.add(0, ListItem.AdItem(nativeAd))
                binding.recyclerViewMusica.adapter?.notifyDataSetChanged()
            }
            override fun onAdClicked(ad: Ad) {}
            override fun onLoggingImpression(ad: Ad) {}
        }
        nativeAd.loadAd(nativeAd.buildLoadAdConfig().withAdListener(nativeAdListener).build())
    }

    // PRUEBA: ca-app-pub-3940256099942544/2247696110
    // REAL: ca-app-pub-2698591014902582/4778167384
    private fun loadAdMobNativeAd() {
        val sharedPref = requireActivity().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val currentConsent = sharedPref.getBoolean("ads_consent_given", true)

        val adLoader = com.google.android.gms.ads.AdLoader.Builder(requireContext(), "ca-app-pub-2698591014902582/4778167384")
            .forNativeAd { adMobNativeAd ->
                if (combinedList.isNotEmpty() && combinedList[0] is ListItem.AdItem) {
                    combinedList.removeAt(0)
                }
                combinedList.add(0, ListItem.AdItem(adMobNativeAd))
                binding.recyclerViewMusica.adapter?.notifyDataSetChanged()
            }
            .withAdListener(object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(adError: com.google.android.gms.ads.LoadAdError) {
                    Log.e("LifeMelody", "Todas las redes fallaron")
                }
            })
            .build()

        val extras = Bundle()
        if (!currentConsent) {
            extras.putString("npa", "1")
        }

        val adRequest = com.google.android.gms.ads.AdRequest.Builder()
            .addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter::class.java, extras)
            .build()

        adLoader.loadAd(adRequest)
    }

    override fun onSongClick(song: Song) {
        val songIndex = songsList.indexOf(song)
        if (songIndex != -1) {
            val serviceIntent = Intent(requireContext(), MusicService::class.java).apply {
                putExtra("ACTION", "PLAY_MEDIA")
                putExtra("SONGS_LIST", ArrayList(songsList))
                putExtra("MEDIA_INDEX", songIndex)
            }
            requireContext().startService(serviceIntent)
        }
    }

    private fun getHiddenSongIds(): Set<Long> {
        val sharedPref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val idSet = sharedPref.getStringSet(HIDDEN_SONGS_KEY, emptySet()) ?: emptySet()

        return idSet.mapNotNull { it.toLongOrNull() }.toSet()
    }

    private fun addSongToHiddenList(songId: Long) {
        val sharedPref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        val currentHidden = sharedPref.getStringSet(HIDDEN_SONGS_KEY, mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        currentHidden.add(songId.toString())

        with(sharedPref.edit()) {
            putStringSet(HIDDEN_SONGS_KEY, currentHidden)
            apply()
        }
    }

    private fun showPlaylistSelector(selectedSongs: List<Song>) {
        lifecycleScope.launch {
            val playlists = appDatabase.playlistDao().getAllPlaylists()
            val playlistNames = playlists.map { it.name }.toMutableList()
            playlistNames.add(0, "" + getString(R.string.action_create_new_list))

            val sharedPref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val colorInt = android.graphics.Color.parseColor(sharedPref.getString("theme_color", "#C34B92"))

            val dialog = android.app.AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.title_select_playlist))
                .setItems(playlistNames.toTypedArray()) { _, which ->
                    if (which == 0) {
                        showCreatePlaylistDialog(selectedSongs)
                    } else {
                        val targetPlaylist = playlists[which - 1]
                        addSongsToExistingPlaylist(targetPlaylist.id, selectedSongs)
                    }
                }
                .create()

            dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rounded_dialog)

            dialog.setOnShowListener {
                val titleId = resources.getIdentifier("alertTitle", "id", "android")
                dialog.findViewById<android.widget.TextView>(titleId)?.setTextColor(colorInt)
            }

            dialog.show()
        }
    }

    private fun showCreatePlaylistDialog(songs: List<Song>) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_input_field, null)
        val input = dialogView.findViewById<android.widget.EditText>(R.id.etPlaylistName)
        val sharedPref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val colorString = sharedPref.getString("theme_color", "#C34B92")
        val colorInt = android.graphics.Color.parseColor(colorString)
        val colorStateList = ColorStateList.valueOf(colorInt)

        input.backgroundTintList = colorStateList
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            input.textCursorDrawable?.setTint(colorInt)
        }

        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.title_new_playlist))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.action_create), null)
            .setNegativeButton(getString(R.string.action_cancel), null)
            .create()

        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_rounded_dialog)

        dialog.setOnShowListener {

            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(colorInt)
            dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE).setTextColor(android.graphics.Color.GRAY)


            val titleId = resources.getIdentifier("alertTitle", "id", "android")
            dialog.findViewById<android.widget.TextView>(titleId)?.setTextColor(colorInt)

            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    createNewPlaylistWithSongs(name, songs)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }


    private fun createNewPlaylistWithSongs(name: String, songs: List<Song>) {
        lifecycleScope.launch {
            val newId = appDatabase.playlistDao().insertPlaylist(Playlist(name = name))

            songs.forEach { song ->
                appDatabase.playlistDao().insertPlaylistSongInfo(
                    PlaylistSongInfoEntity(
                        id = song.id,
                        title = song.title,
                        artist = song.artist,
                        duration = song.duration,
                        path = song.path
                    )
                )
            }

            val relations = songs.map { PlaylistSong(playlistId = newId, songId = it.id) }
            appDatabase.playlistDao().insertSongsToPlaylist(relations)

            val message = getString(R.string.toast_list_created, name)
            android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_SHORT).show()
            exitMultiSelectMode()

        }
    }

    private fun addSongsToExistingPlaylist(playlistId: Long, songs: List<Song>) {
        lifecycleScope.launch {
            songs.forEach { song ->
                appDatabase.playlistDao().insertPlaylistSongInfo(
                    PlaylistSongInfoEntity(
                        id = song.id,
                        title = song.title,
                        artist = song.artist,
                        duration = song.duration,
                        path = song.path
                    )
                )
            }


            val relations = songs.map { song: Song ->
                PlaylistSong(playlistId = playlistId, songId = song.id)
            }
            appDatabase.playlistDao().insertSongsToPlaylist(relations)

            android.widget.Toast.makeText(requireContext(), getString(R.string.toast_songs_added), android.widget.Toast.LENGTH_SHORT).show()
            exitMultiSelectMode()
        }
    }


    private fun applyThemeColor() {
        val sharedPref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val colorString = sharedPref.getString("theme_color", "#C34B92")
        val themeMode = sharedPref.getString("theme_mode", "solid")

        try {
            val colorInt = android.graphics.Color.parseColor(colorString)
            binding.fabFavoritos.backgroundTintList = ColorStateList.valueOf(colorInt)

            if (themeMode == "gradient") {
                applyGradientBackground(colorInt)
            } else {
                val backgroundColor = if (isDarkTheme()) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                binding.root.setBackgroundColor(backgroundColor)
            }
        } catch (e: java.lang.IllegalArgumentException) {
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