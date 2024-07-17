package com.fxz.demo.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.fxz.demo.model.MusicData
import com.fxz.demo.model.MusicModel
import com.fxz.demo.view.MainActivity

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val model: MusicModel = MusicModel

    val musicFiles: LiveData<List<MusicData>> = model.musicFiles
    val currentSongIndex = model.currentSongIndex

    fun isPlaying() = model.isPlaying()

    fun loadMusicFiles() {
        model.loadMusicFiles()
    }

    fun getCurrentSongIndex() = currentSongIndex.value

    fun setCurrentSongIndex(index: Int) {
        model.setCurrentSongIndex(index)
    }


    fun getCurMusic(): MusicData? {
        return model.getCurMusic()
    }

//    fun getCurMusic() = currentSongIndex?.value?.let { musicFiles.value?.get(it) }

    fun playMusic(int: Int) {
        model.playMusic(int)
    }

    fun pauseMusic() {
        model.pauseMusic()
    }

    fun resumeMusic() {
        model.resumeMusic()
    }

    fun playPreviousSong() {
        model.playPreviousSong()
    }

    fun playNextSong() {
        model.playNextSong()
    }

    fun bindService(conetext: Context) {
        model.bindService(conetext)
    }

}