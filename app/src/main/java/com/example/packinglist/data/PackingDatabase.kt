package com.example.packinglist.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Trip::class, Article::class, PackingState::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PackingDatabase : RoomDatabase() {

    abstract fun packingDao(): PackingDao

    companion object {
        @Volatile
        private var INSTANCE: PackingDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN photoUrl TEXT")
                db.execSQL("ALTER TABLE articles ADD COLUMN photoRemotePath TEXT")
            }
        }

        fun get(context: Context): PackingDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PackingDatabase::class.java,
                    "packing-list.db"
                )
                    .addMigrations(MIGRATION_2_3)
                    // Version 1 enthielt nur die alten Beispielreisen.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
