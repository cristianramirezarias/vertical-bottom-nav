package com.krystalshard.lifemelodyapp

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.krystalshard.lifemelodyapp.databinding.ListItemSongBinding

interface OnSongClickListener {
    fun onSongClick(song: Song)
}

class SongAdapter(
    private var songs: List<Song>,
    private val listener: OnSongClickListener,
    private var themeColor: Int
) : RecyclerView.Adapter<SongAdapter.SongViewHolder>() {

    class SongViewHolder(private val binding: ListItemSongBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(song: Song, position: Int, themeColor: Int) {
            binding.songTitleText.text = song.title
            binding.songArtistText.text = song.artist
            binding.songNumberText.text = (position + 1).toString()
            binding.songNumberText.setTextColor(themeColor)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ListItemSongBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = songs[position]
        holder.bind(song, position, themeColor)
        holder.itemView.setOnClickListener {
            listener.onSongClick(song)
        }
    }

    override fun getItemCount(): Int = songs.size

    fun updateSongs(newSongs: List<Song>) {
        this.songs = newSongs
        notifyDataSetChanged()
    }

    fun updateThemeColor(color: Int) {
        this.themeColor = color
        notifyDataSetChanged()
    }
}