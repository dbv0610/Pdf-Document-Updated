package com.azg.pdf8.model

import android.graphics.Bitmap
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class CreatePdf(
    val defId: Int = 0,
    var picture: Bitmap,
    var indexOfList: Int = 0,
    var oldPath: String = ""
) : Parcelable