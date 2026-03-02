package com.krystalshard.lifemelodyapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class IconAdapter(private val icons: List<Int>, private val onIconSelected: (Int) -> Unit) :
    RecyclerView.Adapter<IconAdapter.IconViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION
    var colorAplicado: Int = Color.parseColor("#C34B92")

    inner class IconViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImageView: ImageView = itemView.findViewById(R.id.iconItem)
        val container: FrameLayout = itemView.findViewById(R.id.iconContainer)
        val selectionIndicator: View = itemView.findViewById(R.id.selection_indicator)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val previouslySelected = selectedPosition

                    if (previouslySelected == position) return@setOnClickListener

                    selectedPosition = position

                    notifyItemChanged(previouslySelected)
                    notifyItemChanged(selectedPosition)

                    onIconSelected(icons[position])
                }
            }
        }

        fun bind(iconRes: Int, isSelected: Boolean) {
            iconImageView.setImageResource(iconRes)
            selectionIndicator.visibility = if (isSelected) View.VISIBLE else View.GONE

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IconViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_icon_selector, parent, false)
        return IconViewHolder(view)
    }

    override fun onBindViewHolder(holder: IconViewHolder, position: Int) {
        holder.iconImageView.setImageResource(icons[position])

        if (position == selectedPosition) {
            holder.selectionIndicator.visibility = View.VISIBLE
            holder.selectionIndicator.setBackgroundColor(colorAplicado)
        } else {
            holder.selectionIndicator.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = icons.size

    fun setSelectedIcon(iconResId: Int) {
        val newPosition = icons.indexOf(iconResId)
        val previouslySelected = selectedPosition

        if (iconResId == -1) {
            selectedPosition = RecyclerView.NO_POSITION
            if (previouslySelected != RecyclerView.NO_POSITION) {
                notifyItemChanged(previouslySelected)
            }
        } else if (newPosition != -1) {
            selectedPosition = newPosition

            if (previouslySelected != RecyclerView.NO_POSITION && previouslySelected != selectedPosition) {
                notifyItemChanged(previouslySelected)
            }
            notifyItemChanged(selectedPosition)
        }
    }
}