package com.fxz.demo.model

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fxz.demo.utils.ACTION_PLAY_NEXT_SONG
import java.io.File

object MusicModel {

    val musicFiles = MutableLiveData<List<MusicData>>()

    private val _currentSongIndex = MutableLiveData<Int>()
    var currentSongIndex: MutableLiveData<Int>
        get() = _currentSongIndex
        set(value) {
            _currentSongIndex.value = value.value
        }

    private var size: Int = 0



    private var musicService: MusicService? = null
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



    fun bindService(context: Context) {
        val intent = Intent(context, MusicService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        if (isBound) {
            context.unbindService(serviceConnection)
            isBound = false
        }
    }

    fun loadMusicFiles() {
        val musicList = mutableListOf<MusicData>()
        val musicDir = Environment.getExternalStorageDirectory()

        Log.d("MusicModel", "MusicData directory: ${musicDir.absolutePath}")

        if (musicDir.exists() && musicDir.isDirectory) {
            traverseDirectory(musicDir, musicList)
        } else {
            Log.d("MusicModel", "MusicData directory does not exist or is not a directory.")
        }

        Log.d("MusicModel", "Total music files found: ${musicList.size}")
        size = musicList.size
        musicFiles.postValue(musicList)
//        listSize = musicList.size
    }

    private fun traverseDirectory(directory: File, musicList: MutableList<MusicData>) {
        val files = directory.listFiles()
        files?.let {
            for (file in it) {
                if (file.isDirectory) {
                    Log.d("MusicModel", "Traversing directory: ${file.absolutePath}")
                    traverseDirectory(file, musicList)
                } else {
                    Log.d("MusicModel", "Found file: ${file.absolutePath}")
                    if (file.isFile && file.extension in listOf("mp3", "wav", "m4a")) {
                        val musicData = extractMetadata(file)
                        musicList.add(musicData)
                    }
                }
            }
        } ?: run {
            Log.d("MusicModel", "No files found in directory: ${directory.absolutePath}")
        }
    }

    private fun extractMetadata(file: File): MusicData {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)

        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.nameWithoutExtension
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
        val art = retriever.embeddedPicture
        val albumArt = if (art != null) {
            BitmapFactory.decodeByteArray(art, 0, art.size)
        } else {
            null
        }
        retriever.release()

        return MusicData(title, artist, file.absolutePath, albumArt)
    }

    fun isPlaying(): Boolean {
        return musicService?.isPlaying() ?: false
    }

    fun playMusic(index: Int) {
        val musicList = musicFiles.value
        if (musicList != null && index in musicList.indices) {
            val filePath = musicList[index].filePath
            musicService?.playMusic(filePath)
        }
    }

    fun playPreviousSong() {
        Log.d("model","prevsong")
        val currentIndex = _currentSongIndex.value ?: 0

        val size = musicFiles.value?.size ?: 0
        if (size > 0) {
            val prevIndex = if (currentIndex!! > 0) currentIndex - 1 else size - 1
            setCurrentSongIndex(prevIndex)
            playMusic(prevIndex)
        }
    }

    fun playNextSong() {
        Log.d("model","nextsong")
        val currentIndex = _currentSongIndex.value ?: 0
        val size = musicFiles.value?.size ?: 0
        if (size > 0) {
            val nextIndex = (currentIndex + 1) % size
            setCurrentSongIndex(nextIndex)
            playMusic(nextIndex)
        }
    }

    fun pauseMusic() {
        musicService?.pauseMusic()
    }

    fun resumeMusic() {
        musicService?.resumeMusic()
    }



    fun setCurrentSongIndex(index: Int) {
        _currentSongIndex.value = index
    }

    fun getCurMusic(): MusicData? {
        val index = _currentSongIndex.value
        return if (index != null) {
            musicFiles.value?.getOrNull(index)
        } else {
            null
        }
    }


}