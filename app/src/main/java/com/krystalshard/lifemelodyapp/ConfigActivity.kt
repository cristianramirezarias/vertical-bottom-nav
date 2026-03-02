package com.krystalshard.lifemelodyapp

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.facebook.ads.*
import com.huawei.hms.ads.AdListener
import com.huawei.hms.ads.AdParam
import com.huawei.hms.ads.HwAds
import com.huawei.hms.ads.InterstitialAd
import com.krystalshard.lifemelodyapp.databinding.ActivityConfigBinding

class ConfigActivity : AppCompatActivity() {

    private lateinit var binding: ActivityConfigBinding
    private val PAYPAL_ME_URL = "https://www.paypal.com/paypalme/KrystalShard?country.x=GT&locale.x=es_XC"
    private var interstitialAd: InterstitialAd? = null
    private var metaInterstitial: com.facebook.ads.InterstitialAd? = null
    private val META_ID = "1952918715645443_1955629622041019"
    private var admobInterstitial: com.google.android.gms.ads.interstitial.InterstitialAd? = null
    private val ADMOB_ID = "ca-app-pub-2698591014902582/3478487955"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        HwAds.init(this)
        AudienceNetworkAds.initialize(this)
        enableEdgeToEdge()

        binding = ActivityConfigBinding.inflate(layoutInflater)
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

        setupClickListeners()
        loadInterstitialAd()
        loadMetaAd()
    }

    //id real:u2d11i0pna
    //id de prueba: testb4znbuh3n2
    private fun loadInterstitialAd() {
        interstitialAd = InterstitialAd(this)
        interstitialAd?.adId = "u2d11i0pna"
        interstitialAd?.adListener = object : AdListener() {
            override fun onAdLoaded() { super.onAdLoaded() }
            override fun onAdFailed(errorCode: Int) {
                loadMetaAd()
            }
            override fun onAdClosed() { loadInterstitialAd() }
        }
        interstitialAd?.loadAd(AdParam.Builder().build())
    }

    private fun loadMetaAd() {
        metaInterstitial = com.facebook.ads.InterstitialAd(this, META_ID)
        val metaListener = object : InterstitialAdListener {
            override fun onInterstitialDismissed(ad: Ad?) { loadMetaAd() }
            override fun onError(ad: Ad?, error: AdError?) {
                loadAdMobAd()
            }
            override fun onAdLoaded(ad: Ad?) {}
            override fun onAdClicked(ad: Ad?) {}
            override fun onLoggingImpression(ad: Ad?) {}
            override fun onInterstitialDisplayed(ad: Ad?) {}
        }
        metaInterstitial?.loadAd(metaInterstitial?.buildLoadAdConfig()?.withAdListener(metaListener)?.build())
    }

    private fun loadAdMobAd() {
        val sharedPref = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val currentConsent = sharedPref.getBoolean("ads_consent_given", true)

        val extras = Bundle()
        if (!currentConsent) extras.putString("npa", "1")

        val adRequest = com.google.android.gms.ads.AdRequest.Builder()
            .addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter::class.java, extras)
            .build()

        com.google.android.gms.ads.interstitial.InterstitialAd.load(this, ADMOB_ID, adRequest,
            object : com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: com.google.android.gms.ads.interstitial.InterstitialAd) {
                    admobInterstitial = ad
                }
                override fun onAdFailedToLoad(loadAdError: com.google.android.gms.ads.LoadAdError) {
                    admobInterstitial = null
                }
            })
    }

    private fun showInterstitialAd() {
        when {
            interstitialAd?.isLoaded == true -> {
                interstitialAd?.show(this)
            }
            metaInterstitial != null && metaInterstitial!!.isAdLoaded -> {
                metaInterstitial?.show()
            }
            admobInterstitial != null -> {
                admobInterstitial?.show(this)
                admobInterstitial = null
                loadAdMobAd()
            }
            else -> {

                loadInterstitialAd()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyThemeColorToIcons()
    }

    private fun applyThemeColorToIcons() {
        val sharedPref = getSharedPreferences("app_settings", MODE_PRIVATE)
        val savedColor = sharedPref.getString("theme_color", "#C34B92")
        val color = Color.parseColor(savedColor)

        val textViewsWithIcons = listOf(
            binding.tvTerms,
            binding.tvPrivacy,
            binding.tvPersonalization,
            binding.tvSleepTimer,
            binding.tvEqualizer,
            binding.tvVolumeControl,
            binding.tvAdOptions,
            binding.tvContact,
            binding.tvDonate,
            binding.tvSupportAd
        )

        textViewsWithIcons.forEach { textView ->
            textView.compoundDrawableTintList = ColorStateList.valueOf(color)
            textView.compoundDrawablesRelative.forEach { drawable ->
                drawable?.mutate()?.setTint(color)
            }
        }
    }

    private fun setupClickListeners() {
        binding.tvContact.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:lifemelody@krystalshard.com")
                putExtra(Intent.EXTRA_SUBJECT, "Contacto desde la app LifeMelody")
            }
            startActivity(emailIntent)
        }

        binding.tvDonate.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PAYPAL_ME_URL)))
        }

        binding.tvTerms.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.krystalshard.com/inicio/lifemelody/terminos-y-condiciones")))
        }

        binding.tvPrivacy.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.krystalshard.com/inicio/lifemelody/politicas-de-privacidad")))
        }

        binding.tvPersonalization.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.tvSleepTimer.setOnClickListener {
            showSleepTimerDialog()
        }

        binding.tvEqualizer.setOnClickListener {
            startActivity(Intent(this, EcualizadorActivity::class.java))
        }

        binding.tvVolumeControl.setOnClickListener {
            showVolumeControlDialog()
        }

        binding.tvAdOptions.setOnClickListener {
            startActivity(Intent(this, AdsSettingsActivity::class.java))
        }

        binding.tvSupportAd.setOnClickListener {
            showInterstitialAd()
        }
    }

    private fun showSleepTimerDialog() {
        val dialog = SleepTimerDialogFragment()
        dialog.show(supportFragmentManager, "SleepTimerDialog")
    }

    private fun showVolumeControlDialog() {
        val dialog = VolumeControlDialogFragment()
        dialog.show(supportFragmentManager, "VolumeControlDialog")
    }

    override fun onDestroy() {
        metaInterstitial?.destroy()
        super.onDestroy()
    }
}