package com.fxz.demo.view

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.activity.viewModels
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.fxz.demo.R
import com.fxz.demo.databinding.HistoryFragmentBinding
import com.fxz.demo.model.MusicModel
import com.fxz.demo.viewmodel.MusicViewModel

class HistoryFragment : Fragment() {
    private val viewModel = MusicViewModel()
//    private val viewModel = ViewModelProvider.

    private var _binding: HistoryFragmentBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = HistoryFragmentBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.backButton.setOnClickListener {
            val drawerLayout = activity?.findViewById<DrawerLayout>(R.id.main_layout)
            drawerLayout?.closeDrawer(GravityCompat.START)
        }

        binding.historyRecyclerView.layoutManager = LinearLayoutManager(context)
        val adapter = MusicHistoryAdapter(MusicModel.getMusicHistory()) { filePath ->
            // Handle click event if needed
        }

        binding.clearButton.setOnClickListener {
            viewModel.clearMusicHistory()
            adapter.updateData(emptyList())
        }
        binding.historyRecyclerView.adapter = adapter

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}