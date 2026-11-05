package com.example.packinglist.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "trips",
    indices = [
        Index(value = ["name", "tripType", "season"], unique = true)
    ]
)
data class Trip(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val tripType: TripType,
    val season: Season,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Ein Artikel im zentralen Katalog. Er gilt für eine Reiseart und
 * eine Jahreszeit (oder "Immer") und kann ein eigenes Foto haben.
 */
@Entity(
    tableName = "articles",
    indices = [
        Index(value = ["tripType", "availability"]),
        Index(value = ["name", "tripType", "availability"], unique = true)
    ]
)
data class Article(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val tripType: TripType,
    val availability: Availability,
    /** 1 = sehr wichtig, 2 = wichtig, 3 = optional */
    val importance: Int = 1,
    /** Absoluter Pfad zu einem lokal gespeicherten Foto. */
    val photoPath: String? = null,
    /** Öffentliche GitHub-Download-URL des Fotos. */
    val photoUrl: String? = null,
    /** Pfad im GitHub-Repository, z. B. article_photos/foto.jpg. */
    val photoRemotePath: String? = null,
    val sortOrder: Int = 0
)

val Article.importanceLabel: String
    get() = when (importance) {
        1 -> "Sehr wichtig"
        2 -> "Wichtig"
        else -> "Optional"
    }

/**
 * Packzustand eines Artikels für eine konkrete Reise.
 */
@Entity(
    tableName = "packing_states",
    primaryKeys = ["tripId", "articleId"],
    foreignKeys = [
        ForeignKey(
            entity = Trip::class,
            parentColumns = ["id"],
            childColumns = ["tripId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Article::class,
            parentColumns = ["id"],
            childColumns = ["articleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("tripId"), Index("articleId")]
)
data class PackingState(
    val tripId: Long,
    val articleId: Long,
    val isPacked: Boolean = false,
    val packedAt: Long? = null
)

/**
 * Ein Artikel zusammen mit seinem Packzustand für die aktuelle Reise.
 */
data class PackEntry(
    @Embedded val article: Article,
    val isPacked: Boolean = false,
    val packedAt: Long? = null
)

data class TripCount(
    val tripId: Long,
    val total: Int,
    val packed: Int
)
