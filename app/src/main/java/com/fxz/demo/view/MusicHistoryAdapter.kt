package com.fxz.demo.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fxz.demo.databinding.ItemMusicHistoryBinding
import com.fxz.demo.model.MusicHistoryData

class MusicHistoryAdapter(
    private var musicHistoryList: List<MusicHistoryData>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<MusicHistoryAdapter.MusicHistoryViewHolder>() {

    inner class MusicHistoryViewHolder(private val binding: ItemMusicHistoryBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(musicHistoryData: MusicHistoryData) {
            binding.title.text = musicHistoryData.title
            binding.artist.text = musicHistoryData.artist
            binding.root.setOnClickListener {
                onItemClick(musicHistoryData.filePath)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicHistoryViewHolder {
        val binding = ItemMusicHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MusicHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicHistoryViewHolder, position: Int) {
        holder.bind(musicHistoryList[position])
    }

    override fun getItemCount(): Int = musicHistoryList.size

    fun updateData(newMusicHistoryList: List<MusicHistoryData>) {
        musicHistoryList = newMusicHistoryList
        notifyDataSetChanged()
    }
}