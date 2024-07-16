package com.fxz.demo.view

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fxz.demo.R
import com.fxz.demo.model.Music
import com.fxz.demo.databinding.ItemMusicBinding

class MusicAdapter(
    private var musicList: List<Music>,
    private val onMusicSelected: (String) -> Unit
) : RecyclerView.Adapter<MusicAdapter.MusicViewHolder>() {

    inner class MusicViewHolder(private val binding: ItemMusicBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(musicFile: Music) {
            binding.musicTitle.text = musicFile.title
            binding.musicArtist.text = musicFile.artist
            if (musicFile.albumArt != null) {
                binding.albumCover.setImageBitmap(musicFile.albumArt)
            } else {
                binding.albumCover.setImageResource(R.drawable.ic_album_placeholder)
            }
            binding.root.setOnClickListener {
                onMusicSelected(musicFile.filePath)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MusicViewHolder {
        val binding = ItemMusicBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MusicViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MusicViewHolder, position: Int) {
        holder.bind(musicList[position])
    }

    override fun getItemCount(): Int {
        return musicList.size
    }

    fun updateMusicList(newMusicList: List<Music>) {
        musicList = newMusicList
        notifyDataSetChanged()
    }
}
