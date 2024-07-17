package com.fxz.demo.model

import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.File

class MusicModel(private val context: Context) {

    private val _musicFiles = MutableLiveData<List<MusicData>>()
    val musicFiles: LiveData<List<MusicData>> = _musicFiles
    var listSize: Int = 0
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
        _musicFiles.postValue(musicList)
        listSize = musicList.size
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
                        val musicFile = extractMetadata(file)
                        musicList.add(musicFile)
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
}