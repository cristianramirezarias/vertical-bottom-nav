package com.krystalshard.lifemelodyapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.krystalshard.lifemelodyapp.databinding.ActivityMainBinding
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import android.content.IntentFilter
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val musicaFragment: Fragment = MusicaFragment()
    private val reproductorFragment: Fragment = ReproductorFragment()
    private val playlistsFragment: Fragment = PlaylistsFragment()
    private val topFragment: Fragment = TopFragment()
    private var activeFragment: Fragment = musicaFragment

    private lateinit var localBroadcastManager: LocalBroadcastManager

    private val killAppReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("MainActivity", "Recibiendo señal de cierre de la app")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(top = systemBars.top)
            windowInsets
        }

        localBroadcastManager = LocalBroadcastManager.getInstance(this)
        val adsPersonalized = intent.getBooleanExtra("ADS_PERSONALIZED", false)
        localBroadcastManager.registerReceiver(killAppReceiver, IntentFilter(MusicService.ACTION_KILL_APP))

        supportFragmentManager.beginTransaction().apply {
            add(R.id.fragment_container, reproductorFragment, "reproductorFragment").hide(reproductorFragment)
            add(R.id.fragment_container, topFragment, "topFragment").hide(topFragment)
            add(R.id.fragment_container, musicaFragment, "musicaFragment")
            add(R.id.fragment_container, playlistsFragment, "configFragment").hide(playlistsFragment)
        }.commit()

        val args = Bundle().apply { putBoolean("ADS_PERSONALIZED", adsPersonalized) }
        musicaFragment.arguments = args

        binding.btnMusic.setOnClickListener {
            showFragment(musicaFragment, "music")
        }

        binding.btnPlayer.setOnClickListener {
            showFragment(reproductorFragment, "player")
        }

        binding.btnTop.setOnClickListener {
            showFragment(topFragment, "top")
        }

        binding.btnLists.setOnClickListener {
            showFragment(playlistsFragment, "lists")
        }

        updateBottomBar("music")
    }

    override fun onDestroy() {
        super.onDestroy()
        localBroadcastManager.unregisterReceiver(killAppReceiver)
    }
    private fun showFragment(fragmentToShow: Fragment, tag: String) {
        if (activeFragment != fragmentToShow) {
            supportFragmentManager.beginTransaction().apply {
                hide(activeFragment)
                show(fragmentToShow)
            }.commit()
            activeFragment = fragmentToShow

            if (fragmentToShow is PlaylistsFragment) fragmentToShow.loadPlaylists()
            if (fragmentToShow is TopFragment) fragmentToShow.refreshData()

            updateBottomBar(tag)
        }
    }

    private fun updateBottomBar(selected: String) {
        val menuItems = listOf(
            Triple("music", binding.tvMusicVertical, binding.indicatorMusic),
            Triple("player", binding.tvPlayerVertical, binding.indicatorPlayer),
            Triple("top", binding.tvTopVertical, binding.indicatorTop),
            Triple("lists", binding.tvListsVertical, binding.indicatorLists)
        )

        menuItems.forEach { (tag, textView, indicator) ->
            val isActive = tag == selected

            textView.visibility = if (isActive) android.view.View.VISIBLE else android.view.View.INVISIBLE
            indicator.visibility = if (isActive) android.view.View.VISIBLE else android.view.View.INVISIBLE
        }

        binding.ivMusic.alpha = if (selected == "music") 1.0f else 0.8f
        binding.ivPlayer.alpha = if (selected == "player") 1.0f else 0.8f
        binding.ivTop.alpha = if (selected == "top") 1.0f else 0.8f
        binding.ivLists.alpha = if (selected == "lists") 1.0f else 0.8f
    }
}
//gracias padre, hijo y espiritu santo por que esta sera la primera app que publique
//gracias DIOS por la inteligencia
//gracias DIOS por todo
//esta app esta dedicada a ti
//gracias