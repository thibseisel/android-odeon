package fr.nihilus.music.database

import android.arch.persistence.room.TypeConverter
import android.net.Uri
import java.util.*

internal class Converters {

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time

    @TypeConverter
    fun fromString(str: String?): Uri = str?.let { Uri.parse(it) } ?: Uri.EMPTY

    @TypeConverter
    fun toUriString(uri: Uri?): String? = uri?.toString()

}
