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
    version = 6,
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

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE articles ADD COLUMN isClothing INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "UPDATE articles SET isClothing = 1 WHERE name IN (" +
                        "'Hemd','Hemden','Polo','Klamotten','Badehose','Regenjacke'," +
                        "'Schuhe','Hausschuhe','Mütze','Kappe','Gant')"
                )
                db.execSQL(
                    "ALTER TABLE packing_states ADD COLUMN count INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE packing_states ADD COLUMN isSkipped INTEGER NOT NULL DEFAULT 0"
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
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                    // Version 1 enthielt nur die alten Beispielreisen.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
