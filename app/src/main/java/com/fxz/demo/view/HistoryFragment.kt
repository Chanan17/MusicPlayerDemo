package com.fxz.demo.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.fxz.demo.R
import com.fxz.demo.databinding.FragmentHistoryBinding
import com.fxz.demo.utils.ACTION_PLAY_NEW_SONG
import com.fxz.demo.utils.PACKAGE_NAME
import com.fxz.demo.viewmodel.MusicViewModel

class HistoryFragment : Fragment() {
    private lateinit var adapter: MusicHistoryAdapter
    private val viewModel = MusicViewModel()
//    private val viewModel = ViewModelProvider.

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val view = binding.root

        //历史播放列表
        binding.historyRecyclerView.layoutManager = LinearLayoutManager(context)
        adapter = MusicHistoryAdapter(viewModel.getMusicHistory()) { filePath ->
            Log.d("history",filePath)
            val selectedIndex = viewModel.musicFiles.value?.indexOfFirst { it.filePath == filePath } ?: 0
            viewModel.setCurrentSongIndex(selectedIndex)
            viewModel.playMusic(selectedIndex)

            val intent = Intent(ACTION_PLAY_NEW_SONG)
            intent.setPackage(PACKAGE_NAME)
            requireContext().sendBroadcast(intent)
            Log.d("history","send new")
        }
        binding.historyRecyclerView.adapter = adapter

        //清空按钮
        binding.imgbtnHistClear.setOnClickListener {
            viewModel.clearMusicHistory()
            adapter.updateData(emptyList())
        }
        //返回按钮
        binding.imgbtnHistBack.setOnClickListener {
            val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.main_layout)
            drawerLayout?.closeDrawer(GravityCompat.START)
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}