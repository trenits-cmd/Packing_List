package com.example.packinglist.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow

class PackingRepository(
    private val dao: PackingDao,
    private val preferences: SharedPreferences,
    private val appContext: Context
) {

    fun observeTrips(): Flow<List<Trip>> = dao.observeTrips()

    fun observeTripCounts(): Flow<List<TripCount>> = dao.observeTripCounts()

    fun observeArticles(): Flow<List<Article>> = dao.observeArticles()

    fun observePackEntries(trip: Trip): Flow<List<PackEntry>> =
        dao.observePackEntries(
            tripId = trip.id,
            tripType = trip.tripType,
            season = trip.season
        )

    /**
     * Importiert den CSV-Artikelkatalog beim ersten Start der neuen
     * Datenbankversion. Alte Beispielreisen werden durch die destructive
     * Migration der Datenbankversion 2 entfernt.
     */
    suspend fun ensureSeeded() {
        if (preferences.getBoolean(KEY_SEEDED, false)) return
        if (dao.countArticles() == 0) {
            dao.insertArticles(SeedData.load(appContext))
        }
        preferences.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    private companion object {
        const val KEY_SEEDED = "seeded_articles_v2"
    }
}
