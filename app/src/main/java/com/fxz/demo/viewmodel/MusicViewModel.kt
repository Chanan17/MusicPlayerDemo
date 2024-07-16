package com.fxz.demo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.fxz.demo.model.Music

class MusicViewModel(application: Application) : AndroidViewModel(application) {
    val currentSong = MutableLiveData<Music>()
    val isPlaying = MutableLiveData<Boolean>()

    fun updateCurrentSong(music: Music) {
        currentSong.value = music
    }

    fun updatePlayingStatus(isPlaying: Boolean) {
        this.isPlaying.value = isPlaying
    }
}