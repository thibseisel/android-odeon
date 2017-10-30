package fr.nihilus.music.database

import android.arch.persistence.room.Database
import android.arch.persistence.room.RoomDatabase
import android.arch.persistence.room.TypeConverters

@Database(entities = arrayOf(MusicInfo::class, Playlist::class, PlaylistTrack::class), version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun musicInfoDao(): MusicInfoDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        const val NAME = "music.db"
    }

    // TODO Setup when opened for the first time
}
