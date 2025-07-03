package com.azg.pdf8.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class FolderItem(
    var folderName: String = "All",
    var previewPath: String = "",
    var listData: MutableList<DocumentModel> = mutableListOf<DocumentModel>(),
    var itemCount: Int = 0
) : Parcelable
