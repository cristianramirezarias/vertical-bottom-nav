package com.krystalshard.lifemelodyapp

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.Flow

class FavoritosViewModel(application: Application) : AndroidViewModel(application) {

    private val favoriteSongDao: FavoriteSongDao

    val allFavorites: Flow<List<FavoriteSongEntity>>

    init {
        val database = AppDatabase.getInstance(application)
        favoriteSongDao = database.favoriteSongDao()
        allFavorites = favoriteSongDao.getAllFavorites()
    }
}