package com.fxz.demo.model

import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.io.File

class MusicRepository(private val context: Context) {

    private val _musicFiles = MutableLiveData<List<Music>>()
    val musicFiles: LiveData<List<Music>> = _musicFiles

    fun loadMusicFiles() {
        val musicList = mutableListOf<Music>()
        val musicDir = Environment.getExternalStorageDirectory()

        Log.d("MusicRepository", "Music directory: ${musicDir.absolutePath}")

        if (musicDir.exists() && musicDir.isDirectory) {
            traverseDirectory(musicDir, musicList)
        } else {
            Log.d("MusicRepository", "Music directory does not exist or is not a directory.")
        }

        Log.d("MusicRepository", "Total music files found: ${musicList.size}")
        _musicFiles.postValue(musicList)
    }

    private fun traverseDirectory(directory: File, musicList: MutableList<Music>) {
        val files = directory.listFiles()
        files?.let {
            for (file in it) {
                if (file.isDirectory) {
                    Log.d("MusicRepository", "Traversing directory: ${file.absolutePath}")
                    traverseDirectory(file, musicList)
                } else {
                    Log.d("MusicRepository", "Found file: ${file.absolutePath}")
                    if (file.isFile && file.extension in listOf("mp3", "wav", "m4a")) {
                        val musicFile = extractMetadata(file)
                        musicList.add(musicFile)
                    }
                }
            }
        } ?: run {
            Log.d("MusicRepository", "No files found in directory: ${directory.absolutePath}")
        }
    }

    private fun extractMetadata(file: File): Music {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(file.absolutePath)

        val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.nameWithoutExtension
        val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"
        val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION) ?: ""
        val art = retriever.embeddedPicture
        val albumArt = if (art != null) {
            BitmapFactory.decodeByteArray(art, 0, art.size)
        } else {
            null
        }
        retriever.release()

        return Music(title, artist, file.absolutePath, albumArt)
    }
}