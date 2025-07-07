package com.azg.pdf8.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.azg.pdf8.model.Converters
import com.azg.pdf8.model.FavoriteDocument
import com.azg.pdf8.model.RecentDocument

@Database(
    entities = [RecentDocument::class, FavoriteDocument::class],
    version = 1, exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): RecentDao
    abstract fun favoriteDao(): FavoriteDao
}