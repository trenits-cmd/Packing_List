package com.example.packinglist.data

/**
 * Jahreszeit einer Reise. "Immer" ist bewusst keine Reise-Jahreszeit,
 * sondern nur für die Verfügbarkeit von Artikeln erlaubt.
 */
enum class Season(val label: String, val emoji: String) {
    SPRING("Frühling", "🌸"),
    SUMMER("Sommer", "☀️"),
    AUTUMN("Herbst", "🍂"),
    WINTER("Winter", "❄️")
}

/**
 * Für welche Jahreszeit ein Artikel gedacht ist.
 * [ALWAYS] bedeutet: unabhängig von der Jahreszeit einpacken.
 */
enum class Availability(val label: String, val emoji: String) {
    ALWAYS("Immer", "♾️"),
    SPRING("Frühling", "🌸"),
    SUMMER("Sommer", "☀️"),
    AUTUMN("Herbst", "🍂"),
    WINTER("Winter", "❄️");

    fun matches(season: Season): Boolean = this == ALWAYS || name == season.name
}
