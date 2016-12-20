package fr.nihilus.mymusic.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;

public class MediaUsageProvider extends ContentProvider implements MediaUsageContract {

    private static final int USAGES = 10;
    private static final int USAGE_ID = 11;
    private static final UriMatcher sMatcher;

    static {
        sMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sMatcher.addURI(AUTHORITY, "usage", USAGES);
        sMatcher.addURI(AUTHORITY, "usage/#", USAGE_ID);
    }

    private MediaUsageDatabase mHelper;

    @Override
    public boolean onCreate() {
        mHelper = new MediaUsageDatabase(getContext());
        return true;
    }

    @Override
    public String getType(@NonNull Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        final SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        switch (sMatcher.match(uri)) {
            case USAGE_ID:
                qb.appendWhere(_ID + "=" + ContentUris.parseId(uri));
            case USAGES:
                qb.setTables(MediaUsageContract.TABLE_NAME);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported URI.");
        }

        final SQLiteDatabase db = mHelper.getReadableDatabase();
        final Cursor cursor = qb.query(db, projection, selection, selectionArgs, null, null, sortOrder);
        //noinspection ConstantConditions
        cursor.setNotificationUri(getContext().getContentResolver(), uri);

        return cursor;
    }

    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        if (sMatcher.match(uri) != USAGES) {
            throw new UnsupportedOperationException("Unsupported URI.");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        if (sMatcher.match(uri) != USAGE_ID) {
            throw new UnsupportedOperationException("Unsupported URI.");
        }
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        switch (sMatcher.match(uri)) {
            case USAGES:
                throw new UnsupportedOperationException("Not yet implemented");
            case USAGE_ID:
                throw new UnsupportedOperationException("Not yet implemented");
            default:
                throw new UnsupportedOperationException("Unsupported URI.");
        }
    }
}
