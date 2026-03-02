package com.krystalshardbrCristianR.vertical_bottom_nav

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.krystalshardbrCristianR.vertical_bottom_nav.R

class CristianVerticalNav @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var listener: ((Int) -> Unit)? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.view_cristian_nav, this, true)
        setupButtons()
    }

    private fun setupButtons() {
        val buttons = listOf(
            findViewById<View>(R.id.btn_music),
            findViewById<View>(R.id.btn_player),
            findViewById<View>(R.id.btn_top),
            findViewById<View>(R.id.btn_lists)
        )

        buttons.forEachIndexed { index, view ->
            view.setOnClickListener {
                updateUI(index)
                listener?.invoke(index)
            }
        }

        updateUI(0)
    }

    private fun updateUI(selectedIndex: Int) {
        val ids = listOf(
            Triple(R.id.indicator_music, R.id.tv_music_vertical, R.id.iv_music),
            Triple(R.id.indicator_player, R.id.tv_player_vertical, R.id.iv_player),
            Triple(R.id.indicator_top, R.id.tv_top_vertical, R.id.iv_top),
            Triple(R.id.indicator_lists, R.id.tv_lists_vertical, R.id.iv_lists)
        )

        ids.forEachIndexed { index, (indicatorId, textId, iconId) ->
            val isSelected = index == selectedIndex
            findViewById<View>(indicatorId).visibility = if (isSelected) View.VISIBLE else View.INVISIBLE
            findViewById<View>(textId).visibility = if (isSelected) View.VISIBLE else View.INVISIBLE

            findViewById<ImageView>(iconId).alpha = if (isSelected) 1.0f else 0.5f
        }
    }

    fun setOnNavigationItemSelectedListener(l: (Int) -> Unit) {
        this.listener = l
    }
}