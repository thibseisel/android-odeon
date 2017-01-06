package fr.nihilus.mymusic.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class MusicStatsDbHelper extends SQLiteOpenHelper implements MusicStats {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "MusicStats.db";

    private static MusicStatsDbHelper sInstance;

    private MusicStatsDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    static MusicStatsDbHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new MusicStatsDbHelper(context);
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String createTable = "CREATE TABLE " + TABLE_NAME + " ("
                + MUSIC_ID + " INTEGER PRIMARY KEY,"
                + READ_COUNT + " INTEGER DEFAULT 0,"
                + SKIP_COUNT + " INTEGER DEFAULT 0,"
                + TEMPO + " INTEGER DEFAULT 0,"
                + ENERGY + " INTEGER DEFAULT 0" + ");";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
