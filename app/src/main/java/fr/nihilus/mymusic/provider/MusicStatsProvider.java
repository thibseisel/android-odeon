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

@SuppressWarnings("ConstantConditions")
public class MusicStatsProvider extends ContentProvider implements MusicStats {

    static final String METHOD_INCREMENT = "increment";
    static final String KEY_MUSIC_ID = "musicId";
    static final String KEY_FIELD = "incrementedField";
    static final String KEY_AMOUNT = "incrementAmount";

    private static final String TAG = "MusicStatsProvider";
    private static final int STATS = 100;
    private static final int STAT_ID = 101;

    private static UriMatcher sMatcher;

    static {
        sMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sMatcher.addURI(AUTHORITY, TABLE_NAME, STATS);
        sMatcher.addURI(AUTHORITY, TABLE_NAME + "/#", STAT_ID);
    }

    private MusicStatsDbHelper mHelper;

    @Override
    public boolean onCreate() {
        mHelper = MusicStatsDbHelper.getInstance(getContext());
        return true;
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sMatcher.match(uri)) {
            case STAT_ID:
                qb.appendWhere(MUSIC_ID + "=" + ContentUris.parseId(uri));
            case STATS:
                qb.setTables(MusicStats.TABLE_NAME);
                if (sortOrder == null) {
                    sortOrder = DEFAULT_SORT_ORDER;
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
                return CONTENT_TYPE;
            case STAT_ID:
                return CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unsupported URI: " + uri);
        }
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        if (sMatcher.match(uri) != STATS) {
            throw new UnsupportedOperationException("Unsupported URI for insertion: " + uri);
        }
        SQLiteDatabase db = mHelper.getWritableDatabase();
        long id = db.insertOrThrow(TABLE_NAME, null, values);
        Uri insertedUri = ContentUris.withAppendedId(CONTENT_URI, id);
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
                db.insertOrThrow(TABLE_NAME, null, val);
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

        switch (sMatcher.match(uri)) {
            case STAT_ID:
                long idToDelete = ContentUris.parseId(uri);
                String whereId = MUSIC_ID + "=" + idToDelete;
                selection = (selection == null) ? whereId : (whereId + " AND " + selection);
            case STATS:
                deletedCount = db.delete(TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported URI for delete: " + uri);
        }

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

        switch (sMatcher.match(uri)) {
            case STAT_ID:
                long idToUpdate = ContentUris.parseId(uri);
                String whereId = MUSIC_ID + "=" + idToUpdate;
                selection = (selection == null) ? whereId : (whereId + " AND " + selection);
            case STATS:
                updatedCount = db.update(TABLE_NAME, values, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported URI for update: " + uri);
        }

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
        }
        return null;
    }

    /**
     * Increment the value of a stats associated with a music id.
     * This is more efficient than querying current value and writing again
     * via {@link #query} and {@link #update}.
     * This method must be called via {@link #call} with method name {@link #METHOD_INCREMENT}.
     *
     * @param musicId unique identifier of the music where there's a stat to increment.
     *                Must be provided as an argument with key {@link #KEY_MUSIC_ID}.
     * @param fieldName name of the numeric field to increment in database.
     *                  Must be provided as an argument with key {@link #KEY_FIELD}.
     * @param amount number to add to thhis field current value.
     *               Must be provided as an argument with key {@link #KEY_AMOUNT}.
     */
    private void increment(long musicId, String fieldName, int amount) {
        Log.d(TAG, "increment() called with: musicId = [" + musicId + "], fieldName = ["
                + fieldName + "], amount = [" + amount + "]");
        SQLiteDatabase db = mHelper.getWritableDatabase();
        String sql = "UPDATE " + TABLE_NAME
                + " SET " + fieldName + "=" + fieldName + "+ ?"
                + " WHERE " + MUSIC_ID + "= ?";
        SQLiteStatement statement = db.compileStatement(sql);
        statement.bindLong(1, amount);
        statement.bindLong(2, musicId);
        int updated = statement.executeUpdateDelete();
        if (updated > 0) {
            Log.d(TAG, "increment: success updating.");
            Uri updatedUri = ContentUris.withAppendedId(CONTENT_URI, musicId);
            getContext().getContentResolver().notifyChange(updatedUri, null);
        }
    }

    @Override
    public void shutdown() {
        mHelper.close();
    }
}
