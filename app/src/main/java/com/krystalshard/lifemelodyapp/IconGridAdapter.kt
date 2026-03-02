package com.krystalshard.lifemelodyapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class IconGridAdapter(
    private val icons: List<Int>,
    private var selected: Int,
    private var colorAplicado: Int,
    private val onSelected: (Int) -> Unit
) : RecyclerView.Adapter<IconGridAdapter.IconViewHolder>() {

    inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.iconItem)
        val container: FrameLayout = itemView.findViewById(R.id.iconContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_icon_picker, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        val icon = icons[position]
        holder.img.setImageResource(icon)

        if (icon == selected) {
            holder.container.setBackgroundResource(R.drawable.bg_selected_icon)
            val background = holder.container.background
            if (background is android.graphics.drawable.GradientDrawable) {

                background.shape = android.graphics.drawable.GradientDrawable.OVAL


                val strokeWidth = (3 * holder.itemView.context.resources.displayMetrics.density).toInt()
                background.setStroke(strokeWidth, colorAplicado)
            }
        } else {

            holder.container.background = null
        }

        holder.itemView.setOnClickListener {
            selected = icon
            onSelected(icon)
            notifyDataSetChanged()
        }
    }

    fun updateColor(newColor: Int) {
        this.colorAplicado = newColor
        notifyDataSetChanged()
    }

    override fun getItemCount() = icons.size
}