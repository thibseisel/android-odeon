package fr.nihilus.mymusic.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class DatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "Music.db";

    private static DatabaseHelper sInstance;

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    static DatabaseHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DatabaseHelper(context);
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String tableStats = "CREATE TABLE " + Stats.TABLE + " ("
                + Stats.MUSIC_ID + " INTEGER PRIMARY KEY,"
                + Stats.READ_COUNT + " INTEGER DEFAULT 0,"
                + Stats.SKIP_COUNT + " INTEGER DEFAULT 0,"
                + Stats.TEMPO + " INTEGER DEFAULT 0,"
                + Stats.ENERGY + " INTEGER DEFAULT 0" + ");";
        db.execSQL(tableStats);

        final String tablePlaylist = "CREATE TABLE " + Playlists.TABLE + " ("
                + Playlists.PLAYLIST_ID + " INTEGER PRIMARY KEY,"
                + Playlists.DATE_CREATED + " INTEGER NOT NULL,"
                + Playlists.NAME + " TEXT NOT NULL,"
                + Playlists.ART + " TEXT" + ");";
        db.execSQL(tablePlaylist);

        final String tablePlaylistTracks = "CREATE TABLE " + Playlists.Tracks.TABLE + " ("
                + Playlists.Tracks.TRACK_ID + " INTEGER PRIMARY KEY,"
                + Playlists.Tracks.MUSIC + " INTEGER NOT NULL,"
                + Playlists.Tracks.PLAYLIST + " INTEGER NOT NULL,"
                + Playlists.Tracks.POSITION + " INTEGER NOT NULL,"
                + "FOREIGN KEY (" + Playlists.Tracks.PLAYLIST + ") REFERENCES "
                + Playlists.TABLE + "(" + Playlists.PLAYLIST_ID + ")"
                + ");";
        db.execSQL(tablePlaylistTracks);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
