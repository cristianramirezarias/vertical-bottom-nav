package com.krystalshard.lifemelodyapp

sealed class ListItem {
    data class SongItem(val song: Song) : ListItem()
    data class AdItem(val ad: Any) : ListItem()
}