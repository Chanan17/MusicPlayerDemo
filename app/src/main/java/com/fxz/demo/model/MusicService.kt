package com.fxz.demo.model

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.fxz.demo.R
import com.fxz.demo.utils.ACTION_PAUSE_SONG
import com.fxz.demo.utils.ACTION_PLAY_NEXT_SONG
import com.fxz.demo.utils.ACTION_PLAY_PREV_SONG
import com.fxz.demo.utils.ACTION_RESUME_SONG
import java.io.IOException

class MusicService : Service() {

    private var remoteViews = RemoteViews("com.fxz.demo", R.layout.notification2)
    private var mediaPlayer: MediaPlayer? = null
    private var currentSongPath: String? = null
    private val binder = LocalBinder()

    private val CHANNEL_ID = "mydemo"
    private val NOTIFICATION_ID = 1

//    private var musicFilePaths: List<String> = emptyList()
//    private var currentSongIndex: Int = -1

    override fun onCreate() {
        super.onCreate()
    }


    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "ACTION_PREV" -> {
                val intent = Intent(ACTION_PLAY_PREV_SONG)
                intent.setPackage("com.fxz.demo")
                sendBroadcast(intent)

            }
            "ACTION_PLAY_PAUSE" -> {
                if(isPlaying()) {
                    mediaPlayer?.pause()
                    remoteViews.setImageViewResource(R.id.play_pause_button, R.drawable.ic_play)
                }else {
                    mediaPlayer?.start()
                    remoteViews.setImageViewResource(R.id.play_pause_button, R.drawable.ic_pause)
                }
            }
            "ACTION_NEXT" -> {
                val intent = Intent(ACTION_PLAY_NEXT_SONG)
                intent.setPackage("com.fxz.demo")
                sendBroadcast(intent)
            }
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d("service","create notification")
            val name = "音乐播放器通知"
            val descriptionText = "音乐播放器控制"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            notificationManager.createNotificationChannel(channel)


//            remoteViews.setTextViewText(R.id.tv_song_title,"!11111111")


        }
//        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
////                .setContentTitle("This is content title")
//            .setSmallIcon(R.drawable.small_icon)
//            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
//            .setDefaults(0)
//            .setAutoCancel(true)
//            .setCustomContentView(remoteViews)
//
////                .setCustomContentView(remoteViews)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .build()
//        startForeground(2, notification)
//        notificationManager.notify(2,notification)

    }

    private fun showNotification() {

        // 设置 RemoteViews 内容
//        remoteViews.setImageViewResource(R.id.album_cover, R.drawable.ic_album_placeholder)
        // 设置按钮点击事件
        val prevIntent = Intent(this, MusicService::class.java).apply {
            action = "ACTION_PREV"
        }
        val prevPendingIntent = PendingIntent.getService(this, 0, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        remoteViews.setOnClickPendingIntent(R.id.prev_button, prevPendingIntent)

        val playPauseIntent = Intent(this, MusicService::class.java).apply {
            action = "ACTION_PLAY_PAUSE"
        }
        val playPausePendingIntent = PendingIntent.getService(this, 0, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        remoteViews.setOnClickPendingIntent(R.id.play_pause_button, playPausePendingIntent)

        val nextIntent = Intent(this, MusicService::class.java).apply {
            action = "ACTION_NEXT"
        }
        val nextPendingIntent = PendingIntent.getService(this, 0, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        remoteViews.setOnClickPendingIntent(R.id.next_button, nextPendingIntent)

        // 创建通知
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
//                .setContentTitle("This is content title")
            .setSmallIcon(R.drawable.small_icon)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)

//                .setCustomContentView(remoteViews)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        startForeground(NOTIFICATION_ID, notification)
        Log.d("service","show notification")
    }

    fun playMusic(songPath: String) {
        if (currentSongPath != songPath) {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                try {
                    Log.d("MusicService","playMusic()")
                    setDataSource(songPath)
                    prepare()
                    start()
                    setOnCompletionListener {
                        Log.d("Service","end")
                        val intent = Intent(ACTION_PLAY_NEXT_SONG)
                        intent.setPackage("com.fxz.demo")
                        sendBroadcast(intent)
                        Log.d("MusicService","auto next broadcastsent")
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            currentSongPath = songPath

        } else {
            resumeMusic()
        }
    }

    fun pauseMusic() {
        Log.d("MusicService","pauseMusic()")
        val intent = Intent(ACTION_PAUSE_SONG)
        intent.setPackage("com.fxz.demo")
        sendBroadcast(intent)
        mediaPlayer?.pause()
    }

    fun resumeMusic() {
        Log.d("MusicService","resumeMusic()")
        val intent = Intent(ACTION_RESUME_SONG)
        intent.setPackage("com.fxz.demo")
        sendBroadcast(intent)
        mediaPlayer?.start()
    }

    fun isPlaying(): Boolean {
        return mediaPlayer?.isPlaying == true
    }

//    fun playNextSong() {
//        Log.d("MusicService","next1()")
//        if (musicFilePaths.isNotEmpty()) {
//            Log.d("MusicService","next2()")
//            currentSongIndex = (currentSongIndex + 1) % musicFilePaths.size
//            playMusic(musicFilePaths[currentSongIndex])
//        }
//    }
//
//    fun playPreviousSong() {
//        if (musicFilePaths.isNotEmpty()) {
//            currentSongIndex = if (currentSongIndex > 0) currentSongIndex - 1 else musicFilePaths.size - 1
//            playMusic(musicFilePaths[currentSongIndex])
//        }
//    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }

    fun getCurProgress():Int {
        return mediaPlayer?.currentPosition ?: 0
    }

    fun setNewProgress(newPosition: Int) {
        mediaPlayer?.seekTo(newPosition)
    }

    fun createAndShowNotification() {
        Log.d("service","create and show notification")
        createNotificationChannel()
//        showNotification()
    }

    fun updateNotification(curMusic: MusicData) {
        remoteViews.setTextViewText(R.id.song_title, curMusic.title)
        remoteViews.setTextViewText(R.id.music_artist, curMusic.artist)
        if (curMusic.albumArt != null) {
            remoteViews.setImageViewBitmap(R.id.album_cover, curMusic.albumArt)
        } else {
            remoteViews.setImageViewResource(R.id.album_cover, R.drawable.ic_album_placeholder)
        }

        val playPauseIcon = if (isPlaying()) R.drawable.ic_pause else R.drawable.ic_play
        remoteViews.setImageViewResource(R.id.play_pause_button, playPauseIcon)

    }
}
