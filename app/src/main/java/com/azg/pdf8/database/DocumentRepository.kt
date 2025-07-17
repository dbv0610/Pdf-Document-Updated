package com.azg.pdf8.database

import com.azg.pdf8.model.RecentDocument
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun getAllDocuments(): Flow<List<RecentDocument>>
    suspend fun insert(document: RecentDocument)
    suspend fun insertAll(documents: List<RecentDocument>)
    suspend fun update(document: RecentDocument)
    suspend fun delete(document: RecentDocument)
    suspend fun setFavorite(mediaId: Long, isFavorite: Boolean)
    suspend fun clearAll()
}