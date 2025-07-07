package com.azg.pdf8.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ContentWithPage(val page: Int, val content: String,var expand: Boolean = false) :
    Parcelable