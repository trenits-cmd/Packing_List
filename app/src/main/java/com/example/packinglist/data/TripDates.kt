package com.example.packinglist.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Hilfsfunktionen für den Reisezeitraum. Die Daten werden als UTC-Mitternacht
 * gespeichert, damit die Material-DatePicker-Werte verlustfrei passen.
 */
object TripDates {

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy")

    fun todayUtcMillis(): Long =
        LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    fun toLocalDate(utcMillis: Long): LocalDate =
        Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()

    fun format(utcMillis: Long): String =
        toLocalDate(utcMillis).format(DATE_FORMAT)

    fun formatRange(startDate: Long, endDate: Long): String =
        "${format(startDate)} – ${format(endDate)}"

    /** Beide Grenztage zählen mit: 01.07. bis 07.07. = 7 Tage. */
    fun daysInclusive(startDate: Long, endDate: Long): Int {
        val days = ChronoUnit.DAYS
            .between(toLocalDate(startDate), toLocalDate(endDate))
            .toInt() + 1
        return days.coerceAtLeast(1)
    }

    fun daysLabel(days: Int): String = if (days == 1) "1 Tag" else "$days Tage"
}
