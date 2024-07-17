package com.fxz.demo.model

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import android.util.Log
import java.io.IOException

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSongPath: String? = null
    private val binder = LocalBinder()

    private var musicFilePaths: List<String> = emptyList()
    private var currentSongIndex: Int = -1

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }


    fun playMusic(songPath: String) {
        if (currentSongPath != songPath) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                try {
                    Log.d("MusicService","playMusic()")
                    setDataSource(songPath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        playNextSong()
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            currentSongPath = songPath

        } else {
            resumeMusic()
        }
    }

    fun pauseMusic() {
        Log.d("MusicService","pauseMusic()")
        mediaPlayer?.pause()
    }

    fun resumeMusic() {
        Log.d("MusicService","resumeMusic()")
        mediaPlayer?.start()
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    fun playNextSong() {
        Log.d("MusicService","next1()")
        if (musicFilePaths.isNotEmpty()) {
            Log.d("MusicService","next2()")
            currentSongIndex = (currentSongIndex + 1) % musicFilePaths.size
            playMusic(musicFilePaths[currentSongIndex])
        }
    }

    fun playPreviousSong() {
        if (musicFilePaths.isNotEmpty()) {
            currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else musicFilePaths.size - 1
            playMusic(musicFilePaths[currentSongIndex])
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
