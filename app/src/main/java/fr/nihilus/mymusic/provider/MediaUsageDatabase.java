package fr.nihilus.mymusic.provider;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

class MediaUsageDatabase extends SQLiteOpenHelper implements MediaUsageContract {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "MediaUsage.db";

    MediaUsageDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String createTable = "CREATE TABLE " + TABLE_NAME + " ("
                + _ID + " INTEGER PRIMARY KEY, "
                + COL_READ_COUNT + " INTEGER NOT NULL"
                + ");";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
