package fr.nihilus.mymusic.settings;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatDelegate;

import fr.nihilus.mymusic.R;

/**
 * Helper class allowing to retrieve user shared preferences.
 */
@SuppressWarnings("WrongConstant")
public final class Prefs {

    private static final String DEFAULT_NIGHT_MODE = "-1";
    private static final String KEY_RANDOM_PLAYING = "random_playing_enabled";
    private static final String KEY_DAILY_SONG = "daily_song_id";
    private static final String KEY_DAILY_UPDATE = "last_daily_update";
    private static final String KEY_LAST_PLAYED = "last_played";

    /**
     * Returns the night mode preference. Result of this method can be the following :
     * <ul>
     * <li>{@link AppCompatDelegate#MODE_NIGHT_FOLLOW_SYSTEM} for the default behavior,</li>
     * <li>{@link AppCompatDelegate#MODE_NIGHT_AUTO} to automatically switch depending on time,</li>
     * <li>{@link AppCompatDelegate#MODE_NIGHT_NO} to always use the Light theme,</li>
     * <li>{@link AppCompatDelegate#MODE_NIGHT_YES} to always use the Dark theme.</li>
     * </ul>
     */
    @AppCompatDelegate.NightMode
    public static int getNightMode(@NonNull Context context) {
        String nightMode = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_night_mode), DEFAULT_NIGHT_MODE);
        return Integer.parseInt(nightMode);
    }

    public static boolean isRandomPlayingEnabled(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(KEY_RANDOM_PLAYING, false);
    }

    public static void setRandomPlayingEnabled(@NonNull Context context, boolean enabled) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putBoolean(KEY_RANDOM_PLAYING, enabled)
                .apply();
    }

    public static void setDailySongId(@NonNull Context context, long songId) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putLong(KEY_DAILY_SONG, songId)
                .putLong(KEY_DAILY_UPDATE, System.currentTimeMillis())
                .apply();
    }

    public static long getDailySongId(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_DAILY_SONG, -1L);
    }

    public static long getLastDailySongUpdate(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getLong(KEY_DAILY_UPDATE, -1L);
    }

    public static void setLastPlayedMediaId(@NonNull Context context, String mediaId) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
                .putString(KEY_LAST_PLAYED, mediaId)
                .apply();
    }

    @Nullable
    public static String getLastPlayedMediaId(@NonNull Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(KEY_LAST_PLAYED, null);
    }
}
