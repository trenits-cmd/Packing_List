package com.example.packinglist.data

/**
 * Art der Reise. Wird als Name in der Datenbank gespeichert.
 */
enum class TripType(val label: String, val shortLabel: String) {
    BUSINESS("Dienstreise", "Dienstreise"),
    PRIVATE("Urlaub (privat)", "Urlaub")
}
