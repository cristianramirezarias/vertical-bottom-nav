package com.krystalshard.lifemelodyapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.krystalshard.lifemelodyapp.databinding.ActivityWelcomeBinding
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    private val PREFS_NAME = "app_prefs"
    private val ACCEPTED_KEY = "has_accepted_policies"
    private val CONSENT_KEY = "ads_consent_given"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
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

        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val hasAcceptedPolicies = sharedPref.getBoolean(ACCEPTED_KEY, false)
        val hasGivenAdsConsent = sharedPref.contains(CONSENT_KEY)

        if (hasAcceptedPolicies && hasGivenAdsConsent) {
            startMainActivity(sharedPref.getBoolean(CONSENT_KEY, false))
        } else if (hasAcceptedPolicies && !hasGivenAdsConsent) {
            startAdsActivity()
        } else {
            showAcceptanceDialog()
        }
    }

    private fun showAcceptanceDialog() {
        binding.privacyInfoLayout.visibility = View.VISIBLE
        binding.acceptButton.isEnabled = false
        binding.termsCheckbox.setOnCheckedChangeListener { _, isChecked ->
            binding.acceptButton.isEnabled = isChecked
        }

        binding.acceptButton.setOnClickListener {
            if (binding.termsCheckbox.isChecked) {
                val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                with(sharedPref.edit()) {
                    putBoolean(ACCEPTED_KEY, true)
                    apply()
                }
                startAdsActivity()
            }
        }

        binding.declineAndExitButton.setOnClickListener {
            declineAndExit()
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

    private fun declineAndExit() {
        finishAffinity()
    }

    private fun startAdsActivity() {
        val intent = Intent(this, AdsActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startMainActivity(adsPersonalized: Boolean) {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("ADS_PERSONALIZED", adsPersonalized)
        }
        startActivity(intent)
        finish()
    }
}