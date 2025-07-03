package com.azg.pdf8.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

class Converters {
    @TypeConverter
    fun fromDocumentType(varue: DocumentType): String = varue.name
    @TypeConverter
    fun toDocumentType(varue: String): DocumentType = DocumentType.valueOf(varue)
}

enum class DocumentType {
    Doc, Pdf, Excel, Ppt,Image
}
@Entity(tableName = "DocumentData")
@Parcelize
data class DocumentModel(
    @PrimaryKey
    val mediaId: Long = 0L,
    val path: String = "",
    val lastModified: Long = 0L,
    val size: Long = 0L,
    @IgnoredOnParcel
    var isSelected: Boolean = false,
    var isInFavorite: Boolean = false,
    var type: DocumentType = DocumentType.Doc,
    var duration: Long = 0L
) : Parcelable {
    fun isDefault(): Boolean {
        return path == "" && lastModified == 0L && size == 0L
    }

    fun clone(): DocumentModel {
        return DocumentModel(
            mediaId      = this.mediaId,
            path         = this.path,
            lastModified = this.lastModified,
            size         = this.size,
        ).apply {
            isSelected     = this@DocumentModel.isSelected
            isInFavorite   = this@DocumentModel.isInFavorite
            type           = this@DocumentModel.type
            duration       = this@DocumentModel.duration
        }
    }
}
