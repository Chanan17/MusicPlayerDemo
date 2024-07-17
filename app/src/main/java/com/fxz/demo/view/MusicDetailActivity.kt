package com.fxz.demo.view

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.fxz.demo.R
import com.fxz.demo.model.MusicData
import com.fxz.demo.model.MusicModel.isPlaying
import com.fxz.demo.model.MusicModel.pauseMusic
import com.fxz.demo.model.MusicModel.playNextSong
import com.fxz.demo.model.MusicModel.playPreviousSong
import com.fxz.demo.model.MusicModel.resumeMusic
import com.fxz.demo.model.MusicService
import com.fxz.demo.utils.ACTION_PLAY_NEXT_SONG
import com.fxz.demo.utils.ACTION_PLAY_PREV_SONG
import com.fxz.demo.viewmodel.MusicViewModel
import java.io.File

class MusicDetailActivity : AppCompatActivity() {
    private val viewModel: MusicViewModel by viewModels()
    private lateinit var playPauseButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var albumCover: ImageView

    private var broadcastIsBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_detail)

        playPauseButton = findViewById(R.id.play_pause_button)
        prevButton = findViewById(R.id.prev_button)
        nextButton = findViewById(R.id.next_button)
        songTitle = findViewById(R.id.song_title)
        songArtist = findViewById(R.id.song_artist)
        albumCover = findViewById(R.id.album_cover)

        playPauseButton.setOnClickListener {
            if (viewModel.isPlaying()) {
                pauseMusic()
                playPauseButton.setImageResource(R.drawable.ic_play)
            } else {
                resumeMusic()
                playPauseButton.setImageResource(R.drawable.ic_pause)
            }
        }

        viewModel.currentSongIndex.observe(this, Observer {
            updateMusicDetail(viewModel.getCurMusic())
        })

        prevButton.setOnClickListener { playPreviousSong() }
        nextButton.setOnClickListener { playNextSong() }
        updateMusicDetail(viewModel.getCurMusic())
        registerReceiver()
    }

    private val playNextSongReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("MusicModel", "Broadcast received")
            if (intent?.action == ACTION_PLAY_NEXT_SONG) {
                updateMusicDetail(viewModel.getCurMusic())
            }
        }
    }

    fun registerReceiver() {
        if(broadcastIsBound == false){
            val filter = IntentFilter(ACTION_PLAY_NEXT_SONG)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.registerReceiver(playNextSongReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                this.registerReceiver(playNextSongReceiver, filter)
            }
            Log.d("main","register")
            broadcastIsBound = true
        }

    }

    private fun updateMusicDetail(music: MusicData?) {

        if (music != null) {
            Log.d("detail","update")
            songTitle.text = music.title
            songArtist.text = music.artist
            if(music.albumArt != null) {
                albumCover.setImageBitmap(music.albumArt)
            } else {
                albumCover.setImageResource(R.drawable.ic_album_placeholder)
            }

            if(isPlaying()){
                playPauseButton.setImageResource(R.drawable.ic_pause)
            }else {
                playPauseButton.setImageResource(R.drawable.ic_play)
            }

        }
    }

    private fun pauseMusic() {
        viewModel.pauseMusic()
        playPauseButton.setImageResource(R.drawable.ic_play)
    }

    private fun resumeMusic() {
        viewModel.resumeMusic()
        playPauseButton.setImageResource(R.drawable.ic_pause)
    }

    private fun playPreviousSong() {
        viewModel.playPreviousSong()
        updateMusicDetail(viewModel.getCurMusic())

//        if (musicFilePaths.isNotEmpty()) {
//            currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else musicFilePaths.size - 1
//            updateMusicDetail(musicFilePaths[currentSongIndex])
//            viewModel.setCurrentSongIndex(currentSongIndex)
//        }
    }

    private fun playNextSong() {
        Log.d("detail", viewModel.getCurrentSongIndex().toString())
        viewModel.playNextSong()
//        viewModel.getCurMusic()?.let { updateMusicDetail(it.filePath) }
        updateMusicDetail(viewModel.getCurMusic())
    }

    private fun isPlaying() = viewModel.isPlaying()
}

