package com.krystalshard.lifemelodyapp//APPGALLERY

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.content.Context
import com.krystalshard.lifemelodyapp.databinding.ActivityAdsBinding
import com.huawei.hms.ads.HwAds
import com.huawei.hms.ads.RequestOptions
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class AdsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdsBinding
    private val PREFS_NAME = "app_prefs"
    private val CONSENT_KEY = "ads_consent_given"

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityAdsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.acceptButton.setOnClickListener {
            saveConsentAndStartMain(true)
        }

        binding.rejectButton.setOnClickListener {
            saveConsentAndStartMain(false)
        }

        binding.privacyLink.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.krystalshard.com/inicio/lifemelody/politicas-de-privacidad"))
            startActivity(browserIntent)
        }

        binding.termsLink.setOnClickListener {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.krystalshard.com/inicio/lifemelody/terminos-y-condiciones"))
            startActivity(browserIntent)
        }
    }

    private fun saveConsentAndStartMain(adsPersonalized: Boolean) {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean(CONSENT_KEY, adsPersonalized)
            apply()
        }

        HwAds.init(this)
        val requestOptions = RequestOptions.Builder()
            .setConsent(if (adsPersonalized) "0" else "1")
            .setNonPersonalizedAd(if (adsPersonalized)
                com.huawei.hms.ads.NonPersonalizedAd.ALLOW_ALL
            else com.huawei.hms.ads.NonPersonalizedAd.ALLOW_NON_PERSONALIZED)
            .build()
        HwAds.setRequestOptions(requestOptions)

        if (!adsPersonalized) {
            com.facebook.ads.AdSettings.setDataProcessingOptions(arrayOf("LDU"), 1, 1000)
        } else {
            com.facebook.ads.AdSettings.setDataProcessingOptions(arrayOf<String>())
        }

        com.google.android.gms.ads.MobileAds.initialize(this)

        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("ADS_PERSONALIZED", adsPersonalized)
        }
        startActivity(intent)
        finish()
    }
}