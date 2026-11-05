package com.example.packinglist.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface PackingDao {

    // --- Reisen -------------------------------------------------------------

    @Query("SELECT * FROM trips ORDER BY createdAt DESC")
    fun observeTrips(): Flow<List<Trip>>

    @Query(
        "SELECT * FROM trips " +
            "WHERE LOWER(name) = LOWER(:name) AND tripType = :tripType AND season = :season " +
            "LIMIT 1"
    )
    suspend fun findTrip(name: String, tripType: TripType, season: Season): Trip?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)

    @Delete
    suspend fun deleteTrip(trip: Trip)

    // --- Artikel-Katalog ----------------------------------------------------

    @Query(
        "SELECT * FROM articles " +
            "ORDER BY tripType ASC, availability ASC, importance ASC, sortOrder ASC, " +
            "name COLLATE NOCASE ASC"
    )
    fun observeArticles(): Flow<List<Article>>

    @Query("SELECT * FROM articles WHERE id = :articleId")
    suspend fun getArticle(articleId: Long): Article?

    @Query(
        "SELECT * FROM articles " +
            "WHERE LOWER(name) = LOWER(:name) AND tripType = :tripType " +
            "AND availability = :availability LIMIT 1"
    )
    suspend fun findArticle(
        name: String,
        tripType: TripType,
        availability: Availability
    ): Article?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticle(article: Article): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertArticles(articles: List<Article>)

    @Update
    suspend fun updateArticle(article: Article)

    @Delete
    suspend fun deleteArticle(article: Article)

    @Query("SELECT COUNT(*) FROM articles")
    suspend fun countArticles(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM articles")
    suspend fun nextArticleSortOrder(): Int

    @Query("DELETE FROM packing_states WHERE articleId = :articleId")
    suspend fun clearStatesForArticle(articleId: Long)

    // --- Packliste ----------------------------------------------------------

    /**
     * Alle Artikel, die zu Reiseart und Jahreszeit der Reise passen, zusammen
     * mit dem Packzustand dieser Reise. Artikel mit "Immer" sind immer dabei.
     */
    @Query(
        """
        SELECT a.*, COALESCE(s.isPacked, 0) AS isPacked, s.packedAt AS packedAt
        FROM articles a
        LEFT JOIN packing_states s
            ON s.articleId = a.id AND s.tripId = :tripId
        WHERE a.tripType = :tripType
            AND (a.availability = 'ALWAYS' OR a.availability = :season)
        ORDER BY a.importance ASC, a.sortOrder ASC, a.name COLLATE NOCASE ASC
        """
    )
    fun observePackEntries(
        tripId: Long,
        tripType: TripType,
        season: Season
    ): Flow<List<PackEntry>>

    @Upsert
    suspend fun upsertPackingState(state: PackingState)

    @Query("DELETE FROM packing_states WHERE tripId = :tripId")
    suspend fun resetPacked(tripId: Long)

    @Query(
        """
        SELECT t.id AS tripId,
            (
                SELECT COUNT(*) FROM articles a
                WHERE a.tripType = t.tripType
                    AND (a.availability = 'ALWAYS' OR a.availability = t.season)
            ) AS total,
            (
                SELECT COUNT(*) FROM packing_states s
                JOIN articles a ON a.id = s.articleId
                WHERE s.tripId = t.id AND s.isPacked = 1
                    AND a.tripType = t.tripType
                    AND (a.availability = 'ALWAYS' OR a.availability = t.season)
            ) AS packed
        FROM trips t
        """
    )
    fun observeTripCounts(): Flow<List<TripCount>>
}
