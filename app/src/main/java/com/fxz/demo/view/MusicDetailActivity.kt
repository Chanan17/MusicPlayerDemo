package com.fxz.demo.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.fxz.demo.R
import com.fxz.demo.model.MusicData
import com.fxz.demo.utils.ACTION_PAUSE_SONG
import com.fxz.demo.utils.ACTION_PLAY_NEXT_SONG
import com.fxz.demo.utils.ACTION_PLAY_PREV_SONG
import com.fxz.demo.utils.ACTION_RESUME_SONG
import com.fxz.demo.utils.PACKAGE_NAME
import com.fxz.demo.viewmodel.MusicViewModel

class MusicDetailActivity : AppCompatActivity() {
    private val viewModel: MusicViewModel by viewModels()
    private lateinit var playPauseButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var albumCover: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var currentTime: TextView
    private lateinit var totalTime: TextView

    private var isSeekbarDragging = false
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
        seekBar = findViewById(R.id.seek_bar)
        currentTime = findViewById(R.id.current_time)
        totalTime = findViewById(R.id.total_time)

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

        prevButton.setOnClickListener {
            val intent = Intent(ACTION_PLAY_PREV_SONG)
            intent.setPackage(PACKAGE_NAME)
            sendBroadcast(intent)
        }
        nextButton.setOnClickListener {
            val intent = Intent(ACTION_PLAY_NEXT_SONG)
            intent.setPackage(PACKAGE_NAME)
            sendBroadcast(intent)
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // 用户拖动时，实时更新 currentTime
                    currentTime.text = progressFormat(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                // 用户开始拖动时可以执行的操作
                isSeekbarDragging = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isSeekbarDragging = false
                // 用户停止拖动时，更新播放进度
                val newPosition = seekBar.progress
                viewModel.setNewProgress(newPosition)
            }
        })


        updateMusicDetail(viewModel.getCurMusic())
        registerReceiver()
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("detail", "Broadcast received")
            if (intent?.action == ACTION_PLAY_NEXT_SONG || intent?.action == ACTION_PLAY_PREV_SONG) {
                updateMusicDetail(viewModel.getCurMusic())
            }else if (intent?.action == ACTION_PAUSE_SONG) {
                playPauseButton.setImageResource(R.drawable.ic_play)
            }else if (intent?.action == ACTION_RESUME_SONG) {
                playPauseButton.setImageResource(R.drawable.ic_pause)
            }
        }
    }

    fun registerReceiver() {
        if(!broadcastIsBound){
            val filter = IntentFilter().apply {
                addAction(ACTION_PLAY_PREV_SONG)
                addAction(ACTION_PLAY_NEXT_SONG)
                addAction(ACTION_RESUME_SONG)
                addAction(ACTION_PAUSE_SONG)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                this.registerReceiver(broadcastReceiver, filter)
            }
            Log.d("detail","register")
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

            totalTime.text = progressFormat(music.duration)
            currentTime.text = getCurProgress()?.let { progressFormat(it) }
            seekBar.max = music.duration
            seekBar.progress = getCurProgress()?.toInt() ?: 0
            updateSeekBar()

        }
    }
    private val handler = Handler(Looper.getMainLooper())

    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isPlaying() && !isSeekbarDragging) {
                    val process = getCurProgress()?.toInt() ?: 0
                    currentTime.text = progressFormat(process)
                    seekBar.progress = process
                }
                handler.postDelayed(this, 1000)
            }
        }, 1000)
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

    private fun getCurProgress() = viewModel.getCurProgress()

    private fun progressFormat(progress: Int): String {
        val curProgress = progress / 1000
        val min = curProgress / 60
        val second = curProgress % 60

        // 格式化秒数
        val strSecond = if (second < 10) {
            "0$second"
        } else {
            "$second"
        }

        // 格式化分钟数
        val strMin = if (min < 10) {
            "0$min"
        } else {
            "$min"
        }

        return "$strMin:$strSecond"
    }
}

