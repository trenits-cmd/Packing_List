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
    version = 4,
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE trips ADD COLUMN startDate INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE trips ADD COLUMN endDate INTEGER NOT NULL DEFAULT 0"
                )
                val today = TripDates.todayUtcMillis()
                db.execSQL(
                    "UPDATE trips SET startDate = ?, endDate = ?",
                    arrayOf<Any>(today, today)
                )
            }
        }

        fun get(context: Context): PackingDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    PackingDatabase::class.java,
                    "packing-list.db"
                )
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    // Version 1 enthielt nur die alten Beispielreisen.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
