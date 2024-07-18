package com.fxz.demo.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.fxz.demo.databinding.HistoryFragmentBinding

class HistoryFragment : Fragment() {

    private lateinit var binding: HistoryFragmentBinding
    private var historyList = mutableListOf<String>()

}