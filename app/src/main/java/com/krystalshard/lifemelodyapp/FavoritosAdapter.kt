package com.krystalshard.lifemelodyapp

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.krystalshard.lifemelodyapp.databinding.ItemCancionBinding

class FavoritosAdapter(
    private var songsList: List<Song>,
    private val onSongClickListener: OnSongClickListener,
    private val onFavoriteRemoveListener: OnFavoriteRemoveListener,
    private val context: Context // Nuevo parámetro
) : RecyclerView.Adapter<FavoritosAdapter.SongViewHolder>() {

    interface OnFavoriteRemoveListener {
        fun onRemoveFavorite(song: Song)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemCancionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songsList[position]
        holder.bind(song)
    }

    override fun getItemCount(): Int = songsList.size

    fun updateData(newSongsList: List<Song>) {
        songsList = newSongsList
        notifyDataSetChanged()
    }

    fun getSongsList(): List<Song> = songsList

    inner class SongViewHolder(private val binding: ItemCancionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onSongClickListener.onSongClick(songsList[adapterPosition])
                }
            }
            binding.btnFavorite.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onFavoriteRemoveListener.onRemoveFavorite(songsList[adapterPosition])
                }
            }
        }

        fun bind(song: Song) {
            binding.tituloCancion.text = song.title
            binding.nombreArtista.text = song.artist

            val sharedPref = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val colorString = sharedPref.getString("theme_color", "#C34B92")

            try {
                val colorInt = Color.parseColor(colorString)
                binding.btnFavorite.setColorFilter(colorInt)
            } catch (e: IllegalArgumentException) {
            }
        }
    }
}