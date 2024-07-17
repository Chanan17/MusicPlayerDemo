package com.fxz.demo.view

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.fxz.demo.viewmodel.MusicViewModel
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.fxz.demo.R
import com.fxz.demo.model.MusicData
import com.fxz.demo.databinding.ActivityMainBinding
import com.fxz.demo.utils.ACTION_PLAY_NEXT_SONG
import com.fxz.demo.utils.ACTION_PLAY_PREV_SONG

class MainActivity : AppCompatActivity() {
    private val viewModel: MusicViewModel by viewModels()
    private lateinit var adapter: MusicAdapter
    private lateinit var binding: ActivityMainBinding
    private lateinit var playPauseButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var albumCover: ImageView

    private val REQUEST_MEDIA_AUDIO = 1

    private var broadcastIsBound = false

    private val playNextSongReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("MusicModel", "Broadcast received")
            if (intent?.action == ACTION_PLAY_NEXT_SONG) {
                playNextSong()
                updateBottomControlBar(viewModel.getCurMusic())
            } else if (intent?.action == ACTION_PLAY_PREV_SONG) {
                playPreviousSong()
                updateBottomControlBar(viewModel.getCurMusic())
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 初始化底部控制栏
        playPauseButton = findViewById(R.id.play_pause_button)
        prevButton = findViewById(R.id.prev_button)
        nextButton = findViewById(R.id.next_button)
        songTitle = findViewById(R.id.song_title)
        songArtist = findViewById(R.id.song_artist)
        albumCover = findViewById(R.id.album_cover)

        // 禁用按钮
        updateControlButtons(false)

        // 检查并请求权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "Requesting READ_MEDIA_AUDIO permission")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_MEDIA_AUDIO), REQUEST_MEDIA_AUDIO)
            } else {
                Log.d("Permission", "READ_MEDIA_AUDIO permission already granted")
                viewModel.loadMusicFiles()
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                Log.d("Permission", "Requesting READ_EXTERNAL_STORAGE permission")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_MEDIA_AUDIO)
            } else {
                Log.d("Permission", "READ_EXTERNAL_STORAGE permission already granted")
                viewModel.loadMusicFiles()
            }
        }

        adapter = MusicAdapter(emptyList()) { filePath ->
            val selectedIndex = viewModel.musicFiles.value?.indexOfFirst { it.filePath == filePath } ?: 0
            if (viewModel.currentSongIndex.value == selectedIndex) {
                // 当前点击的歌曲已经在播放，进入详情页
                val intent = Intent(this, MusicDetailActivity::class.java)
//                val filePaths = viewModel.musicFiles.value?.map { it.filePath }?.toTypedArray()
//                intent.putExtra("musicFiles", filePaths)
//                intent.putExtra("currentSongIndex", viewModel.getCurrentSongIndex())
                startActivity(intent)
            } else {
                // 播放新的歌曲
                viewModel.setCurrentSongIndex(selectedIndex)
                Log.d("main play new",viewModel.getCurrentSongIndex().toString())
                playMusic(selectedIndex)
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.musicFiles.observe(this, Observer { musicFiles ->
            adapter.updateMusicList(musicFiles)
            val currentSongIndex = viewModel.getCurrentSongIndex() ?: -1
            if ( currentSongIndex >= 0 && currentSongIndex < musicFiles.size) {
                updateBottomControlBar(musicFiles[currentSongIndex])
            } else {
                updateBottomControlBar(null)
            }
        })

        viewModel.currentSongIndex.observe(this, Observer { index ->
            // 当索引变化时，更新底部控制栏
            Log.d("Main", index.toString())
            val musicFiles = viewModel.musicFiles.value
            if (musicFiles != null && index >= 0 && index < musicFiles.size) {
                updateBottomControlBar(musicFiles[index])
            } else {
                updateBottomControlBar(null)
            }
        })


        playPauseButton.setOnClickListener {
            if (viewModel.isPlaying()) {
                pauseMusic()
                playPauseButton.setImageResource(R.drawable.ic_play)
            } else {
                resumeMusic()
                playPauseButton.setImageResource(R.drawable.ic_pause)
            }
        }

        prevButton.setOnClickListener { playPreviousSong() }
        nextButton.setOnClickListener { playNextSong() }

        viewModel.bindService(this@MainActivity)
        registerReceiver()
    }

    private fun playMusic(index: Int) {
        viewModel.playMusic(index)
        updateControlButtons(true)
        playPauseButton.setImageResource(R.drawable.ic_pause)
    }

//    private fun playMusic(filePath: String) {
//        viewModel.playMusic(filePath)
////            viewModel.musicFiles.value?.get(currentSongIndex)?.let { updateBottomControlBar(it) }
//        viewModel.getCurMusic()?.let { updateBottomControlBar(it) }
//        playPauseButton.setImageResource(R.drawable.ic_pause)
//        updateControlButtons(true)
//    }

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
    //        if (isBound) {
//            viewModel.playPreviousSong()
//            val size = viewModel.getSize()
//            if(size != 0){
//                val currentSongIndex = viewModel.getCurrentSongIndex()
//                val index = if (currentSongIndex!! > 0) currentSongIndex - 1 else size - 1
//                viewModel.setCurrentSongIndex(index)
//            }
//        }
    }

    private fun playNextSong() {
        viewModel.playNextSong()
    }

    private fun updateBottomControlBar(music: MusicData?) {
        Log.d("Main","update buttom")
        if (music != null) {
            songTitle.text = music.title
            songArtist.text = music.artist
            // Get and set the album cover
            setAlbumCover(music.filePath)
            if(isPlaying()){
                playPauseButton.setImageResource(R.drawable.ic_pause)
            }else {
                playPauseButton.setImageResource(R.drawable.ic_play)
            }
        } else {
            songTitle.text = ""
            songArtist.text = ""
            albumCover.setImageResource(R.drawable.ic_album_placeholder)
        }
    }

    private fun setAlbumCover(filePath: String) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            val art = retriever.embeddedPicture
            if (art != null) {
                val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                albumCover.setImageBitmap(bitmap)
            } else {
                albumCover.setImageResource(R.drawable.ic_album_placeholder)
            }
        } catch (e: Exception) {
            albumCover.setImageResource(R.drawable.ic_album_placeholder)
        } finally {
            retriever.release()
        }
    }

    private fun updateControlButtons(enable: Boolean) {
        playPauseButton.isEnabled = enable
        prevButton.isEnabled = enable
        nextButton.isEnabled = enable
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MEDIA_AUDIO && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission", "READ_MEDIA_AUDIO permission granted")
            viewModel.loadMusicFiles()
        } else {
            Log.d("Permission", "READ_MEDIA_AUDIO permission denied")
            // 权限请求被拒绝，可以在这里显示一个提示或者处理权限被拒绝的情况
        }
    }

    private fun isPlaying():Boolean {
        return viewModel.isPlaying()
    }


}
