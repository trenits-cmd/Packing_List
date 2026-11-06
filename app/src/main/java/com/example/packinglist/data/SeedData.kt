package com.example.packinglist.data

import android.content.Context
import com.example.packinglist.R

/**
 * Liest den Artikelkatalog aus res/raw/articles.csv.
 * Die CSV-Spalten sind: Artikel, Reiseart, Saison, Wichtigkeit.
 */
object SeedData {

    fun load(context: Context): List<Article> {
        val raw = context.resources.openRawResource(R.raw.articles)
            .bufferedReader(Charsets.UTF_8)
            .use { it.readText() }

        return raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .drop(1) // Kopfzeile
            .mapIndexedNotNull { index, line ->
                val columns = line.split(",", limit = 4)
                if (columns.size < 4) return@mapIndexedNotNull null

                val name = columns[0].trim().removeSurrounding("\"")
                if (name.isBlank()) return@mapIndexedNotNull null

                val tripType = when (columns[1].trim()) {
                    "Dienstreise" -> TripType.BUSINESS
                    else -> TripType.PRIVATE
                }
                val availability = when (columns[2].trim().lowercase()) {
                    "immer", "always" -> Availability.ALWAYS
                    "frühling", "fruehling", "spring" -> Availability.SPRING
                    "sommer", "summer" -> Availability.SUMMER
                    "herbst", "autumn" -> Availability.AUTUMN
                    "winter" -> Availability.WINTER
                    else -> Availability.ALWAYS
                }
                val importance = columns[3].trim().toIntOrNull()?.coerceIn(1, 3) ?: 1

                Article(
                    name = name,
                    tripType = tripType,
                    availability = availability,
                    importance = importance,
                    isClothing = isClothingName(name),
                    sortOrder = index
                )
            }
            .toList()
    }
}
