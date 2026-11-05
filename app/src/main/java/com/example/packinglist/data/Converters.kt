package com.example.packinglist.data

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun tripTypeToString(value: TripType): String = value.name

    @TypeConverter
    fun stringToTripType(value: String): TripType =
        TripType.entries.firstOrNull { it.name == value } ?: TripType.PRIVATE

    @TypeConverter
    fun seasonToString(value: Season): String = value.name

    @TypeConverter
    fun stringToSeason(value: String): Season =
        Season.entries.firstOrNull { it.name == value } ?: Season.SUMMER

    @TypeConverter
    fun availabilityToString(value: Availability): String = value.name

    @TypeConverter
    fun stringToAvailability(value: String): Availability =
        Availability.entries.firstOrNull { it.name == value } ?: Availability.ALWAYS
}
