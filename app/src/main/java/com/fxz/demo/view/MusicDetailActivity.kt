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
import android.widget.SeekBar
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.fxz.demo.R
import com.fxz.demo.databinding.ActivityMusicDetailBinding
import com.fxz.demo.model.MusicData
import com.fxz.demo.utils.ACTION_PAUSE_SONG
import com.fxz.demo.utils.ACTION_PLAY_NEXT_SONG
import com.fxz.demo.utils.ACTION_PLAY_PREV_SONG
import com.fxz.demo.utils.ACTION_RESUME_SONG
import com.fxz.demo.utils.PACKAGE_NAME
import com.fxz.demo.viewmodel.MusicViewModel

class MusicDetailActivity : AppCompatActivity() {
    private val viewModel: MusicViewModel by viewModels()

    private lateinit var binding: ActivityMusicDetailBinding

    private var isSeekbarDragging = false
    private var isBroadcastBound = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMusicDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //播放，上一首，下一首按钮监听
        binding.btnDetailPlayPause.setOnClickListener {
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
        binding.btnDetailPrev.setOnClickListener {
            val intent = Intent(ACTION_PLAY_PREV_SONG)
            intent.setPackage(PACKAGE_NAME)
            sendBroadcast(intent)
        }
        binding.btnDetailNext.setOnClickListener {
            val intent = Intent(ACTION_PLAY_NEXT_SONG)
            intent.setPackage(PACKAGE_NAME)
            sendBroadcast(intent)
        }

        //进度条拖拽
        binding.seekBarDetail.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    // 用户拖动时，实时更新时间
                    binding.tvDetailCurrentTime.text = progressFormat(progress)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
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
                binding.btnDetailPlayPause.setImageResource(R.drawable.ic_play)
            }else if (intent?.action == ACTION_RESUME_SONG) {
                binding.btnDetailPlayPause.setImageResource(R.drawable.ic_pause)
            }
        }
    }

    //注册广播
    fun registerReceiver() {
        if(!isBroadcastBound){
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
            isBroadcastBound = true
        }

    }

    //更新详情页
    private fun updateMusicDetail(music: MusicData?) {

        if (music != null) {
            Log.d("detail","update")
            binding.tvDetailSongTitle.text = music.title
            binding.tvDetailSongArtist.text = music.artist
            if(music.albumArt != null) {
                binding.ivDetailAlbumCover.setImageBitmap(music.albumArt)
            } else {
                binding.ivDetailAlbumCover.setImageResource(R.drawable.ic_album_placeholder)
            }

            if(isPlaying()){
                binding.btnDetailPlayPause.setImageResource(R.drawable.ic_pause)
            }else {
                binding.btnDetailPlayPause.setImageResource(R.drawable.ic_play)
            }

            binding.tvDetailTotalTime.text = progressFormat(music.duration)
            binding.tvDetailCurrentTime.text = getCurProgress()?.let { progressFormat(it) }
            binding.seekBarDetail.max = music.duration
            binding.seekBarDetail.progress = getCurProgress()?.toInt() ?: 0
            updateSeekBar()

        }
    }

    //每秒更新进度条
    private val handler = Handler(Looper.getMainLooper())
    private fun updateSeekBar() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                if (isPlaying() && !isSeekbarDragging) {
                    val process = getCurProgress()?.toInt() ?: 0
                    binding.tvDetailCurrentTime.text = progressFormat(process)
                    binding.seekBarDetail.progress = process
                }
                handler.postDelayed(this, 1000)
            }
        }, 1000)
    }

    private fun isPlaying() = viewModel.isPlaying()

    //获取当前进度
    private fun getCurProgress() = viewModel.getCurProgress()

    //进度时间格式化
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

