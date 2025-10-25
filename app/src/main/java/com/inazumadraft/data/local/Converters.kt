package com.inazumadraft.data.local

import androidx.room.TypeConverter

class Converters {
    @TypeConverter
    fun fromList(values: List<String>?): String = values?.joinToString(separator = "|") ?: ""

    @TypeConverter
    fun toList(stored: String?): List<String> =
        stored?.takeIf { it.isNotBlank() }?.split('|')?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: emptyList()
}
