package com.krystalshard.lifemelodyapp//play store

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Parcelable
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.launch
import android.util.Log
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothA2dp
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.audiofx.Equalizer
import com.krystalshard.lifemelodyapp.data.VideoItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.ui.PlayerView
import android.view.SurfaceView
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi

class MusicService : LifecycleService(), AudioManager.OnAudioFocusChangeListener {

    private val TAG = "MusicService"

    private val binder = LocalBinder()

    inner class LocalBinder : android.os.Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }
    private lateinit var exoPlayer: ExoPlayer
    private var mediaList: List<Any> = listOf()
    private var currentMediaIndex: Int = -1
    private var isPrepared: Boolean = false
    private var isRepeating: Boolean = false
    private var resumeOnAudioFocusGain = false
    private var equalizer: Equalizer? = null
    private lateinit var localBroadcastManager: LocalBroadcastManager
    private lateinit var appDatabase: AppDatabase
    private lateinit var audioManager: AudioManager
    private val handler = Handler()
    private var sleepTimerEndTime: Long = 0
    private var isVideoMode: Boolean = false
    private var attachedPlayerView: PlayerView? = null

    private val stopPlaybackRunnable = Runnable {
        stopPlayback()
        sleepTimerEndTime = 0
        sendUpdateBroadcast()
    }

    private val updateProgressAction = object : Runnable {
        override fun run() {
            if (exoPlayer.isPlaying) {
                val intent = Intent("com.krystalshard.lifemelodyapp.MUSIC_PROGRESS").apply {
                    putExtra("PROGRESS", exoPlayer.currentPosition.toInt())
                }
                localBroadcastManager.sendBroadcast(intent)
            }
            handler.postDelayed(this, 1000)
        }
    }

    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.action?.let { action ->
                when (action) {
                    Intent.ACTION_HEADSET_PLUG -> {
                        val state = intent.getIntExtra("state", -1)
                        if (state == 0 && exoPlayer.isPlaying) {
                            handlePlayPause()
                        }
                    }
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_DISCONNECTED)
                        if (state == BluetoothA2dp.STATE_DISCONNECTED && exoPlayer.isPlaying) {
                            handlePlayPause()
                        }
                    }
                    BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED)
                        val prevState = intent.getIntExtra(BluetoothAdapter.EXTRA_PREVIOUS_CONNECTION_STATE, BluetoothAdapter.STATE_DISCONNECTED)
                        if (state == BluetoothAdapter.STATE_DISCONNECTED && prevState == BluetoothAdapter.STATE_CONNECTED && exoPlayer.isPlaying) {
                            handlePlayPause()
                        }
                    }
                }
            }
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == ExoPlayer.STATE_READY) {
                if (!isPrepared) {
                    isPrepared = true
                    setupEqualizer()
                    if (!exoPlayer.isPlaying) {
                        exoPlayer.play()
                    }

                    sendUpdateBroadcast()
                    startForeground(NOTIFICATION_ID, buildNotification())
                    handler.post(updateProgressAction)
                }
            }
            if (playbackState == ExoPlayer.STATE_ENDED) {
                if (isRepeating) {
                    exoPlayer.seekTo(0)
                } else {
                    handleNext()
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            sendUpdateBroadcast()
            updateNotification()
        }

        @OptIn(UnstableApi::class)
        override fun onMediaItemTransition(mediaItem: androidx.media3.common.MediaItem?, @Player.MediaItemTransitionReason reason: Int) {
            if (exoPlayer.currentMediaItemIndex != -1) {

                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    lifecycleScope.launch {
                        saveMediaPlayToDatabase()
                    }
                }

                currentMediaIndex = exoPlayer.currentMediaItemIndex
                updateMediaMode()
                sendUpdateBroadcast()
                updateNotification()

                if (isVideoMode && attachedPlayerView != null) {
                    exoPlayer.setVideoSurfaceView(attachedPlayerView?.videoSurfaceView as? SurfaceView)
                }
            }
        }

        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e(TAG, "ExoPlayer Error: ${error.message}")
        }
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "MusicPlayerChannel_V2"
        const val ACTION_STOP_SERVICE = "com.krystalshard.lifemelodyapp.STOP_SERVICE"
        const val ACTION_KILL_APP = "com.krystalshard.lifemelodyapp.KILL_APP"
        const val ACTION_SET_SLEEP_TIMER = "com.krystalshard.lifemelodyapp.SET_SLEEP_TIMER"
        const val ACTION_ATTACH_VIDEO_VIEW = "com.krystalshard.lifemelodyapp.ATTACH_VIDEO_VIEW"
    }

    override fun onCreate() {
        super.onCreate()
        exoPlayer = ExoPlayer.Builder(this).build()
        exoPlayer.addListener(playerListener)
        exoPlayer.repeatMode = ExoPlayer.REPEAT_MODE_OFF

        exoPlayer.setVideoSurfaceView(null)

        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        appDatabase = AppDatabase.getInstance(this)
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        createNotificationChannel()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
        }
        registerReceiver(headsetReceiver, filter)
    }

    @OptIn(UnstableApi::class)
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val action = intent?.getStringExtra("ACTION") ?: intent?.action
        when (action) {
            "PLAY_MEDIA" -> {
                val songList: ArrayList<Song>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableArrayListExtra("SONGS_LIST", Song::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent?.getParcelableArrayListExtra<Parcelable>("SONGS_LIST") as? ArrayList<Song>
                }

                val videoList: ArrayList<VideoItem>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableArrayListExtra("VIDEOS_LIST", VideoItem::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent?.getParcelableArrayListExtra<Parcelable>("VIDEOS_LIST") as? ArrayList<VideoItem>
                }

                val mediaIndex = intent?.getIntExtra("MEDIA_INDEX", -1) ?: -1

                if (songList != null && mediaIndex != -1) {
                    mediaList = songList
                    currentMediaIndex = mediaIndex
                    updatePlayerList()
                    requestAudioFocus()
                    handlePlayPause(true)
                } else if (videoList != null && mediaIndex != -1) {
                    mediaList = videoList
                    currentMediaIndex = mediaIndex
                    updatePlayerList()
                    requestAudioFocus()
                    handlePlayPause(true)
                }
            }
            "PLAY_PAUSE" -> handlePlayPause()
            "NEXT" -> handleNext()
            "PREVIOUS" -> handlePrevious()
            "TOGGLE_REPEAT" -> {
                isRepeating = intent?.getBooleanExtra("IS_REPEATING", false) ?: false
                exoPlayer.repeatMode = if (isRepeating) ExoPlayer.REPEAT_MODE_ONE else ExoPlayer.REPEAT_MODE_OFF
                sendUpdateBroadcast()
            }
            "SEEK_TO" -> {
                val seekPosition = intent?.getIntExtra("SEEK_POSITION", 0) ?: 0
                if (isPrepared) {
                    exoPlayer.seekTo(seekPosition.toLong())
                }
            }
            "REQUEST_STATUS" -> sendUpdateBroadcast()
            "UPDATE_EQUALIZER" -> {
                val bandIndex = intent?.getIntExtra("BAND_INDEX", -1) ?: -1
                val bandValue = intent?.getIntExtra("BAND_VALUE", 50) ?: 50
                if (isPrepared) {
                    val currentSessionId = exoPlayer.audioSessionId
                    if (equalizer == null || equalizer?.id != currentSessionId) {
                        setupEqualizer()
                    }
                    applyEqualizerSetting(bandIndex, bandValue)
                }
            }
            ACTION_SET_SLEEP_TIMER -> {
                val durationMillis = intent?.getLongExtra("DURATION_MILLIS", 0) ?: 0L
                setSleepTimer(durationMillis)
            }
            ACTION_STOP_SERVICE -> stopSelf()
            ACTION_KILL_APP -> {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(NOTIFICATION_ID)

                localBroadcastManager.sendBroadcast(Intent(ACTION_KILL_APP))

                stopSelf()
            }

            ACTION_ATTACH_VIDEO_VIEW -> {
                if (isVideoMode && attachedPlayerView != null) {
                    exoPlayer.setVideoSurfaceView(attachedPlayerView?.videoSurfaceView as? SurfaceView)
                }
            }
        }
        return START_NOT_STICKY
    }

    fun isPlaying(): Boolean {
        return exoPlayer.isPlaying
    }
    fun pausePlayback() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            stopForeground(false)
            handler.removeCallbacks(updateProgressAction)
            sendUpdateBroadcast()
            updateNotification()
        }
    }
    @OptIn(UnstableApi::class)
    fun setPlayerView(playerView: PlayerView?) {
        attachedPlayerView?.player = null

        attachedPlayerView = playerView

        if (playerView != null) {
            playerView.player = exoPlayer
            Log.d(TAG, "PlayerView conectado.")
        } else {
            exoPlayer.clearVideoSurface()
            Log.d(TAG, "PlayerView desconectado, superficie de video limpiada.")
        }
    }

    private fun updatePlayerList() {
        val mediaItems = mediaList.map { item ->
            val uri: Uri = when (item) {
                is Song -> {
                    Log.d(TAG, "Cargando audio URI: ${item.path}")
                    Uri.parse(item.path)
                }
                is VideoItem -> {
                    val videoUri = item.getUri()
                    Log.d(TAG, "Cargando video URI: $videoUri")
                    videoUri
                }
                else -> throw IllegalArgumentException("Tipo de media no compatible")
            }

            MediaItem.fromUri(uri)
        }

        exoPlayer.setMediaItems(mediaItems, currentMediaIndex, 0L)
        exoPlayer.prepare()
        updateMediaMode()
    }

    @OptIn(UnstableApi::class)
    private fun updateMediaMode() {
        if (currentMediaIndex >= 0 && currentMediaIndex < mediaList.size) {
            isVideoMode = mediaList[currentMediaIndex] is VideoItem

            if (isVideoMode) {

                if (attachedPlayerView != null) {
                    exoPlayer.setVideoSurfaceView(attachedPlayerView?.videoSurfaceView as? SurfaceView)
                } else {
                    exoPlayer.clearVideoSurface()
                }
                exoPlayer.clearVideoSurface()
            }
        }
    }

    private fun handlePlayPause(forcePlay: Boolean = false) {
        if (isPrepared) {
            if (exoPlayer.isPlaying && !forcePlay) {
                exoPlayer.pause()
                stopForeground(false)
                handler.removeCallbacks(updateProgressAction)
            } else if (forcePlay || !exoPlayer.isPlaying) {
                requestAudioFocus()
                exoPlayer.play()
                startForeground(NOTIFICATION_ID, buildNotification())
                handler.post(updateProgressAction)
            }
            sendUpdateBroadcast()
            updateNotification()
        } else if (forcePlay) {
            requestAudioFocus()
            exoPlayer.play()
            sendUpdateBroadcast()
            updateNotification()
        }
    }

    private fun handleNext() {
        if (isPrepared && mediaList.isNotEmpty()) {
            if (exoPlayer.hasNextMediaItem()) {
                exoPlayer.seekToNextMediaItem()
            } else {
                lifecycleScope.launch {
                    saveMediaPlayToDatabase()
                }

                exoPlayer.seekTo(0, 0L)
                currentMediaIndex = 0
                exoPlayer.play()
                updateNotification()
                sendUpdateBroadcast()
            }
        }
    }

    private fun handlePrevious() {
        if (isPrepared && mediaList.isNotEmpty()) {
            if (exoPlayer.hasPreviousMediaItem()) {
                exoPlayer.seekToPreviousMediaItem()
            } else {
                currentMediaIndex = mediaList.size - 1
                exoPlayer.seekTo(currentMediaIndex, 0L)
                exoPlayer.play()

                updateNotification()
                sendUpdateBroadcast()

            }
        }
    }

    private fun setSleepTimer(durationMillis: Long) {
        handler.removeCallbacks(stopPlaybackRunnable)
        if (durationMillis > 0) {
            sleepTimerEndTime = System.currentTimeMillis() + durationMillis
            handler.postDelayed(stopPlaybackRunnable, durationMillis)
            if (!exoPlayer.isPlaying && isPrepared) {
                handlePlayPause()
            }
        } else {
            sleepTimerEndTime = 0
        }
        sendUpdateBroadcast()
    }

    private fun stopPlayback() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            stopForeground(false)
            handler.removeCallbacks(updateProgressAction)
            sendUpdateBroadcast()
            updateNotification()
        }
    }


    private fun sendUpdateBroadcast() {
        if (currentMediaIndex >= 0 && currentMediaIndex < mediaList.size) {
            val currentMedia = mediaList[currentMediaIndex]
            val mediaId: Long
            val title: String
            val artist: String
            val duration: Int
            val uri: Uri

            when (currentMedia) {
                is Song -> {
                    mediaId = currentMedia.id
                    title = currentMedia.title
                    artist = currentMedia.artist
                    duration = currentMedia.duration
                    uri = Uri.parse(currentMedia.path)
                }
                is VideoItem -> {
                    mediaId = currentMedia.id
                    title = currentMedia.title
                    artist = currentMedia.artist
                    duration = currentMedia.durationMs.toInt()
                    uri = currentMedia.getUri()
                }
                else -> return
            }

            val intent = Intent("com.krystalshard.lifemelodyapp.MUSIC_UPDATE").apply {
                putExtra("TITLE", title)
                putExtra("ARTIST", artist)
                putExtra("IS_PLAYING", exoPlayer.isPlaying)
                putExtra("DURATION", duration)
                putExtra("IS_REPEATING", isRepeating)
                putExtra("MEDIA_ID", mediaId)
                putExtra("MEDIA_URI", uri)
                putExtra("IS_VIDEO", isVideoMode)
                putExtra("SLEEP_TIMER_END_TIME", sleepTimerEndTime)
            }
            localBroadcastManager.sendBroadcast(intent)
        }
    }

    private suspend fun saveMediaPlayToDatabase() {
        Log.d(TAG, "saveMediaPlayToDatabase: INTENTANDO registrar la reproducción.")

        if (currentMediaIndex >= 0 && currentMediaIndex < mediaList.size) {
            val media = mediaList[currentMediaIndex]

            if (media is Song) {
                Log.d(TAG, "saveMediaPlayToDatabase: Media actual es Song: ${media.title} (ID: ${media.id})")

                try {
                    val existingPlay = appDatabase.songPlayDao().getSongPlayById(media.id)

                    if (existingPlay != null) {
                        existingPlay.playCount += 1
                        appDatabase.songPlayDao().update(existingPlay)
                        Log.d(TAG, "saveMediaPlayToDatabase: ÉXITO - ACTUALIZADO: ${media.title}. Nuevo conteo: ${existingPlay.playCount}")
                    } else {
                        val newPlay = SongPlayEntity(
                            id = media.id,
                            title = media.title,
                            artist = media.artist,
                            duration = media.duration,
                            path = media.path,
                            playCount = 1
                        )
                        appDatabase.songPlayDao().insert(newPlay)
                        Log.d(TAG, "saveMediaPlayToDatabase: ÉXITO - INSERTADO: ${media.title}. Conteo: 1")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "saveMediaPlayToDatabase: ERROR CRÍTICO de Room/DB: ${e.message}", e)
                }
            } else {
                Log.d(TAG, "saveMediaPlayToDatabase: Media no es Song, omitiendo el registro.")
            }
        } else {
            Log.e(TAG, "saveMediaPlayToDatabase: Índice de media inválido: $currentMediaIndex. No se puede registrar.")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(headsetReceiver)
        exoPlayer.stop()
        exoPlayer.release()
        equalizer?.release()
        equalizer = null
        handler.removeCallbacks(updateProgressAction)
        handler.removeCallbacks(stopPlaybackRunnable)
        audioManager.abandonAudioFocus(this)
        stopForeground(true)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Notificación de Reproducción"
            val descriptionText = "Muestra los controles del reproductor"

            // IMPORTANCE_LOW evita que el teléfono suene o vibre al aparecer/actualizarse
            val importance = NotificationManager.IMPORTANCE_LOW

            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getCurrentMediaInfo(): Triple<String, String, Int> {
        return if (currentMediaIndex >= 0 && currentMediaIndex < mediaList.size) {
            when (val media = mediaList[currentMediaIndex]) {
                is Song -> Triple(media.title, media.artist, R.drawable.ic_nota)
                is VideoItem -> Triple(media.title, "", R.drawable.ic_nota)

                else -> Triple("Desconocido", "", R.drawable.ic_nota)
            }
        } else {
            Triple("Sin Media", "", R.drawable.ic_nota)
        }
    }

    private fun buildNotification(): Notification {
        val (title, artist, mediaIcon) = getCurrentMediaInfo()
        val playPauseIcon = if (exoPlayer.isPlaying) R.drawable.ic_pause else R.drawable.ic_reproductor

        val prevIntent = Intent(this, MusicService::class.java).apply { putExtra("ACTION", "PREVIOUS") }
        val playPauseIntent = Intent(this, MusicService::class.java).apply { putExtra("ACTION", "PLAY_PAUSE") }
        val nextIntent = Intent(this, MusicService::class.java).apply { putExtra("ACTION", "NEXT") }
        val closeAppIntent = Intent(this, MusicService::class.java).apply { putExtra("ACTION", ACTION_KILL_APP) }

        val pendingPrevIntent = PendingIntent.getService(this, 0, prevIntent, PendingIntent.FLAG_IMMUTABLE)
        val pendingPlayPauseIntent = PendingIntent.getService(this, 1, playPauseIntent, PendingIntent.FLAG_IMMUTABLE)
        val pendingNextIntent = PendingIntent.getService(this, 2, nextIntent, PendingIntent.FLAG_IMMUTABLE)
        val pendingCloseAppIntent = PendingIntent.getService(this, 5, closeAppIntent, PendingIntent.FLAG_IMMUTABLE)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingOpenAppIntent = PendingIntent.getActivity(this, 4, openAppIntent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(mediaIcon)
            .setContentTitle(title)
            .setContentText(artist)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(exoPlayer.isPlaying)
            .setOnlyAlertOnce(true)
            .addAction(R.drawable.ic_previus, "Anterior", pendingPrevIntent)
            .addAction(playPauseIcon, "Reproducir/Pausar", pendingPlayPauseIntent)
            .addAction(R.drawable.ic_next, "Siguiente", pendingNextIntent)
            .addAction(R.drawable.ic_close, "Cerrar", pendingCloseAppIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .setDeleteIntent(pendingCloseAppIntent)
            .setContentIntent(pendingOpenAppIntent)

        return builder.build()
    }

    private fun updateNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun requestAudioFocus(): Boolean {
        val result: Int
        result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(audioAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(this, handler)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }
        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun onAudioFocusChange(focusChange: Int) {
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (resumeOnAudioFocusGain) {
                    exoPlayer.play()
                    resumeOnAudioFocusGain = false
                }
            }
            AudioManager.AUDIOFOCUS_LOSS,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                    resumeOnAudioFocusGain = true
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (exoPlayer.isPlaying) {
                    exoPlayer.pause()
                    resumeOnAudioFocusGain = true
                }
            }
        }
        sendUpdateBroadcast()
        updateNotification()
    }

    @OptIn(UnstableApi::class)
    private fun setupEqualizer() {
        val sessionId = exoPlayer.audioSessionId
        if (equalizer != null && equalizer?.enabled == true) return

        if (sessionId != AudioManager.AUDIO_SESSION_ID_GENERATE && sessionId != 0) {
            try {
                if (equalizer == null) {
                    equalizer = Equalizer(0, sessionId)
                }

                equalizer?.apply {
                    val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                    for (i in 0 until numberOfBands.toInt()) {
                        val savedValue = sharedPref.getInt("eq_band_$i", 50)
                        val range = bandLevelRange
                        val level = (range[0] + (range[1] - range[0]) * (savedValue / 100f)).toInt().toShort()
                        setBandLevel(i.toShort(), level)
                    }
                    enabled = true
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fallo setup: ${e.message}")
            }
        }
    }

    private fun applyEqualizerSetting(bandIndex: Int, value: Int) {
        if (equalizer == null) {
            setupEqualizer()
        }

        equalizer?.let { eq ->
            try {
                val numBands = eq.numberOfBands.toInt()
                if (bandIndex in 0 until numBands) {
                    val bandLevelRange = eq.bandLevelRange
                    val minLevel = bandLevelRange[0].toInt()
                    val maxLevel = bandLevelRange[1].toInt()

                    val bandLevel = (minLevel + (maxLevel - minLevel) * (value / 100f)).toInt().toShort()

                    if (!eq.enabled) {
                        eq.enabled = true
                    }

                    eq.setBandLevel(bandIndex.toShort(), bandLevel)

                    getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                        .edit()
                        .putInt("eq_band_$bandIndex", value)
                        .apply()

                    Log.d(TAG, "EQ Aplicado: Banda $bandIndex -> $bandLevel")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error en EQ: ${e.message}")
                setupEqualizer()
            }
        }
    }
}