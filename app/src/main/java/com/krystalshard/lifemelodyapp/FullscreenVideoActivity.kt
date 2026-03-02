package com.krystalshard.lifemelodyapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.krystalshard.lifemelodyapp.databinding.ActivityFullscreenVideoBinding
import kotlin.also
import kotlin.apply
import kotlin.jvm.java
import androidx.activity.enableEdgeToEdge

class FullscreenVideoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullscreenVideoBinding
    private var musicService: MusicService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as MusicService.LocalBinder
            musicService = binder.getService()
            isBound = true

            musicService?.setPlayerView(binding.fullscreenPlayerView)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            musicService = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityFullscreenVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, binding.root)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onStart() {
        super.onStart()
        Intent(this, MusicService::class.java).also { intent ->
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            musicService?.setPlayerView(null)

            val wasPlaying = musicService?.isPlaying() ?: false
            if (wasPlaying) {
                musicService?.pausePlayback()
            }

            val intent = Intent("com.example.lifemelody.FULLSCREEN_EXIT").apply {
                putExtra("WAS_PLAYING", wasPlaying)
            }
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {

            unbindService(serviceConnection)
            isBound = false
            musicService = null
        }
    }
}