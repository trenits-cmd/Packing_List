package com.example.packinglist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [Trip::class, Article::class, PackingState::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PackingDatabase : RoomDatabase() {

    abstract fun packingDao(): PackingDao

    companion object {
        @Volatile
        private var INSTANCE: PackingDatabase? = null

        fun get(context: Context): PackingDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PackingDatabase::class.java,
                    "packing-list.db"
                )
                    // Version 1 enthielt nur die alten Beispielreisen.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
