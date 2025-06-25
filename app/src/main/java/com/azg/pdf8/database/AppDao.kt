package com.azg.pdf8.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.azg.pdf8.model.DocumentModel
import kotlinx.coroutines.flow.Flow
@Dao
interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: DocumentModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(documents: List<DocumentModel>)

    @Query("SELECT * FROM DocumentData")
    fun getAll(): Flow<List<DocumentModel>>

    @Query("SELECT * FROM DocumentData WHERE isInFavorite = 1")
    fun getFavorites(): Flow<List<DocumentModel>>

    @Delete
    suspend fun delete(document: DocumentModel)

    @Update
    suspend fun update(document: DocumentModel)

    @Query("UPDATE DocumentData SET isInFavorite = :favorite WHERE mediaId = :mediaId")
    suspend fun setFavorite(mediaId: Long, favorite: Boolean)

    @Query("DELETE FROM DocumentData")
    suspend fun clearAll()
}