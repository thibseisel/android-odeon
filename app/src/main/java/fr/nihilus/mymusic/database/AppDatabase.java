package fr.nihilus.mymusic.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.RoomDatabase;

@Database(entities = {MusicInfo.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public static final String NAME = "nihilus.db";

    public abstract MusicInfoDao musicInfoDao();

    // TODO Setup when opened for the first time
}
