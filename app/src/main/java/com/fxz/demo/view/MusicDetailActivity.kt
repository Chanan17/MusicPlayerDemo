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
    private lateinit var btn_play_pause: ImageButton
    private lateinit var btn_prev: ImageButton
    private lateinit var btn_next: ImageButton
    private lateinit var tv_song_title: TextView
    private lateinit var tv_song_artist: TextView
    private lateinit var iv_album_cover: ImageView
    private lateinit var seekBar: SeekBar
    private lateinit var tv_current_time: TextView
    private lateinit var tv_total_time: TextView
 
    private var isSeekbarDragging = false
    private var isbroadcastBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_music_detail)

        btn_play_pause = findViewById(R.id.btn_play_pause)
        btn_prev = findViewById(R.id.btn_prev)
        btn_next = findViewById(R.id.btn_next)
        tv_song_title = findViewById(R.id.tv_song_title)
        tv_song_artist = findViewById(R.id.tv_song_artist)
        iv_album_cover = findViewById(R.id.iv_album_cover)
        seekBar = findViewById(R.id.seek_bar)
        tv_current_time = findViewById(R.id.tv_current_time)
        tv_total_time = findViewById(R.id.tv_total_time)
        
        btn_play_pause.setOnClickListener {
            if (viewModel.isPlaying()) {
                val intent = Intent(ACTION_PAUSE_SONG)
                intent.setPackage(PACKAGE_NAME)
                sendBroadcast(intent)
            } else {
                val intent = Intent(ACTION_RESUME_SONG)
                intent.setPackage(PACKAGE_NAME)
                sendBroadcast(intent)
            }
        }
//        playPauseButton.setOnClickListener {
//            if (viewModel.isPlaying()) {
//                pauseMusic()
//                playPauseButton.setImageResource(R.drawable.ic_play)
//            } else {
//                resumeMusic()
//                playPauseButton.setImageResource(R.drawable.ic_pause)
//            }
//        }

        viewModel.currentSongIndex.observe(this, Observer {
            updateMusicDetail(viewModel.getCurMusic())
        })

        btn_prev.setOnClickListener {
            val intent = Intent(ACTION_PLAY_PREV_SONG)
            intent.setPackage(PACKAGE_NAME)
            sendBroadcast(intent)
        }
        btn_next.setOnClickListener {
            val intent = Intent(ACTION_PLAY_NEXT_SONG)
            intent.setPackage(PACKAGE_NAME)
            sendBroadcast(intent)
        }
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // 用户拖动时，实时更新 currentTime
                    tv_current_time.text = progressFormat(progress)
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
                btn_play_pause.setImageResource(R.drawable.ic_play)
            }else if (intent?.action == ACTION_RESUME_SONG) {
                btn_play_pause.setImageResource(R.drawable.ic_pause)
            }
        }
    }

    fun registerReceiver() {
        if(!isbroadcastBound){
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
            isbroadcastBound = true
        }

    }

    private fun updateMusicDetail(music: MusicData?) {

        if (music != null) {
            Log.d("detail","update")
            tv_song_title.text = music.title
            tv_song_artist.text = music.artist
            if(music.albumArt != null) {
                iv_album_cover.setImageBitmap(music.albumArt)
            } else {
                iv_album_cover.setImageResource(R.drawable.ic_album_placeholder)
            }

            if(isPlaying()){
                btn_play_pause.setImageResource(R.drawable.ic_pause)
            }else {
                btn_play_pause.setImageResource(R.drawable.ic_play)
            }

            tv_total_time.text = progressFormat(music.duration)
            tv_current_time.text = getCurProgress()?.let { progressFormat(it) }
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
                    tv_current_time.text = progressFormat(process)
                    seekBar.progress = process
                }
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }


    private fun pauseMusic() {
        viewModel.pauseMusic()
        btn_play_pause.setImageResource(R.drawable.ic_play)
    }

    private fun resumeMusic() {
        viewModel.resumeMusic()
        btn_play_pause.setImageResource(R.drawable.ic_pause)
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

