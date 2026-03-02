package com.krystalshard.lifemelodyapp

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.krystalshard.lifemelodyapp.data.VideoItem
import com.krystalshard.lifemelodyapp.databinding.ItemVideoBinding
import com.bumptech.glide.Glide

class VideoAdapter(
    private var videoList: List<VideoItem>,
    private val clickListener: (VideoItem) -> Unit,
    private val activity: VideosActivity,
    private var isMultiSelectMode: Boolean = false,
    private val selectedVideos: MutableSet<VideoItem> = mutableSetOf()
) : RecyclerView.Adapter<VideoAdapter.VideoViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        val binding = ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VideoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        val video = videoList[position]
        holder.bind(video)

        if (selectedVideos.contains(video)) {
            val sharedPref = holder.itemView.context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val colorString = sharedPref.getString("theme_color", "#C34B92") ?: "#C34B92"
            try {
                val colorInt = Color.parseColor(colorString)
                holder.itemView.setBackgroundColor((colorInt and 0x00FFFFFF) or 0x45000000)
            } catch (e: Exception) {
                holder.itemView.setBackgroundColor(0x45C34B92.toInt())
            }
        } else {
            holder.itemView.setBackgroundColor(Color.TRANSPARENT)
        }

        holder.itemView.setOnClickListener {
            if (isMultiSelectMode) toggleSelection(video) else clickListener(video)
        }

        holder.itemView.setOnLongClickListener {
            if (!isMultiSelectMode) {
                activity.startMultiSelectMode(video)
                true
            } else false
        }
    }

    override fun getItemCount(): Int = videoList.size

    fun selectInitialVideo(video: VideoItem) {
        isMultiSelectMode = true
        selectedVideos.add(video)
        val count = selectedVideos.size
        activity.updateSelectionCount(count)
        notifyDataSetChanged()
    }

    fun setMultiSelectMode(enabled: Boolean) {
        isMultiSelectMode = enabled
        if (!enabled) selectedVideos.clear()
        notifyDataSetChanged()
    }

    fun isInMultiSelectMode(): Boolean = isMultiSelectMode
    fun getSelectedVideos(): List<VideoItem> = selectedVideos.toList()

    private fun toggleSelection(video: VideoItem) {
        if (selectedVideos.contains(video)) selectedVideos.remove(video) else selectedVideos.add(video)

        val count = selectedVideos.size
        activity.updateSelectionCount(count)
        notifyDataSetChanged()
    }

    fun updateData(newVideoList: List<VideoItem>) {
        videoList = newVideoList
        notifyDataSetChanged()
    }

    class VideoViewHolder(private val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(video: VideoItem) {
            binding.textViewVideoTitle.text = video.title
            binding.durationVideo.text = video.formatDuration()

            Glide.with(binding.root.context)
                .load(video.getUri())
                .centerCrop()
                .into(binding.imageViewThumbnail)
        }
    }
}