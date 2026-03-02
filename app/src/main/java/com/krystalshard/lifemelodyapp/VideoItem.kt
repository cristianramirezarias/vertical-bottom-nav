package com.krystalshard.lifemelodyapp.data

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoItem(
    val id: Long = 0,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val uriString: String
) : Parcelable {
    fun getUri(): Uri {
        return Uri.parse(uriString)
    }

    fun formatDuration(): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%d:%02d", minutes, seconds)
    }
}