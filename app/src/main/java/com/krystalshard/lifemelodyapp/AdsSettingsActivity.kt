package com.krystalshard.lifemelodyapp

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.krystalshard.lifemelodyapp.databinding.ActivityAdsSettingsBinding
import com.huawei.hms.ads.HwAds
import com.huawei.hms.ads.RequestOptions
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class AdsSettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdsSettingsBinding
    private val PREFS_NAME = "app_prefs"
    private val CONSENT_KEY = "ads_consent_given"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAdsSettingsBinding.inflate(layoutInflater)
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

        setupAdsSwitches()
        restoreSavedSettings()

        binding.btnSaveSettings.setOnClickListener {
            applyNewSettings()
        }
    }

    private fun setupAdsSwitches() {
        binding.switchPersonalized.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.switchNonPersonalized.isChecked = false
            } else if (!binding.switchNonPersonalized.isChecked) {
                binding.switchPersonalized.isChecked = true
            }
        }

        binding.switchNonPersonalized.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.switchPersonalized.isChecked = false
            } else if (!binding.switchPersonalized.isChecked) {
                binding.switchNonPersonalized.isChecked = true
            }
        }
    }

    private fun restoreSavedSettings() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isPersonalized = sharedPref.getBoolean(CONSENT_KEY, true)

        if (isPersonalized) {
            binding.switchPersonalized.isChecked = true
            binding.switchNonPersonalized.isChecked = false
        } else {
            binding.switchPersonalized.isChecked = false
            binding.switchNonPersonalized.isChecked = true
        }
    }

    private fun applyNewSettings() {
        val personalized = binding.switchPersonalized.isChecked

        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean(CONSENT_KEY, personalized).apply()

        HwAds.init(this)
        val requestOptions = RequestOptions.Builder()
            .setConsent(if (personalized) "0" else "1")
            .setNonPersonalizedAd(if (personalized)
                com.huawei.hms.ads.NonPersonalizedAd.ALLOW_ALL
            else com.huawei.hms.ads.NonPersonalizedAd.ALLOW_NON_PERSONALIZED)
            .build()
        HwAds.setRequestOptions(requestOptions)

        if (!personalized) {
            com.facebook.ads.AdSettings.setDataProcessingOptions(arrayOf("LDU"), 1, 1000)
        } else {
            com.facebook.ads.AdSettings.setDataProcessingOptions(arrayOf<String>())
        }

        com.google.android.gms.ads.MobileAds.initialize(this)

        Toast.makeText(this, getString(R.string.settings_updated), Toast.LENGTH_SHORT).show()
        finish()
    }
}