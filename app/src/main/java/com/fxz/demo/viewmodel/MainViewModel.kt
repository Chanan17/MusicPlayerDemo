package com.fxz.demo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fxz.demo.model.MusicData
import com.fxz.demo.model.MusicModel

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MusicModel = MusicModel(application.applicationContext)

    val musicFiles: LiveData<List<MusicData>> = repository.musicFiles

    private val _currentSongIndex = MutableLiveData<Int>()

    init {
        _currentSongIndex.value = -1
    }

    val currentSongIndex: LiveData<Int>
        get() = _currentSongIndex

    fun loadMusicFiles() {
        repository.loadMusicFiles()
    }

    fun getCurrentSongIndex() = _currentSongIndex.value

    fun setCurrentSongIndex(index: Int) {
        _currentSongIndex.value = index
    }

    fun getSize() = repository.listSize

    fun getCurMusic() = currentSongIndex?.value?.let { musicFiles.value?.get(it) }
}