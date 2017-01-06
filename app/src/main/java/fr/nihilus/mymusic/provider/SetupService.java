package fr.nihilus.mymusic.provider;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.util.Log;

import fr.nihilus.mymusic.settings.Prefs;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SetupService extends IntentService {
    private static final String TAG = "SetupService";
    private static final String ACTION_SETUP_DB = "fr.nihilus.mymusic.action.SETUP_DATABASE";

    public SetupService() {
        super("SetupService");
    }

    /**
     * Start initialization of the database containing all statistics related
     * to the use of this application.
     * This should be called when the application starts for the first time.
     * Use {@link Prefs#isDatabaseSetupComplete} to make sure this is the first time you run this.
     * @param context context of this application package
     */
    public static void startDatabaseSetup(Context context) {
        Intent intent = new Intent(context, SetupService.class);
        intent.setAction(ACTION_SETUP_DB);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SETUP_DB.equals(action)) {
                handleDatabaseSetup();
            }
        }
    }

    private void handleDatabaseSetup() {
        // Perform only if database setup is not already done
        if (!Prefs.isDatabaseSetupComplete(this)) {
            Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{BaseColumns._ID}, null, null, BaseColumns._ID);
            if (cursor != null) {
                ContentValues[] values = new ContentValues[cursor.getCount()];
                int colId = cursor.getColumnIndexOrThrow(BaseColumns._ID);

                while (cursor.moveToNext()) {
                    long musicId = cursor.getLong(colId);
                    ContentValues cv = new ContentValues();
                    cv.put(MusicStats.MUSIC_ID, musicId);
                    values[cursor.getPosition()] = cv;
                }

                cursor.close();
                int insertCount = getContentResolver().bulkInsert(MusicStats.CONTENT_URI, values);
                Log.d(TAG, "handleDatabaseSetup: inserted " + insertCount + " rows in database.");
                Prefs.setDatabaseSetupComplete(this);
            }
        }
    }
}