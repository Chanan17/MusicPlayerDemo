package com.fxz.demo.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.fxz.demo.model.MusicData
import com.fxz.demo.model.MusicModel

class MusicViewModel: ViewModel() {

    private val model: MusicModel = MusicModel

    val musicFiles = model.musicList
    val searchMusicFiles = model.searchMusicList
    val currentSongIndex = model.currentSongIndex
    val serviceBound = model.serviceBound

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

    fun getCurProgress() = model.getCurProgress()

    fun setNewProgress(newPosition: Int) {
        model.setNewProgress(newPosition)
    }

    fun createAndShowNotification() {
        model.createAndShowNotification()
    }

    fun updateNotification() {
        model.updateNotification()
    }

    fun updateMusicList(content: String) {
        model.updateMusicList(content)
    }

    fun initializeDB(context: Context) {
        model.initializeDB(context)
    }

    fun clearMusicHistory() {
        model.clearMusicHistory()
    }

}