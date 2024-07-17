package com.fxz.demo.model

import android.graphics.Bitmap

data class MusicData(
    val title: String,
    val artist: String,
    val filePath: String,
    var albumArt: Bitmap? = null,
    val duration: Int
)