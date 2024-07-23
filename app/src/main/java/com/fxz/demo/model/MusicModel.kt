package com.fxz.demo.model

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Environment
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.fxz.demo.utils.MusicDatabaseHelper
import java.io.File

object MusicModel {

    private lateinit var dbHelper: MusicDatabaseHelper

    fun initializeDB(context: Context) {
        dbHelper = MusicDatabaseHelper(context)
    }

    private val originalMusicList = mutableListOf<MusicData>()
    val musicList = MutableLiveData<List<MusicData>>()
    val searchMusicList = MutableLiveData<List<MusicData>>()

    private val _currentSongIndex = MutableLiveData<Int>()
    var currentSongIndex: MutableLiveData<Int>
        get() = _currentSongIndex
        set(value) {
            _currentSongIndex.value = value.value
        }

    private var size: Int = 0

    private var musicService: MusicService? = null

    private val _serviceBound = MutableLiveData<Boolean>()
    val serviceBound: LiveData<Boolean> get() = _serviceBound

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.LocalBinder
            musicService = binder.getService()
            _serviceBound.postValue(true)
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _serviceBound.postValue(false)
        }
    }
    

    fun bindService(context: Context) {
        val intent = Intent(context, MusicService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    fun unbindService(context: Context) {
        if (_serviceBound.value == true) {
            context.unbindService(serviceConnection)
            _serviceBound.postValue(false)
        }
    }

    fun updateNotification() {
        getCurMusic()?.let { musicService?.updateNotification(it) }
    }


    fun loadMusicFiles(): Boolean {
        val musicDir = Environment.getExternalStorageDirectory()

        Log.d("MusicModel", "MusicData directory: ${musicDir.absolutePath}")

        if (musicDir.exists() && musicDir.isDirectory) {
            traverseDirectory(musicDir, originalMusicList)
        } else {
            Log.d("MusicModel", "MusicData directory does not exist or is not a directory.")
        }

        Log.d("MusicModel", "Total music files found: ${originalMusicList.size}")
        size = originalMusicList.size
        this.musicList.postValue(originalMusicList)
        this.searchMusicList.postValue(originalMusicList)
        return size!=0
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
        val duration: Int = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toInt() ?: 0

        retriever.release()

        return MusicData(title, artist, file.absolutePath, albumArt, duration)
    }

    fun isPlaying(): Boolean {
        return musicService?.isPlaying() ?: false
    }

    fun playMusic(index: Int) {
        val musicList = musicList.value
        if (musicList != null && index in musicList.indices) {
            val filePath = musicList[index].filePath
            musicService?.playMusic(filePath)
            saveMusicToHistory(musicList[index])
            updateNotification()
        }
    }

    private fun saveMusicToHistory(musicData: MusicData) {
        val db = dbHelper.writableDatabase
        val cursor = db.query(
            MusicDatabaseHelper.TABLE_NAME,
            arrayOf(MusicDatabaseHelper.COLUMN_FILE_PATH),
            "${MusicDatabaseHelper.COLUMN_FILE_PATH} = ?",
            arrayOf(musicData.filePath),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            // 如果记录存在，更新时间
            val values = ContentValues().apply {
                put(MusicDatabaseHelper.COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000)
            }
            db.update(
                MusicDatabaseHelper.TABLE_NAME,
                values,
                "${MusicDatabaseHelper.COLUMN_FILE_PATH} = ?",
                arrayOf(musicData.filePath)
            )
        } else {
            val values = ContentValues().apply {
                put(MusicDatabaseHelper.COLUMN_TITLE, musicData.title)
                put(MusicDatabaseHelper.COLUMN_ARTIST, musicData.artist)
                put(MusicDatabaseHelper.COLUMN_FILE_PATH, musicData.filePath)
                put(MusicDatabaseHelper.COLUMN_TIMESTAMP, System.currentTimeMillis() / 1000)
            }
            db.insert(MusicDatabaseHelper.TABLE_NAME, null, values)
        }
        cursor.close()
    }

    fun getMusicHistory(): List<MusicHistoryData> {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            MusicDatabaseHelper.TABLE_NAME,
            arrayOf(
                MusicDatabaseHelper.COLUMN_TITLE,
                MusicDatabaseHelper.COLUMN_ARTIST,
                MusicDatabaseHelper.COLUMN_FILE_PATH
            ),
            null, null, null, null,
            "${MusicDatabaseHelper.COLUMN_TIMESTAMP} DESC"
        )
        val musicList = mutableListOf<MusicHistoryData>()
        with(cursor) {
            while (moveToNext()) {
                val title = getString(getColumnIndexOrThrow(MusicDatabaseHelper.COLUMN_TITLE))
                val artist = getString(getColumnIndexOrThrow(MusicDatabaseHelper.COLUMN_ARTIST))
                val filePath = getString(getColumnIndexOrThrow(MusicDatabaseHelper.COLUMN_FILE_PATH))
                musicList.add(MusicHistoryData(title, artist, filePath))
            }
        }
        cursor.close()
        return musicList
    }
    fun clearMusicHistory() {
        val db = dbHelper.writableDatabase
        db.delete(MusicDatabaseHelper.TABLE_NAME, null, null)
    }
    fun playPreviousSong() {
        Log.d("model","prevsong")
        val currentIndex = _currentSongIndex.value ?: 0

        val size = musicList.value?.size ?: 0
        if (size > 0) {
            val prevIndex = if (currentIndex!! > 0) currentIndex - 1 else size - 1
            setCurrentSongIndex(prevIndex)
            playMusic(prevIndex)
        }
    }

    fun playNextSong() {
        Log.d("model","nextsong")
        val currentIndex = _currentSongIndex.value ?: 0
        val size = musicList.value?.size ?: 0
        if (size > 0) {
            val nextIndex = (currentIndex + 1) % size
            setCurrentSongIndex(nextIndex)
            playMusic(nextIndex)
        }
    }

    fun pauseMusic() {
        musicService?.pauseMusic()
        getCurMusic()?.let { musicService?.updateNotification(it) }
    }

    fun resumeMusic() {
        musicService?.resumeMusic()
        getCurMusic()?.let { musicService?.updateNotification(it) }
    }



    fun setCurrentSongIndex(index: Int) {
        _currentSongIndex.value = index
    }

    fun getCurMusic(): MusicData? {
        val index = _currentSongIndex.value
        return if (index != null) {
            musicList.value?.getOrNull(index)
        } else {
            null
        }
    }

    fun getCurProgress() = musicService?.getCurProgress()

    fun setNewProgress(newPosition: Int) {
        musicService?.setNewProgress(newPosition)
    }

    fun createAndShowNotification() {
        musicService?.createAndShowNotification()
    }

    fun updateMusicList(content: String): Boolean {
        val searchResults = originalMusicList.filter {
            it.title.contains(content, ignoreCase = true) || it.artist.contains(content, ignoreCase = true)
        }
        searchMusicList.postValue(searchResults)
        return searchResults.isNotEmpty()
    }


}