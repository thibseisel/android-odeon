package fr.nihilus.music.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.TypeConverters;

@Database(entities = {MusicInfo.class, Playlist.class, PlaylistTrack.class}, version = 1)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public static final String NAME = "music.db";

    public abstract MusicInfoDao musicInfoDao();
    public abstract PlaylistDao playlistDao();

    // TODO Setup when opened for the first time
}
