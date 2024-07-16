package com.fxz.demo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.fxz.demo.model.Music
import com.fxz.demo.model.MusicRepository

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicRepository = MusicRepository(application.applicationContext)

    val musicFiles: LiveData<List<Music>> = repository.musicFiles

    fun loadMusicFiles() {
        repository.loadMusicFiles()
    }
}