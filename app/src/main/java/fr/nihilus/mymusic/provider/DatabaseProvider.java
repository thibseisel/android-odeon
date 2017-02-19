package fr.nihilus.mymusic.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import static fr.nihilus.mymusic.provider.Stats.MUSIC_ID;

@SuppressWarnings("ConstantConditions")
public class DatabaseProvider extends ContentProvider {

    static final String METHOD_INCREMENT = "increment";
    static final String METHOD_BULK_INCREMENT = "bulkIncrement";
    static final String KEY_MUSIC_ID = "musicId";
    static final String KEY_FIELD = "incrementedField";
    static final String KEY_AMOUNT = "incrementAmount";

    private static final String TAG = "DatabaseProvider";
    private static final int STATS = 100;
    private static final int STAT_ID = 101;
    private static final int PLAYLISTS = 200;
    private static final int PLAYLIST_ID = 201;
    private static final int PLAYLIST_TRACKS = 300;
    private static final int PLAYLIST_TRACK_ID = 301;

    private static UriMatcher sMatcher;

    static {
        sMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sMatcher.addURI(Stats.AUTHORITY, Stats.TABLE_NAME, STATS);
        sMatcher.addURI(Stats.AUTHORITY, Stats.TABLE_NAME + "/#", STAT_ID);
        sMatcher.addURI(Stats.AUTHORITY, Playlists.TABLE_NAME, PLAYLISTS);
        sMatcher.addURI(Stats.AUTHORITY, Playlists.TABLE_NAME + "/#", PLAYLIST_ID);
        sMatcher.addURI(Stats.AUTHORITY, Playlists.TABLE_NAME + "/#/" + Playlists.Tracks.TABLE_NAME, PLAYLIST_TRACKS);
        sMatcher.addURI(Stats.AUTHORITY, Playlists.TABLE_NAME + "/#/" + Playlists.Tracks.TABLE_NAME + "/#", PLAYLIST_TRACK_ID);
    }

    private DatabaseHelper mHelper;

    @Override
    public boolean onCreate() {
        mHelper = DatabaseHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sMatcher.match(uri)) {
            case STAT_ID:
                qb.appendWhere(Stats.MUSIC_ID + "=" + ContentUris.parseId(uri));
            case STATS:
                qb.setTables(Stats.TABLE_NAME);
                if (sortOrder == null) {
                    sortOrder = Stats.DEFAULT_SORT_ORDER;
                }
                break;
            case PLAYLIST_ID:
                qb.appendWhere(Playlists.TABLE_NAME + "=" + ContentUris.parseId(uri));
            case PLAYLISTS:
                qb.setTables(Playlists.TABLE_NAME);
                if (sortOrder == null) {
                    sortOrder = Playlists.DEFAULT_SORT_ORDER;
                }
                break;
            default:
                throw new UnsupportedOperationException("Unsupported URI.");
        }

        final SQLiteDatabase db = mHelper.getReadableDatabase();
        final Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        if (getContext() != null) {
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return cursor;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        switch (sMatcher.match(uri)) {
            case STATS:
                return Stats.CONTENT_TYPE;
            case STAT_ID:
                return Stats.CONTENT_ITEM_TYPE;
            case PLAYLISTS:
                return Playlists.CONTENT_TYPE;
            case PLAYLIST_ID:
                return Playlists.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        String tableName;
        Uri contentUri;
        switch (sMatcher.match(uri)) {
            case STATS:
                tableName = Stats.TABLE_NAME;
                contentUri = Stats.CONTENT_URI;
                break;
            case PLAYLISTS:
                tableName = Playlists.TABLE_NAME;
                contentUri = Playlists.CONTENT_URI;
                break;
            default:
                throw new UnsupportedOperationException("Unsupported URI for insertion: " + uri);
        }

        SQLiteDatabase db = mHelper.getWritableDatabase();
        long id = db.insertOrThrow(tableName, null, values);
        Uri insertedUri = ContentUris.withAppendedId(contentUri, id);
        getContext().getContentResolver().notifyChange(insertedUri, null);
        return insertedUri;
    }

    @Override
    public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
        if (sMatcher.match(uri) != STATS) {
            throw new UnsupportedOperationException("Unsupported URI for insertion: " + uri);
        }
        SQLiteDatabase db = mHelper.getWritableDatabase();
        db.beginTransaction();
        int insertCount = 0;
        try {
            for (ContentValues val : values) {
                db.insertOrThrow(Stats.TABLE_NAME, null, val);
                insertCount++;
            }
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(TAG, "bulkInsert: error while inserting multiple rows.", e);
            throw e;
        } finally {
            db.endTransaction();
        }

        if (insertCount > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return insertCount;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int deletedCount;
        String tableName;

        int match = sMatcher.match(uri);

        // Add the requested ID to the selection if the URI refers to a single item
        if (match % 100 == 1) {
            long idToDelete = ContentUris.parseId(uri);
            String whereId = MUSIC_ID + "=" + idToDelete;
            selection = (selection == null) ? whereId : (whereId + " AND " + selection);
        }

        switch (match) {
            case STAT_ID:
            case STATS:
                tableName = Stats.TABLE_NAME;
                break;
            case PLAYLIST_ID:
            case PLAYLISTS:
                tableName = Playlists.TABLE_NAME;
                break;
            default:
                throw new UnsupportedOperationException("Unsupported URI for delete: " + uri);
        }

        deletedCount = db.delete(tableName, selection, selectionArgs);
        if (deletedCount > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return deletedCount;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        int updatedCount;
        String tableName;

        int match = sMatcher.match(uri);

        // Add the requested ID to the selection if the URI refers to a single item
        if (match % 100 == 1) {
            long idToDelete = ContentUris.parseId(uri);
            String whereId = MUSIC_ID + "=" + idToDelete;
            selection = (selection == null) ? whereId : (whereId + " AND " + selection);
        }

        switch (match) {
            case STAT_ID:
            case STATS:
                tableName = Stats.TABLE_NAME;
                break;
            case PLAYLIST_ID:
            case PLAYLISTS:
                tableName = Playlists.TABLE_NAME;
                break;
            default:
                throw new UnsupportedOperationException("Unsupported URI for update: " + uri);
        }

        updatedCount = db.update(tableName, values, selection, selectionArgs);
        if (updatedCount > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return updatedCount;
    }

    @Nullable
    @Override
    public Bundle call(@NonNull String method, @Nullable String arg, @Nullable Bundle extras) {
        Log.d(TAG, "call() called with: method = [" + method + "]");
        if (METHOD_INCREMENT.equals(method)) {
            if (extras == null) return null;
            long musicId = extras.getLong(KEY_MUSIC_ID, -1L);
            String fieldName = extras.getString(KEY_FIELD);
            int amount = extras.getInt(KEY_AMOUNT, 1);
            increment(musicId, fieldName, amount);
        } else if (METHOD_BULK_INCREMENT.equals(method)) {
            if (extras == null) return null;
            long[] musicId = extras.getLongArray(KEY_MUSIC_ID);
            String fieldName = extras.getString(KEY_FIELD);
            int[] amount = extras.getIntArray(KEY_AMOUNT);
            bulkIncrement(musicId, fieldName, amount);
        }
        return null;
    }

    @Override
    public void shutdown() {
        mHelper.close();
    }

    /**
     * Increment the value of a stats associated with a music id.
     * This is more efficient than querying current value and writing again
     * via {@link #query} and {@link #update}.
     * This method must be called via {@link #call} with method name {@link #METHOD_INCREMENT}.
     *
     * @param musicId   unique identifier of the music where there's a stat to increment.
     *                  Must be provided as an argument with key {@link #KEY_MUSIC_ID}.
     * @param fieldName name of the numeric field to increment in database.
     *                  Must be provided as an argument with key {@link #KEY_FIELD}.
     * @param amount    number to add to this field current value.
     *                  Must be provided as an argument with key {@link #KEY_AMOUNT}.
     */
    private void increment(long musicId, String fieldName, int amount) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String sql = "UPDATE " + Stats.TABLE_NAME
                + " SET " + fieldName + "=" + fieldName + "+ ?"
                + " WHERE " + MUSIC_ID + "= ?";
        SQLiteStatement statement = db.compileStatement(sql);
        statement.bindLong(1, amount);
        statement.bindLong(2, musicId);
        int updated = statement.executeUpdateDelete();
        if (updated > 0) {
            Log.d(TAG, "increment: success updating.");
            Uri updatedUri = ContentUris.withAppendedId(Stats.CONTENT_URI, musicId);
            getContext().getContentResolver().notifyChange(updatedUri, null);
        }
    }

    /**
     * Increment the value of a stats associated with a music id.
     * This is more efficient than querying current value and writing again
     * via {@link #query} and {@link #update}.
     * This method must be called via {@link #call} with method name {@link #METHOD_INCREMENT}.
     *
     * @param musicId   unique identifiers of musics where there's a stat to increment.
     *                  Must be provided as an argument with key {@link #KEY_MUSIC_ID}.
     * @param fieldName name of the numeric field to increment in database.
     *                  Must be provided as an argument with key {@link #KEY_FIELD}.
     * @param amount    numbers to add to this field current value.
     *                  Must be provided as an argument with key {@link #KEY_AMOUNT}.
     */
    private void bulkIncrement(long[] musicId, String fieldName, int[] amount) {
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String sql = "UPDATE " + Stats.TABLE_NAME
                + " SET " + fieldName + "=" + fieldName + "+ ?"
                + " WHERE " + MUSIC_ID + "= ?";
        SQLiteStatement statement = db.compileStatement(sql);
        int updateCount = 0;

        db.beginTransaction();
        try {
            int limit = Math.min(musicId.length, amount.length);
            for (int i = 0; i < limit; i++) {
                statement.bindLong(1, amount[i]);
                statement.bindLong(2, musicId[i]);
                updateCount = statement.executeUpdateDelete();
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
            Log.d(TAG, "bulkIncrement: incremented row count: " + updateCount);
        }
    }
}
