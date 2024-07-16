package com.fxz.demo.model

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.os.Binder
import android.os.IBinder
import java.io.IOException

class MusicService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSongPath: String? = null
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun playMusic(songPath: String) {
        if (currentSongPath != songPath) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                try {
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
        mediaPlayer?.pause()
    }

    fun resumeMusic() {
        mediaPlayer?.start()
    }

    fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentSongPath = null
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

    private fun playNextSong() {
        // 实现播放下一首歌曲的逻辑
    }

    private fun playPreviousSong() {
        // 实现播放上一首歌曲的逻辑
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
