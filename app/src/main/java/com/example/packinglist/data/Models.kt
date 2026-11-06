package com.example.packinglist.data

import androidx.room.ColumnInfo
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
    @ColumnInfo(defaultValue = "0")
    val startDate: Long = TripDates.todayUtcMillis(),
    @ColumnInfo(defaultValue = "0")
    val endDate: Long = TripDates.todayUtcMillis(),
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
    /** Kleidungsstück: bekommt in der Packliste einen Stückzahl-Zähler. */
    @ColumnInfo(defaultValue = "0")
    val isClothing: Boolean = false,
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
 * Artikelnamen aus dem Startkatalog, die als Kleidung gelten. Neue Artikel
 * lassen sich im Artikel-Dialog zusätzlich manuell als Kleidung markieren.
 */
val CLOTHING_NAMES: Set<String> = setOf(
    "hemd",
    "hemden",
    "polo",
    "klamotten",
    "badehose",
    "regenjacke",
    "schuhe",
    "hausschuhe",
    "mütze",
    "kappe",
    "gant"
)

fun isClothingName(name: String): Boolean = name.trim().lowercase() in CLOTHING_NAMES

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
    val packedAt: Long? = null,
    /** Gezählte Stücke – nur für Kleidungsstücke relevant. */
    @ColumnInfo(defaultValue = "0")
    val count: Int = 0,
    /** Artikel wird bewusst nicht mitgenommen. */
    @ColumnInfo(defaultValue = "0")
    val isSkipped: Boolean = false
)

/** Dreifacher Packstatus eines Artikels. */
enum class PackStatus {
    OPEN,
    PACKED,
    SKIPPED
}

/**
 * Ein Artikel zusammen mit seinem Packzustand für die aktuelle Reise.
 */
data class PackEntry(
    @Embedded val article: Article,
    val isPacked: Boolean = false,
    val packedAt: Long? = null,
    val count: Int = 0,
    val isSkipped: Boolean = false
)

val PackEntry.status: PackStatus
    get() = when {
        isSkipped -> PackStatus.SKIPPED
        isPacked -> PackStatus.PACKED
        else -> PackStatus.OPEN
    }

data class TripCount(
    val tripId: Long,
    val total: Int,
    val packed: Int
)
