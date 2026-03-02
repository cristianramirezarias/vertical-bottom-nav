package com.krystalshard.lifemelodyapp//Appgallery

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.krystalshard.lifemelodyapp.databinding.ActivitySettingsBinding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.gridlayout.widget.GridLayout
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private var isGradientMode = false
    private var selectedIconResId: Int = R.drawable.ic_note
    private var selectedPhotoUri: Uri? = null
    private var selectedHexColor: String = "#000000"

    private val iconList = listOf(
        R.drawable.ic_note,
        R.drawable.ic_melody,
        R.drawable.ic_phone,
        R.drawable.ic_heard,
        R.drawable.ic_headset,
        R.drawable.ic_orbit,
        R.drawable.ic_gallery

    )

    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION

                contentResolver.takePersistableUriPermission(it, takeFlags)

                selectedPhotoUri = it
                selectedIconResId = -1
                (binding.rvIconSelector.adapter as? IconAdapter)?.setSelectedIcon(-1)
            } catch (e: SecurityException) {
            }
        }
    }

    companion object {
        const val THEME_SOLID = "solid"
        const val THEME_GRADIENT = "gradient"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
        setupColorGridSelector()
        setupThemeModeSwitches()
        setupIconSelector()
        restoreSavedSettings()

        binding.switchAnimations.setOnCheckedChangeListener { _, isChecked ->
            saveSettings(selectedHexColor, if (isGradientMode) THEME_GRADIENT else THEME_SOLID, selectedIconResId, selectedPhotoUri?.toString(), isChecked)
        }
    }

    private fun setupClickListeners() {
        binding.btnSaveColor.setOnClickListener {
            if (selectedHexColor.equals("#FFFFFF", ignoreCase = true)) {
                Toast.makeText(this, getString(R.string.no_seleccionar), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedPhotoUri != null) {
                saveSettings(selectedHexColor, if (isGradientMode) THEME_GRADIENT else THEME_SOLID, -1, selectedPhotoUri.toString())
            } else {
                saveSettings(selectedHexColor, if (isGradientMode) THEME_GRADIENT else THEME_SOLID, selectedIconResId, null)
            }

            finish()
        }

    }

    private fun setupColorGridSelector() {
        for (i in 0 until binding.colorSelectorForeground.childCount) {
            val colorFrame = binding.colorSelectorForeground.getChildAt(i) as FrameLayout
            colorFrame.setOnClickListener {
                handleColorSelection(colorFrame)
            }
        }
    }

    private fun handleColorSelection(selectedFrame: FrameLayout) {
        val gridLayout: androidx.gridlayout.widget.GridLayout = binding.colorSelectorForeground
        for (i in 0 until gridLayout.childCount) {
            val frame = gridLayout.getChildAt(i) as FrameLayout
            val checkmark = frame.getChildAt(1) as ImageView
            checkmark.visibility = View.INVISIBLE
        }

        val checkmark = selectedFrame.getChildAt(1) as ImageView
        checkmark.visibility = View.VISIBLE

        selectedHexColor = selectedFrame.tag.toString()

        val colorInt = android.graphics.Color.parseColor(selectedHexColor)
        (binding.rvIconSelector.adapter as? IconAdapter)?.let {
            it.colorAplicado = colorInt
            it.notifyDataSetChanged()
        }
    }

    private fun setupThemeModeSwitches() {
        binding.switchSolidColor.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.switchGradientBackground.isChecked = false
                isGradientMode = false

                saveSettings(
                    selectedHexColor,
                    THEME_SOLID,
                    selectedIconResId,
                    selectedPhotoUri?.toString()
                )
            } else if (!binding.switchGradientBackground.isChecked) {
                binding.switchSolidColor.isChecked = true
            }
        }

        binding.switchGradientBackground.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                binding.switchSolidColor.isChecked = false
                isGradientMode = true
                saveSettings(
                    selectedHexColor,
                    THEME_GRADIENT,
                    selectedIconResId,
                    selectedPhotoUri?.toString()
                )
            } else if (!binding.switchSolidColor.isChecked) {
                binding.switchGradientBackground.isChecked = true
            }
        }
    }

    private fun setupIconSelector() {
        val adapter = IconAdapter(iconList) { iconResId ->
            if (iconResId == R.drawable.ic_gallery) {
                getContent.launch("image/*")
            } else {
                selectedIconResId = iconResId
                selectedPhotoUri = null
            }
        }

        binding.rvIconSelector.layoutManager = androidx.recyclerview.widget.GridLayoutManager(this, 7)
        binding.rvIconSelector.adapter = adapter
    }

    private fun restoreSavedSettings() {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val themeMode = sharedPref.getString("theme_mode", THEME_SOLID)
        isGradientMode = (themeMode == THEME_GRADIENT)

        val savedColor = sharedPref.getString("theme_color", "#C34B92")
        selectedHexColor = savedColor ?: "#C34B92"

        val animationsEnabled = sharedPref.getBoolean("animations_enabled", false)
        binding.switchAnimations.isChecked = animationsEnabled

        val savedIconResId = sharedPref.getInt("icon_res_id", R.drawable.ic_note)
        val savedPhotoUriString = sharedPref.getString("photo_uri", null)

        if (savedPhotoUriString != null) {
            selectedPhotoUri = Uri.parse(savedPhotoUriString)
            selectedIconResId = -1
            (binding.rvIconSelector.adapter as? IconAdapter)?.setSelectedIcon(-1)
        } else {
            selectedIconResId = savedIconResId
            (binding.rvIconSelector.adapter as? IconAdapter)?.setSelectedIcon(savedIconResId)
        }

        if (isGradientMode) {
            binding.switchGradientBackground.isChecked = true
            binding.switchSolidColor.isChecked = false
        } else {
            binding.switchSolidColor.isChecked = true
            binding.switchGradientBackground.isChecked = false
        }

        markSelectedColor(selectedHexColor)

        val colorInt = android.graphics.Color.parseColor(selectedHexColor)
        (binding.rvIconSelector.adapter as? IconAdapter)?.let {
            it.colorAplicado = colorInt
            it.notifyDataSetChanged()
        }
    }

    private fun markSelectedColor(hexColor: String) {
        val gridLayout: GridLayout = binding.colorSelectorForeground
        for (i in 0 until gridLayout.childCount) {
            val frame = gridLayout.getChildAt(i) as FrameLayout
            val checkmark = frame.getChildAt(1) as ImageView

            if (frame.tag.toString().equals(hexColor, ignoreCase = true)) {
                checkmark.visibility = View.VISIBLE
            } else {
                checkmark.visibility = View.INVISIBLE
            }
        }
    }

    private fun saveSettings(color: String, themeMode: String, iconResId: Int, photoUri: String?, animationsEnabled: Boolean = binding.switchAnimations.isChecked) {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        val animationType = when {
            iconResId == R.drawable.ic_orbit -> "rotate"
            iconResId == R.drawable.ic_heard -> "rotate"
            else -> "rotate"
        }

        with(sharedPref.edit()) {
            putString("theme_color", color)
            putString("theme_mode", themeMode)
            putString("animation_type", animationType)
            putBoolean("animations_enabled", animationsEnabled)
            if (photoUri != null) {
                putString("photo_uri", photoUri)
                remove("icon_res_id")
            } else {
                putInt("icon_res_id", iconResId)
                remove("photo_uri")
            }
            apply()
        }
    }
}