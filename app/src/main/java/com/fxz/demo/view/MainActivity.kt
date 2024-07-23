package com.fxz.demo.view
import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.fxz.demo.R
import com.fxz.demo.databinding.ActivityMainBinding
import com.fxz.demo.databinding.LayoutBottomControlBarBinding
import com.fxz.demo.databinding.LayoutTopBinding
import com.fxz.demo.model.MusicData
import com.fxz.demo.model.MusicService
import com.fxz.demo.utils.ACTION_PAUSE_SONG
import com.fxz.demo.utils.ACTION_PLAY_NEW_SONG
import com.fxz.demo.utils.ACTION_PLAY_NEXT_SONG
import com.fxz.demo.utils.ACTION_PLAY_PREV_SONG
import com.fxz.demo.utils.ACTION_RESUME_SONG
import com.fxz.demo.utils.PACKAGE_NAME
import com.fxz.demo.viewmodel.MusicViewModel

class MainActivity : AppCompatActivity() {
    private val viewModel: MusicViewModel by viewModels()

    private lateinit var adapter: MusicAdapter
    private lateinit var binding: ActivityMainBinding
    private lateinit var layoutTopBinding: LayoutTopBinding
    private lateinit var layoutBottomControlBarBinding: LayoutBottomControlBarBinding

    private val REQUEST_MEDIA_AUDIO = 1

    private var broadcastIsBound = false

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d("Main", "Broadcast received")
            if (intent?.action == ACTION_PLAY_NEXT_SONG) {
                playNextSong()
                updateBottomControlBar(viewModel.getCurMusic())
                updateNotification()
            } else if (intent?.action == ACTION_PLAY_PREV_SONG) {
                playPreviousSong()
                updateBottomControlBar(viewModel.getCurMusic())
            } else if (intent?.action == ACTION_RESUME_SONG) {
                resumeMusic()
                Log.d("main","play")
            } else if (intent?.action == ACTION_PAUSE_SONG) {
                pauseMusic()
                Log.d("main","pause")
            } else if (intent?.action == ACTION_PLAY_NEW_SONG) {
                updateBottomControlBar(viewModel.getCurMusic())
                Log.d("main","play new")
            }
        }
    }

    fun registerReceiver() {
        if(broadcastIsBound == false){
            val filter = IntentFilter().apply {
                addAction(ACTION_PLAY_NEXT_SONG)
                addAction(ACTION_PLAY_PREV_SONG)
                addAction(ACTION_PAUSE_SONG)
                addAction(ACTION_RESUME_SONG)
                addAction(ACTION_PLAY_NEW_SONG)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                this.registerReceiver(broadcastReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                this.registerReceiver(broadcastReceiver, filter)
            }
            Log.d("main","register")
            broadcastIsBound = true
        }

    }

    fun createAndShowNotification() {
        Log.d("main","create and show notification")
        viewModel.createAndShowNotification()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        layoutTopBinding = LayoutTopBinding.bind(binding.layoutTop.root)
        layoutBottomControlBarBinding = LayoutBottomControlBarBinding.bind(binding.layoutBottom.root)
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
                startActivity(intent)
            } else {
                // 播放新的歌曲
                viewModel.setCurrentSongIndex(selectedIndex)
                Log.d("main play new",viewModel.getCurrentSongIndex().toString())
                playMusic(selectedIndex)
            }
        }

        binding.mainRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.mainRecyclerView.adapter = adapter

        viewModel.searchMusicFiles.observe(this, Observer { musicFiles ->
            adapter.updateMusicList(musicFiles)
        })

        viewModel.serviceBound.observe(this) { serviceBound ->
            if (serviceBound == true) {
                createAndShowNotification()
            }
        }
        layoutBottomControlBarBinding.btnMainPlayPause
        layoutBottomControlBarBinding.btnMainPlayPause.setOnClickListener {
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

        layoutTopBinding.btnMainSearch.setOnClickListener {
            val content = layoutTopBinding.etMainSearch.text.toString()
            updateMusicList(content)
        }
        layoutTopBinding.btnMainHistory.setOnClickListener{
            binding.mainLayout.openDrawer(GravityCompat.START)
            supportFragmentManager.beginTransaction()
                .replace(R.id.history_fragment, HistoryFragment())
                .commit()
        }
        layoutBottomControlBarBinding.btnMainPrev.setOnClickListener {
            val intent = Intent(ACTION_PLAY_PREV_SONG)
            intent.setPackage(PACKAGE_NAME)
            sendBroadcast(intent)
        }
        layoutBottomControlBarBinding.btnMainNext.setOnClickListener {
            val intent = Intent(ACTION_PLAY_NEXT_SONG)
            intent.setPackage(PACKAGE_NAME)
            sendBroadcast(intent)
        }
        layoutBottomControlBarBinding.ivMainAlbumCover.setOnClickListener {
            if(viewModel.getCurMusic()!=null){
                val intent = Intent(this, MusicDetailActivity::class.java)
                startActivity(intent)
            }
        }
        layoutBottomControlBarBinding.llMainBottomBar.setOnClickListener {
            if(viewModel.getCurMusic()!=null){
                val intent = Intent(this, MusicDetailActivity::class.java)
                startActivity(intent)
            }
        }
        if (!isNotificationEnable()) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("通知未开启")
            builder.setMessage("音乐通知未开启，是否前往设置开启？")

            builder.setPositiveButton("是") { dialog, which ->
                val intent = Intent()
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
                intent.putExtra("app_package", PACKAGE_NAME)
                intent.putExtra("android.provider.extra.APP_PACKAGE", PACKAGE_NAME)
                intent.putExtra("app_uid", applicationInfo.uid)
                startActivity(intent)
            }

            builder.setNegativeButton("否") { dialog, which ->
                dialog.dismiss()
            }

            builder.show()
        }

        viewModel.bindService(this@MainActivity)
        registerReceiver()

        initializeDB(this)

    }

    private fun initializeDB(context: Context) {
        viewModel.initializeDB(context)
    }

    private fun updateMusicList(content: String) {
        viewModel.updateMusicList(content)
    }

    private fun playMusic(index: Int) {
        viewModel.playMusic(index)
        updateControlButtons(true)
        updateBottomControlBar(viewModel.getCurMusic())
        layoutBottomControlBarBinding.btnMainPlayPause.setImageResource(R.drawable.ic_pause)
    }

    private fun pauseMusic() {
        viewModel.pauseMusic()
        layoutBottomControlBarBinding.btnMainPlayPause.setImageResource(R.drawable.ic_play)
    }

    private fun resumeMusic() {
        viewModel.resumeMusic()
        layoutBottomControlBarBinding.btnMainPlayPause.setImageResource(R.drawable.ic_pause)
    }

    private fun playPreviousSong() {
        viewModel.playPreviousSong()
    }

    private fun playNextSong() {
        viewModel.playNextSong()
    }

    private fun updateBottomControlBar(music: MusicData?) {
        Log.d("Main","update buttom")
        if (music != null) {
            layoutBottomControlBarBinding.tvMainSongTitle.text = music.title
            layoutBottomControlBarBinding.tvMainSongArtist.text = music.artist
            setAlbumCover(music.filePath)
            if(isPlaying()){
                layoutBottomControlBarBinding.btnMainPlayPause.setImageResource(R.drawable.ic_pause)
            }else {
                layoutBottomControlBarBinding.btnMainPlayPause.setImageResource(R.drawable.ic_play)
            }
        } else {
//            layoutBottomControlBarBinding.tvMainSongTitle.text = ""
//            songArtist.text = ""
//            albumCover.setImageResource(R.drawable.ic_album_placeholder)
//            layoutBottomControlBarBinding.btnMainPlayPause.setImageResource(R.drawable.ic_play)
        }
    }

    private fun setAlbumCover(filePath: String) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(filePath)
            val art = retriever.embeddedPicture
            if (art != null) {
                val bitmap = BitmapFactory.decodeByteArray(art, 0, art.size)
                layoutBottomControlBarBinding.ivMainAlbumCover.setImageBitmap(bitmap)
            } else {
                layoutBottomControlBarBinding.ivMainAlbumCover.setImageResource(R.drawable.ic_album_placeholder)
            }
        } catch (e: Exception) {
            layoutBottomControlBarBinding.ivMainAlbumCover.setImageResource(R.drawable.ic_album_placeholder)
        } finally {
            retriever.release()
        }
    }

    private fun updateControlButtons(enable: Boolean) {
        layoutBottomControlBarBinding.btnMainPlayPause.isEnabled = enable
        layoutBottomControlBarBinding.btnMainPrev.isEnabled = enable
        layoutBottomControlBarBinding.btnMainNext.isEnabled = enable
    }

    //请求权限
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_MEDIA_AUDIO && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d("Permission", "READ_MEDIA_AUDIO permission granted")
            viewModel.loadMusicFiles()
        } else {
            Log.d("Permission", "READ_MEDIA_AUDIO permission denied") }
    }

    private fun isPlaying():Boolean {
        return viewModel.isPlaying()
    }

    private fun updateNotification() {
        viewModel.updateNotification()
    }

    private fun isNotificationEnable(): Boolean {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        return notificationManager.areNotificationsEnabled()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, MusicService::class.java))
    }

}
