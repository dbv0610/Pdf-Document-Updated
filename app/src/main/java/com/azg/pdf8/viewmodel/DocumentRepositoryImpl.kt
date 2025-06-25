package com.azg.pdf8.viewmodel

import com.azg.pdf8.database.DocumentDao
import com.azg.pdf8.database.DocumentRepository
import com.azg.pdf8.model.DocumentModel
import kotlinx.coroutines.flow.Flow

class DocumentRepositoryImpl(
    private val dao: DocumentDao
) : DocumentRepository {
    override fun getAllDocuments(): Flow<List<DocumentModel>> = dao.getAll()

    override fun getFavoriteDocuments(): Flow<List<DocumentModel>> = dao.getFavorites()

    override suspend fun insert(document: DocumentModel) {
        dao.insert(document)
    }

    override suspend fun insertAll(documents: List<DocumentModel>) {
        dao.insertAll(documents)
    }

    override suspend fun update(document: DocumentModel) {
        dao.update(document)
    }

    override suspend fun delete(document: DocumentModel) {
        dao.delete(document)
    }

    override suspend fun setFavorite(mediaId: Long, isFavorite: Boolean) {
        dao.setFavorite(mediaId, isFavorite)
    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}