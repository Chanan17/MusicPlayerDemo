package com.fxz.demo.model

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable

data class Music(
    val title: String,
    val artist: String,
    val filePath: String,
    var albumArt: Bitmap? = null
)