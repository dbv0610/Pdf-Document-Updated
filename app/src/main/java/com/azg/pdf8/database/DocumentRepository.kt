package com.azg.pdf8.database

import com.azg.pdf8.model.DocumentModel
import kotlinx.coroutines.flow.Flow

interface DocumentRepository {
    fun getAllDocuments(): Flow<List<DocumentModel>>
    fun getFavoriteDocuments(): Flow<List<DocumentModel>>
    suspend fun insert(document: DocumentModel)
    suspend fun insertAll(documents: List<DocumentModel>)
    suspend fun update(document: DocumentModel)
    suspend fun delete(document: DocumentModel)
    suspend fun setFavorite(mediaId: Long, isFavorite: Boolean)
    suspend fun clearAll()
}