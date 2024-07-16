package com.fxz.demo.view

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Bundle
import android.os.IBinder
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fxz.demo.R
import com.fxz.demo.model.Music
import com.fxz.demo.model.MusicService
import java.io.File
import java.io.IOException

class MusicDetailActivity : AppCompatActivity() {

    private lateinit var playPauseButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var albumCover: ImageView

    private var currentSongIndex: Int = -1
    private lateinit var musicFilePaths: Array<String>
    private lateinit var musicService: MusicService
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.LocalBinder
            musicService = binder.getService()
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_detail)

        playPauseButton = findViewById(R.id.play_pause_button)
        prevButton = findViewById(R.id.prev_button)
        nextButton = findViewById(R.id.next_button)
        songTitle = findViewById(R.id.song_title)
        songArtist = findViewById(R.id.song_artist)
        albumCover = findViewById(R.id.album_cover)

        // 获取传递过来的音乐文件路径列表和当前播放的歌曲索引
        musicFilePaths = intent.getStringArrayExtra("musicFiles")!!
        currentSongIndex = intent.getIntExtra("currentSongIndex", -1)

        if (currentSongIndex != -1) {
            updateMusicDetail(musicFilePaths[currentSongIndex])
        }

        playPauseButton.setOnClickListener {
            if (isBound) {
                if (musicService.isPlaying()) {
                    musicService.pauseMusic()
                    playPauseButton.setImageResource(R.drawable.ic_play)
                } else {
                    musicService.resumeMusic()
                    playPauseButton.setImageResource(R.drawable.ic_pause)
                }
            }
        }

        prevButton.setOnClickListener { playPreviousSong() }
        nextButton.setOnClickListener { playNextSong() }

        // 绑定服务
        val intent = Intent(this, MusicService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun updateMusicDetail(filePath: String) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: File(filePath).nameWithoutExtension
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val art = retriever.embeddedPicture
            val albumArt = if (art != null) {
                BitmapFactory.decodeByteArray(art, 0, art.size)
            } else {
                null
            }

            songTitle.text = title
            songArtist.text = artist
            if (albumArt != null) {
                albumCover.setImageBitmap(albumArt)
            } else {
                albumCover.setImageResource(R.drawable.ic_album_placeholder)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            retriever.release()
        }

        playMusic(filePath)
    }

    private fun playMusic(filePath: String) {
        if (isBound) {
            musicService.playMusic(filePath)
            playPauseButton.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun pauseMusic() {
        if (isBound) {
            musicService.pauseMusic()
            playPauseButton.setImageResource(R.drawable.ic_play)
        }
    }

    private fun resumeMusic() {
        if (isBound) {
            musicService.resumeMusic()
            playPauseButton.setImageResource(R.drawable.ic_pause)
        }
    }

    private fun playPreviousSong() {
        if (musicFilePaths.isNotEmpty()) {
            currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else musicFilePaths.size - 1
            updateMusicDetail(musicFilePaths[currentSongIndex])
        }
    }

    private fun playNextSong() {
        if (musicFilePaths.isNotEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % musicFilePaths.size
            updateMusicDetail(musicFilePaths[currentSongIndex])
        }
    }

    private fun showErrorMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
    }
}

