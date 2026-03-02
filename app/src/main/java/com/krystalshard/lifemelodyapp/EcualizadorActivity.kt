package com.krystalshard.lifemelodyapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.krystalshard.lifemelodyapp.databinding.ActivityEcualizadorBinding
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class EcualizadorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEcualizadorBinding
    private val EQ_BANDS = 5
    private val equalizerSettings = IntArray(EQ_BANDS)
    private val TAG = "EcualizadorActivity"

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityEcualizadorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        loadEqualizerSettings()
        setupEqualizerSeekBars()
        setupChipColors()

        binding.chipGroupPresets.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                when (checkedIds.first()) {

                    R.id.chipPop -> aplicarPreset(intArrayOf(58, 54, 46, 54, 60))

                    R.id.chipRock -> aplicarPreset(intArrayOf(62, 56, 42, 56, 65))

                    R.id.chipJazz -> aplicarPreset(intArrayOf(46, 48, 54, 58, 52))

                    R.id.chipClassical -> aplicarPreset(intArrayOf(60, 50, 40, 50, 60))

                    R.id.chipReggaeton -> aplicarPreset(intArrayOf(60, 48, 36, 48, 60))

                    R.id.chipElectronic -> aplicarPreset(intArrayOf(64, 52, 44, 55, 66))

                    R.id.chipHipHop -> aplicarPreset(intArrayOf(58, 65, 50, 45, 48))
                }
            }
        }

        binding.btnReiniciarEcualizador.setOnClickListener {

            binding.chipGroupPresets.clearCheck()

            resetEqualizer()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun aplicarDegradadoDinamico(seekBar: SeekBar) {
        // Detectamos la dirección del sistema del usuario
        val isRtl = seekBar.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL

        if (isRtl) {
            configurarBarraEspejo(seekBar)
        } else {
            configurarBarraNormal(seekBar)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun configurarBarraNormal(seekBar: SeekBar) {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val userColor = Color.parseColor(sharedPref.getString("theme_color", "#000000"))
        val colorFondoTema = if (isDarkTheme()) Color.WHITE else Color.BLACK
        val progresoActual = seekBar.progress

        val gradient = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
            intArrayOf(userColor, colorFondoTema)
        ).apply {
            cornerRadius = 100f
            setSize(0, 3)
        }

        val progressClip = android.graphics.drawable.ClipDrawable(
            gradient,
            android.view.Gravity.LEFT,
            android.graphics.drawable.ClipDrawable.HORIZONTAL
        )

        aplicarYRefrescar(seekBar, progressClip, userColor, progresoActual)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun configurarBarraEspejo(seekBar: SeekBar) {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val userColor = Color.parseColor(sharedPref.getString("theme_color", "#000000"))
        val colorFondoTema = if (isDarkTheme()) Color.WHITE else Color.BLACK
        val progresoActual = seekBar.progress

        val gradient = android.graphics.drawable.GradientDrawable(
            android.graphics.drawable.GradientDrawable.Orientation.RIGHT_LEFT,
            intArrayOf(userColor, colorFondoTema)
        ).apply {
            cornerRadius = 100f
            setSize(0, 3)
        }

        val progressClip = android.graphics.drawable.ClipDrawable(
            gradient,
            android.view.Gravity.RIGHT,
            android.graphics.drawable.ClipDrawable.HORIZONTAL
        )

        aplicarYRefrescar(seekBar, progressClip, userColor, progresoActual)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun aplicarYRefrescar(seekBar: SeekBar, clip: android.graphics.drawable.ClipDrawable, color: Int, progreso: Int) {
        val background = seekBar.progressDrawable.mutate() as android.graphics.drawable.LayerDrawable
        background.setDrawableByLayerId(android.R.id.progress, clip)

        seekBar.maxHeight = 3
        seekBar.minHeight = 3
        seekBar.thumb.setTint(color)
        seekBar.progress = 0
        seekBar.progress = progreso
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun setupEqualizerSeekBars() {
        val seekBars = arrayOf(
            binding.seekBarBanda1,
            binding.seekBarBanda2,
            binding.seekBarBanda3,
            binding.seekBarBanda4,
            binding.seekBarBanda5
        )

        for (i in seekBars.indices) {
            aplicarDegradadoDinamico(seekBars[i])
            seekBars[i].progress = equalizerSettings[i]
            seekBars[i].setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        equalizerSettings[i] = progress
                        binding.chipGroupPresets.clearCheck()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val finalProgress = seekBar?.progress ?: 50
                    sendEqualizerUpdateToService(i, finalProgress)
                    saveEqualizerSettings()
                }
            })
        }
    }

    private fun sendEqualizerUpdateToService(bandIndex: Int, value: Int) {
        val intent = Intent(this, MusicService::class.java).apply {
            putExtra("ACTION", "UPDATE_EQUALIZER")
            putExtra("BAND_INDEX", bandIndex)
            putExtra("BAND_VALUE", value)
        }
        startService(intent)
    }

    private fun loadEqualizerSettings() {
        Log.d(TAG, "Cargando ajustes de SharedPreferences.")
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        for (i in 0 until EQ_BANDS) {
            equalizerSettings[i] = sharedPref.getInt("eq_band_$i", 50)
        }
        updateSeekBarValues()
    }

    private fun saveEqualizerSettings() {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            for (i in 0 until EQ_BANDS) {
                putInt("eq_band_$i", equalizerSettings[i])
            }
            putInt("selected_chip_id", binding.chipGroupPresets.checkedChipId)
            apply()
        }
    }

    private fun aplicarPreset(valores: IntArray) {
        for (i in 0 until EQ_BANDS) {
            equalizerSettings[i] = valores[i]
            sendEqualizerUpdateToService(i, valores[i])
        }
        updateSeekBarValues()
        saveEqualizerSettings()
    }

    private fun resetEqualizer() {
        val valoresPlanos = intArrayOf(50, 50, 50, 50, 50)
        aplicarPreset(valoresPlanos)
        saveEqualizerSettings()
    }

    private fun updateSeekBarValues() {
        val seekBars = arrayOf(
            binding.seekBarBanda1,
            binding.seekBarBanda2,
            binding.seekBarBanda3,
            binding.seekBarBanda4,
            binding.seekBarBanda5
        )
        for (i in seekBars.indices) {
            seekBars[i].progress = equalizerSettings[i]
        }
    }

    private fun setupChipColors() {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val userColor = Color.parseColor(sharedPref.getString("theme_color", "#000000"))

        val softUserColor = ColorUtils.setAlphaComponent(userColor, 128)

        val backgroundStates = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf(-android.R.attr.state_checked)
        )

        val backgroundColors = intArrayOf(
            softUserColor,
            if (isDarkTheme()) Color.parseColor("#2C2C2C") else Color.parseColor("#E0E0E0")
        )

        val textStates = arrayOf(
            intArrayOf(android.R.attr.state_checked),
            intArrayOf()
        )
        val textColors = intArrayOf(
            if (isDarkTheme()) Color.WHITE else Color.BLACK,
            if (isDarkTheme()) Color.GRAY else Color.DKGRAY
        )

        val bgList = android.content.res.ColorStateList(backgroundStates, backgroundColors)
        val txtList = android.content.res.ColorStateList(textStates, textColors)

        for (i in 0 until binding.chipGroupPresets.childCount) {
            val chip = binding.chipGroupPresets.getChildAt(i) as com.google.android.material.chip.Chip
            chip.chipBackgroundColor = bgList
            chip.setTextColor(txtList)
            chip.chipStrokeWidth = 0f
        }
    }

    private fun isDarkTheme(): Boolean {
        return (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }


    override fun onDestroy() {
        super.onDestroy()
        saveEqualizerSettings()
    }
}