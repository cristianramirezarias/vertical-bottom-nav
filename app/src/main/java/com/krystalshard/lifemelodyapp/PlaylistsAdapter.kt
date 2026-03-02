package com.krystalshard.lifemelodyapp

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class PlaylistAdapter(
    private var playlists: List<Playlist>,
    private val onClick: (Playlist) -> Unit,
    private val onLongClick: (Playlist) -> Boolean,
    private var isMultiSelectMode: Boolean = false,
    private val selectedPlaylists: MutableSet<Playlist> = mutableSetOf()
) : RecyclerView.Adapter<PlaylistAdapter.PlaylistViewHolder>() {

    class PlaylistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.tvPlaylistName)
        val iconImage: ImageView = view.findViewById(R.id.ivPlaylistIcon)
        val card: CardView = view.findViewById(R.id.cardPlaylist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = playlists[position]
        holder.name.text = playlist.name

        val sharedPref = holder.itemView.context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val colorString = sharedPref.getString("theme_color", "#C34B92") ?: "#C34B92"
        val colorInt = try {
            android.graphics.Color.parseColor(colorString)
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#C34B92")
        }


        holder.iconImage.imageTintList = android.content.res.ColorStateList.valueOf(colorInt)


        if (selectedPlaylists.contains(playlist)) {
            try {
                holder.card.setCardBackgroundColor((colorInt and 0x00FFFFFF) or 0x45000000)
            } catch (e: Exception) {
                holder.card.setCardBackgroundColor(0x45C34B92.toInt())
            }
        } else {
            val typedValue = android.util.TypedValue()
            holder.itemView.context.theme.resolveAttribute(R.attr.colorDialogBackground, typedValue, true)
            holder.card.setCardBackgroundColor(typedValue.data)
        }

        holder.itemView.setOnClickListener { onClick(playlist) }
        holder.itemView.setOnLongClickListener { onLongClick(playlist) }

        val iconRes = if (playlist.iconResId != 0) {
            playlist.iconResId
        } else {
            R.drawable.ic_list
        }

        try {
            holder.iconImage.setImageResource(iconRes)
        } catch (e: Exception) {
            holder.iconImage.setImageResource(R.drawable.ic_list)
        }
    }

    override fun getItemCount() = playlists.size

    fun setMultiSelectMode(enabled: Boolean) {
        isMultiSelectMode = enabled
        if (!enabled) selectedPlaylists.clear()
        notifyDataSetChanged()
    }

    fun isInMultiSelectMode(): Boolean = isMultiSelectMode

    fun toggleSelection(playlist: Playlist) {
        if (selectedPlaylists.contains(playlist)) selectedPlaylists.remove(playlist)
        else selectedPlaylists.add(playlist)
        notifyDataSetChanged()
    }

    fun getSelectedPlaylists(): List<Playlist> = selectedPlaylists.toList()
    fun getSelectedCount(): Int = selectedPlaylists.size
    fun updateData(newList: List<Playlist>) {
        this.playlists = newList
        notifyDataSetChanged()
    }
}