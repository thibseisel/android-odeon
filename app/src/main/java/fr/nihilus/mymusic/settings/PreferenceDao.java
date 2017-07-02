package fr.nihilus.mymusic.settings;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.nihilus.mymusic.R;

@Singleton
public class PreferenceDao {
    private static final String DEFAULT_NIGHT_MODE = "-1";
    private static final String KEY_RANDOM_PLAYING = "random_playing_enabled";
    private static final String KEY_DAILY_SONG = "daily_song_id";
    private static final String KEY_DAILY_UPDATE = "last_daily_update";
    private static final String KEY_LAST_PLAYED = "last_played";
    private static final String KEY_DB_SETUP = "db_setup";

    private final Context mAppContext;
    private final SharedPreferences mPrefs;

    @Inject
    public PreferenceDao(Application app, SharedPreferences prefs) {
        mAppContext = app;
        mPrefs = prefs;
    }

    @AppCompatDelegate.NightMode
    public int getNightMode() {
        String nightMode = mPrefs.getString(mAppContext.getString(R.string.pref_night_mode),
                DEFAULT_NIGHT_MODE);
        return Integer.parseInt(nightMode);
    }

    public boolean isRandomPlayingEnabled() {
        return mPrefs.getBoolean(KEY_RANDOM_PLAYING, false);
    }

    public void setRandomPlayingEnabled(boolean enabled) {
        mPrefs.edit().putBoolean(KEY_RANDOM_PLAYING, enabled).apply();
    }

    public long getDailySongId() {
        return mPrefs.getLong(KEY_DAILY_SONG, -1L);
    }

    public void setDailySongId(long songId) {
        mPrefs.edit()
                .putLong(KEY_DAILY_SONG, songId)
                .putLong(KEY_DAILY_UPDATE, System.currentTimeMillis())
                .apply();
    }

    public long getLastDailySongUpdate() {
        return mPrefs.getLong(KEY_DAILY_UPDATE, 0L);
    }

    public void setLastPlayedMediaId(String mediaId) {
        mPrefs.edit().putString(KEY_LAST_PLAYED, mediaId).apply();
    }

    @Nullable
    public String getLastPlayedMediaId() {
        return mPrefs.getString(KEY_LAST_PLAYED, null);
    }

    public boolean isDatabaseSetupComplete() {
        return mPrefs.getBoolean(KEY_DB_SETUP, false);
    }

    public void setDatabaseSetupComplete(boolean complete) {
        mPrefs.edit().putBoolean(KEY_DB_SETUP, complete).apply();
    }
}
