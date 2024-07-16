package com.fxz.demo.view

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.fxz.demo.viewmodel.MainViewModel
import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.fxz.demo.R
import com.fxz.demo.model.Music
import com.fxz.demo.databinding.ActivityMainBinding
import com.fxz.demo.model.MusicService
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private val viewModel: MainViewModel by viewModels()
    private lateinit var adapter: MusicAdapter
    private lateinit var binding: ActivityMainBinding
    private var mediaPlayer: MediaPlayer? = null
    private val REQUEST_MEDIA_AUDIO = 1
    private var currentSongIndex: Int = -1
    private lateinit var playPauseButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var nextButton: ImageButton
    private lateinit var songTitle: TextView
    private lateinit var songArtist: TextView
    private lateinit var albumCover: ImageView

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
            if (currentSongIndex == selectedIndex) {
                // 当前点击的歌曲已经在播放，进入详情页
                val intent = Intent(this, MusicDetailActivity::class.java)
                val filePaths = viewModel.musicFiles.value?.map { it.filePath }?.toTypedArray()
                intent.putExtra("musicFiles",filePaths)
                intent.putExtra("currentSongIndex",currentSongIndex)
                startActivity(intent)
            } else {
                // 播放新的歌曲
                currentSongIndex = selectedIndex
                playMusic(filePath)
            }
        }


        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        viewModel.musicFiles.observe(this, Observer { musicFiles ->
            adapter.updateMusicList(musicFiles)
            if (currentSongIndex >= 0 && currentSongIndex < musicFiles.size) {
                updateBottomControlBar(musicFiles[currentSongIndex])
            } else {
                updateBottomControlBar(null)
            }
        })

        playPauseButton.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                pauseMusic()
            } else {
                resumeMusic()
            }
        }

        prevButton.setOnClickListener { playPreviousSong() }
        nextButton.setOnClickListener { playNextSong() }
    }



    private fun playMusic(filePath: String) {
        mediaPlayer?.release()
        mediaPlayer = null

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(filePath)
                prepare()
                start()
                Log.d("MainActivity", "Music is playing...")

                setOnCompletionListener {
                    Log.d("MainActivity", "Music playback completed.")
                    playNextSong()
                }
            } catch (e: IOException) {
                Log.e("MainActivity", "Error setting data source or preparing media player", e)
                showErrorMessage("无法播放音乐文件")
            } catch (e: IllegalArgumentException) {
                Log.e("MainActivity", "Invalid argument provided to media player", e)
                showErrorMessage("无法播放音乐文件")
            } catch (e: SecurityException) {
                Log.e("MainActivity", "Permission denied while accessing the file", e)
                showErrorMessage("无法播放音乐文件")
            } catch (e: IllegalStateException) {
                Log.e("MainActivity", "Media player is in an illegal state", e)
                showErrorMessage("无法播放音乐文件")
            }
        }

        viewModel.musicFiles.value?.get(currentSongIndex)?.let { updateBottomControlBar(it) }
        playPauseButton.setImageResource(R.drawable.ic_pause)
        updateControlButtons(true)
    }

    private fun showErrorMessage(message: String) {
        // 可以根据需要显示错误消息，这里使用 Toast 举例
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun pauseMusic() {
        mediaPlayer?.pause()
        playPauseButton.setImageResource(R.drawable.ic_play)
    }

    private fun resumeMusic() {
        mediaPlayer?.start()
        playPauseButton.setImageResource(R.drawable.ic_pause)
    }

    private fun playPreviousSong() {
        val musicFiles = viewModel.musicFiles.value ?: return
        if (musicFiles.isNotEmpty()) {
            currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else musicFiles.size - 1
            val prevFilePath = musicFiles[currentSongIndex].filePath
            playMusic(prevFilePath)
        }
    }

    private fun playNextSong() {
        val musicFiles = viewModel.musicFiles.value ?: return
        if (musicFiles.isNotEmpty()) {
            currentSongIndex = (currentSongIndex + 1) % musicFiles.size
            val nextFilePath = musicFiles[currentSongIndex].filePath
            playMusic(nextFilePath)
        }
    }

    private fun updateBottomControlBar(music: Music?) {
        if (music != null) {
            songTitle.text = music.title
            songArtist.text = music.artist
            // Get and set the album cover
            setAlbumCover(music.filePath)
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

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}
