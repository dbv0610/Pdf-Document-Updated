package com.azg.pdf8.viewmodel

import com.azg.pdf8.database.RecentDao
import com.azg.pdf8.database.DocumentRepository
import com.azg.pdf8.model.RecentDocument
import kotlinx.coroutines.flow.Flow

class DocumentRepositoryImpl(
    private val dao: RecentDao
) : DocumentRepository {
    override fun getAllDocuments(): Flow<List<RecentDocument>> = dao.getAll()


    override suspend fun insert(document: RecentDocument) {
        dao.insert(document)
    }

    override suspend fun insertAll(documents: List<RecentDocument>) {
        dao.insertAll(documents)
    }

    override suspend fun update(document: RecentDocument) {
        dao.update(document)
    }

    override suspend fun delete(document: RecentDocument) {
        dao.delete(document)
    }

    override suspend fun setFavorite(mediaId: Long, isFavorite: Boolean) {

    }

    override suspend fun clearAll() {
        dao.clearAll()
    }
}