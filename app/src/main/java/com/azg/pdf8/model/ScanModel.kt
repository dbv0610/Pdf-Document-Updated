package com.azg.pdf8.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.RoomWarnings
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlin.Long

class Converters {
    @TypeConverter
    fun fromDocumentType(value: DocumentType): String = value.name
    @TypeConverter
    fun toDocumentType(value: String): DocumentType = DocumentType.valueOf(value)
}

enum class DocumentType {
    Doc, Pdf, Excel, Ppt, Image
}

@SuppressWarnings(RoomWarnings.QUERY_MISMATCH)
@Entity(tableName = "RecentDocument")
@TypeConverters(Converters::class)
@Parcelize
data class RecentDocument(
    @PrimaryKey
    @ColumnInfo(name = "mediaId")
    val mediaId: Long = 0L,
    @ColumnInfo(name = "path")
    val path: String = "",
    @ColumnInfo(name = "lastModified")
    val lastModified: Long = 0L,
    @ColumnInfo(name = "lastTimeView")
    val lastTimeView: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "size")
    val size: Long = 0L,
    @ColumnInfo(name = "type")
    var type: DocumentType = DocumentType.Doc
) : Parcelable {
    @Ignore
    var isSelected: Boolean = false

    fun isDefault(): Boolean =
        path.isEmpty() && lastModified == 0L && size == 0L

    fun clone(): RecentDocument =
        copy().also {
            it.isSelected = this.isSelected
            it.type = this.type
        }
}
@Entity(tableName = "FavoriteDocument")
@TypeConverters(Converters::class)
@Parcelize
data class FavoriteDocument(
    @PrimaryKey
    val mediaId: Long = 0L,
    val path: String = "",
    val lastModified: Long = 0L,
    val size: Long = 0L,
    var type: DocumentType = DocumentType.Doc,
) : Parcelable {
    @Ignore
    var isSelected: Boolean = false

    fun isDefault(): Boolean =
        path.isEmpty() && lastModified == 0L && size == 0L

    fun clone(): FavoriteDocument =
        copy().also {
            it.isSelected = this.isSelected
            it.type = this.type
        }
}