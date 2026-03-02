package com.krystalshard.lifemelodyapp//APPGALLERY

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.bumptech.glide.Glide
import com.krystalshard.lifemelodyapp.data.VideoItem
import com.krystalshard.lifemelodyapp.databinding.FragmentReproductorBinding
import kotlinx.coroutines.launch
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import androidx.annotation.AttrRes

class ReproductorFragment : Fragment() {

    private lateinit var binding: FragmentReproductorBinding
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private var musicService: MusicService? = null
    private var isBound = false

    private var isPlaying = false
    private var isRepeating = false
    private lateinit var appDatabase: AppDatabase
    private var currentSong: Song? = null
    private var isVideoMode = false
    private var currentVideo: VideoItem? = null
    private var rotationAnimator: android.animation.ObjectAnimator? = null


    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.LocalBinder
            musicService = binder.getService()
            isBound = true
            requestCurrentStatus()

        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            musicService = null
        }
    }

    private fun requestCurrentStatus() {
        val intent = Intent(requireContext(), MusicService::class.java).apply {
            putExtra("ACTION", "REQUEST_STATUS")
        }
        requireContext().startService(intent)
    }

    private fun toggleSmartAnimation(shouldAnimate: Boolean) {
        val sharedPref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val animationsEnabled = sharedPref.getBoolean("animations_enabled", false)
        val photoUriString = sharedPref.getString("photo_uri", null)

        val animationType = if (photoUriString != null) "rotate" else sharedPref.getString("animation_type", "pulse") ?: "pulse"

        val effectivelyAnimate = shouldAnimate && animationsEnabled

        if (isVideoMode) {
            rotationAnimator?.pause()
            return
        }

        if (rotationAnimator == null) {
            val viewToAnimate = binding.frameLayoutPortadaContainer

            rotationAnimator = if (animationType == "rotate") {
                android.animation.ObjectAnimator.ofFloat(viewToAnimate, "rotation", 0f, 360f).apply {
                    duration = 15000
                    repeatCount = android.animation.ObjectAnimator.INFINITE
                    interpolator = android.view.animation.LinearInterpolator()
                }
            } else {
                val scaleX = android.animation.PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.12f)
                val scaleY = android.animation.PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.12f)
                android.animation.ObjectAnimator.ofPropertyValuesHolder(viewToAnimate, scaleX, scaleY).apply {
                    duration = 1000
                    repeatCount = android.animation.ObjectAnimator.INFINITE
                    repeatMode = android.animation.ObjectAnimator.REVERSE
                    interpolator = android.view.animation.AccelerateDecelerateInterpolator()
                }
            }
        }

        if (effectivelyAnimate) {
            if (rotationAnimator?.isStarted == false) {
                rotationAnimator?.start()
            } else {
                rotationAnimator?.resume()
            }
        } else {
            rotationAnimator?.pause()
            if (!animationsEnabled) {
                binding.frameLayoutPortadaContainer.rotation = 0f
                binding.frameLayoutPortadaContainer.scaleX = 1f
                binding.frameLayoutPortadaContainer.scaleY = 1f
            }
        }
    }

    private val musicUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val title = it.getStringExtra("TITLE")
                var artist = it.getStringExtra("ARTIST")
                val duration = it.getIntExtra("DURATION", 0)
                isPlaying = it.getBooleanExtra("IS_PLAYING", false) // Aquí capturamos el estado
                isRepeating = it.getBooleanExtra("IS_REPEATING", false)
                val uri: Uri? = it.getParcelableExtra("MEDIA_URI")
                val newIsVideoMode = it.getBooleanExtra("IS_VIDEO", false)

                if (newIsVideoMode) { artist = "" }

                binding.tituloCancion.text = title
                binding.nombreArtista.text = artist
                binding.barraProgreso.max = duration
                binding.btnPlayPause.setImageResource(
                    if (isPlaying) R.drawable.ic_pause else R.drawable.ic_reproductor
                )

                binding.root.post {
                    toggleSmartAnimation(isPlaying)
                }

                binding.btnRepetir.setImageResource(
                    if (isRepeating) R.drawable.ic_repeat_active else R.drawable.ic_repeat
                )
                binding.tiempoTotal.text = formatDuration(duration)

                val mediaId = it.getLongExtra("MEDIA_ID", -1)
                if (mediaId != -1L && uri != null) {
                    isVideoMode = newIsVideoMode
                    if (isVideoMode) {
                        currentVideo = VideoItem(mediaId, title ?: "", artist ?: "", duration.toLong(), uri.toString())
                        currentSong = null
                    } else {
                        currentSong = Song(mediaId, title ?: "", artist ?: "", duration, uri.toString())
                        currentVideo = null
                        checkFavoriteStatus(mediaId)
                    }
                    updateMediaView(uri)
                }
            }
        }
    }

    private val progressUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val progress = it.getIntExtra("PROGRESS", 0)
                binding.barraProgreso.progress = progress
                binding.tiempoActual.text = formatDuration(progress)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReproductorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        localBroadcastManager = LocalBroadcastManager.getInstance(requireContext())
        appDatabase = AppDatabase.getInstance(requireContext())

        binding.btnConfigMain.setOnClickListener {
            startActivity(Intent(requireContext(), ConfigActivity::class.java))
        }

        binding.btnPlayPause.setOnClickListener {
            val intent = Intent(requireContext(), MusicService::class.java).apply {
                putExtra("ACTION", "PLAY_PAUSE")
            }
            requireContext().startService(intent)
        }

        binding.btnSiguiente.setOnClickListener {
            val intent = Intent(requireContext(), MusicService::class.java).apply {
                putExtra("ACTION", "NEXT")
            }
            requireContext().startService(intent)
        }

        binding.btnAnterior.setOnClickListener {
            val intent = Intent(requireContext(), MusicService::class.java).apply {
                putExtra("ACTION", "PREVIOUS")
            }
            requireContext().startService(intent)
        }

        binding.btnRepetir.setOnClickListener {
            isRepeating = !isRepeating
            val intent = Intent(requireContext(), MusicService::class.java).apply {
                putExtra("ACTION", "TOGGLE_REPEAT")
                putExtra("IS_REPEATING", isRepeating)
            }
            requireContext().startService(intent)
        }

        binding.btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        binding.barraProgreso.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    binding.tiempoActual.text = formatDuration(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let {
                    val intent = Intent(requireContext(), MusicService::class.java).apply {
                        putExtra("ACTION", "SEEK_TO")
                        putExtra("SEEK_POSITION", it.progress)
                    }
                    requireContext().startService(intent)
                }
            }
        })

        binding.cardViewVideoPlayer.setOnClickListener {
            if (isVideoMode && currentVideo != null) {
                val fullscreenIntent = Intent(requireContext(), FullscreenVideoActivity::class.java).apply {
                    putExtra("VIDEO_URI", currentVideo!!.uriString)
                    putExtra("VIDEO_TITLE", currentVideo!!.title)
                }
                startActivity(fullscreenIntent)
            }
        }
    }

    private fun updateMediaView(uri: Uri) {
        if (isVideoMode) {
            binding.frameLayoutPortadaContainer.visibility = View.GONE
            binding.cardViewVideoPlayer.visibility = View.VISIBLE

            if (isBound) {
                musicService?.setPlayerView(binding.videoPlayerView)
            }
            binding.cardViewVideoPlayer.isClickable = true
            binding.cardViewVideoPlayer.isFocusable = true

        } else {
            if (isBound) {
                musicService?.setPlayerView(null)
            }
            binding.cardViewVideoPlayer.visibility = View.GONE
            binding.frameLayoutPortadaContainer.visibility = View.VISIBLE
            binding.cardViewVideoPlayer.isClickable = false
            binding.cardViewVideoPlayer.isFocusable = false

            applySelectedIconOrImage()
        }

        checkFavoriteStatus(currentSong?.id ?: -1L)
    }

    override fun onStart() {
        super.onStart()
        Intent(requireContext(), MusicService::class.java).also { intent ->
            requireContext().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            requireContext().unbindService(serviceConnection)
            isBound = false
            musicService = null
        }
    }

    override fun onResume() {
        super.onResume()
        localBroadcastManager.registerReceiver(musicUpdateReceiver, IntentFilter("com.krystalshard.lifemelodyapp.MUSIC_UPDATE"))
        localBroadcastManager.registerReceiver(progressUpdateReceiver, IntentFilter("com.krystalshard.lifemelodyapp.MUSIC_PROGRESS"))
        localBroadcastManager.registerReceiver(fullscreenExitReceiver, IntentFilter("com.krystalshard.lifemelodyapp.FULLSCREEN_EXIT"))


        val intent = Intent(requireContext(), MusicService::class.java).apply {
            putExtra("ACTION", "REQUEST_STATUS")
        }
        requireContext().startService(intent)
        Handler(Looper.getMainLooper()).post {
            if (isVideoMode && isBound && musicService != null) {
                musicService?.setPlayerView(binding.videoPlayerView)
            }
        }

        applyThemeColor()
        applySelectedIconOrImage()
    }

    override fun onPause() {
        super.onPause()
        localBroadcastManager.unregisterReceiver(musicUpdateReceiver)
        localBroadcastManager.unregisterReceiver(progressUpdateReceiver)
        localBroadcastManager.unregisterReceiver(fullscreenExitReceiver)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (isVideoMode && isBound) {
            musicService?.setPlayerView(null)
        }
    }

    private val fullscreenExitReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val wasPlaying = intent?.getBooleanExtra("WAS_PLAYING", false) ?: false
            if (wasPlaying) {
                val resumeIntent = Intent(requireContext(), MusicService::class.java).apply {
                    putExtra("ACTION", "PLAY_PAUSE")
                }
                requireContext().startService(resumeIntent)
            }
        }
    }

    fun Context.getThemeColor(@AttrRes attrColor: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attrColor, typedValue, true)
        return typedValue.data
    }

    private fun isDarkTheme(): Boolean {
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    private fun applyThemeColor() {
        val sharedPref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val colorString = sharedPref.getString("theme_color", "#C34B92")
        val themeMode = sharedPref.getString("theme_mode", SettingsActivity.THEME_SOLID)

        try {
            val colorInt = Color.parseColor(colorString)
            val isGradientMode = themeMode == SettingsActivity.THEME_GRADIENT
            val SHADOW_RADIUS = 15f
            val NO_SHADOW = 0f

            if (isGradientMode) {
                binding.tituloCancion.setShadowLayer(SHADOW_RADIUS, 0f, 0f, colorInt)
                binding.nombreArtista.setShadowLayer(SHADOW_RADIUS, 0f, 0f, colorInt)
            } else {
                binding.tituloCancion.setShadowLayer(NO_SHADOW, 0f, 0f, 0)
                binding.nombreArtista.setShadowLayer(NO_SHADOW, 0f, 0f, 0)
            }

            val isRtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

            if (isRtl) {

                binding.btnSiguiente.setImageResource(R.drawable.ic_previus)
                binding.btnAnterior.setImageResource(R.drawable.ic_next)
            } else {

                binding.btnSiguiente.setImageResource(R.drawable.ic_next)
                binding.btnAnterior.setImageResource(R.drawable.ic_previus)
            }

            binding.tiempoActual.setShadowLayer(NO_SHADOW, 0f, 0f, 0)
            binding.tiempoTotal.setShadowLayer(NO_SHADOW, 0f, 0f, 0)
            binding.btnSiguiente.setColorFilter(colorInt)
            binding.btnAnterior.setColorFilter(colorInt)
            binding.btnRepetir.setColorFilter(colorInt)
            binding.btnFavorite.setColorFilter(colorInt)
            binding.barraProgreso.progressTintList = ColorStateList.valueOf(colorInt)
            binding.barraProgreso.thumbTintList = ColorStateList.valueOf(Color.TRANSPARENT)
            binding.btnPlayPause.setColorFilter(colorInt)

            val circleBackgroundColor = if (isDarkTheme()) Color.WHITE else Color.BLACK
            val circleDrawable = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(circleBackgroundColor)
            }

            if (themeMode == SettingsActivity.THEME_GRADIENT) {
                applyGradientBackground(colorInt)
                binding.imagenPortadaIcono.background = circleDrawable
                binding.cardViewPortada.setCardBackgroundColor(Color.TRANSPARENT)
                binding.imagenPortadaIcono.setColorFilter(if (isDarkTheme()) Color.BLACK else Color.WHITE)
            } else {
                val solidBackgroundColor = requireContext().getThemeColor(R.attr.colorSurfaceContainer)
                binding.reproductorLayoutRoot.setBackgroundColor(solidBackgroundColor)
                binding.imagenPortadaIcono.background = circleDrawable
                binding.imagenPortadaIcono.setColorFilter(colorInt)
                binding.cardViewPortada.setCardBackgroundColor(Color.TRANSPARENT)
            }

        } catch (e: IllegalArgumentException) {
        }
        checkFavoriteStatus(currentSong?.id ?: -1L)
    }

    private fun applySelectedIconOrImage() {
        rotationAnimator?.end()
        rotationAnimator = null
        binding.frameLayoutPortadaContainer.rotation = 0f
        binding.frameLayoutPortadaContainer.scaleX = 1f
        binding.frameLayoutPortadaContainer.scaleY = 1f

        val sharedPref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val iconResId = sharedPref.getInt("icon_res_id", R.drawable.ic_note)
        val photoUriString = sharedPref.getString("photo_uri", null)

        binding.cardViewPortada.visibility = View.VISIBLE

        if (photoUriString != null) {
            binding.imagenPortadaIcono.visibility = View.GONE
            binding.imagenPortadaFoto.visibility = View.VISIBLE
            binding.imagenPortadaIcono.background = null
            binding.imagenPortadaIcono.setPadding(0, 0, 0, 0)

            val photoUri = Uri.parse(photoUriString)
            Glide.with(this)
                .load(photoUri)
                .circleCrop()
                .placeholder(R.drawable.ic_note)
                .into(binding.imagenPortadaFoto)
        } else {
            binding.imagenPortadaFoto.visibility = View.GONE
            binding.imagenPortadaIcono.visibility = View.VISIBLE
            val resourceToSet = if (iconResId != -1) iconResId else R.drawable.ic_note
            binding.imagenPortadaIcono.setImageResource(resourceToSet)

            val paddingInDp = 30
            val paddingInPx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                paddingInDp.toFloat(),
                resources.displayMetrics
            ).toInt()
            binding.imagenPortadaIcono.setPadding(paddingInPx, paddingInPx, paddingInPx, paddingInPx)

            applyThemeColor()
        }
    }


    private fun applyGradientBackground(themeColor: Int) {
        val backgroundColor = if (isDarkTheme()) Color.BLACK else Color.WHITE

        val colors = intArrayOf(
            themeColor,
            backgroundColor,
            backgroundColor
        )

        val gradientDrawable = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            colors
        )

        gradientDrawable.gradientType = GradientDrawable.LINEAR_GRADIENT

        binding.reproductorLayoutRoot.background = gradientDrawable
    }


    private fun checkFavoriteStatus(songId: Long) {
        if (isVideoMode) {
            binding.btnFavorite.setImageResource(R.drawable.ic_heart)
            binding.btnFavorite.clearColorFilter()
            return
        }

        val sharedPref = requireActivity().getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val colorString = sharedPref.getString("theme_color", "#C34B92")

        val selectedColor = try {
            Color.parseColor(colorString)
        } catch (e: IllegalArgumentException) {
            Color.parseColor("#C34B92")
        }

        lifecycleScope.launch {
            val isFavorite = appDatabase.favoriteSongDao().isFavorite(songId)
            if (isFavorite) {
                binding.btnFavorite.setImageResource(R.drawable.ic_heart_filled)
                binding.btnFavorite.setColorFilter(selectedColor)
            } else {
                binding.btnFavorite.setImageResource(R.drawable.ic_heart)
                binding.btnFavorite.clearColorFilter()
            }
        }
    }

    private fun toggleFavorite() {

        if (isVideoMode || currentSong == null) {
            Toast.makeText(requireContext(), requireContext().getString(R.string.toast_favorite_video_restriction), Toast.LENGTH_SHORT).show()
            return
        }

        if (currentSong != null) {
            lifecycleScope.launch {
                val isFavorite = appDatabase.favoriteSongDao().isFavorite(currentSong!!.id)
                if (isFavorite) {
                    appDatabase.favoriteSongDao().delete(
                        FavoriteSongEntity(
                            id = currentSong!!.id,
                            title = currentSong!!.title,
                            artist = currentSong!!.artist,
                            duration = currentSong!!.duration,
                            path = currentSong!!.path
                        )
                    )
                } else {
                    appDatabase.favoriteSongDao().insert(
                        FavoriteSongEntity(
                            id = currentSong!!.id,
                            title = currentSong!!.title,
                            artist = currentSong!!.artist,
                            duration = currentSong!!.duration,
                            path = currentSong!!.path
                        )
                    )
                }
                checkFavoriteStatus(currentSong!!.id)
            }
        }
    }

    private fun formatDuration(duration: Int): String {
        val minutes = (duration / 1000) / 60
        val seconds = (duration / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}