package fr.nihilus.mymusic.settings;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatDelegate;

import fr.nihilus.mymusic.R;

/**
 * Helper class allowing to retrieve user shared preferences.
 */
@SuppressWarnings("WrongConstant")
public final class Prefs {

    private static final String DEFAULT_NIGHT_MODE = "-1";

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
    public static int getNightMode(Context context) {
        String nightMode = PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_night_mode), DEFAULT_NIGHT_MODE);
        return Integer.parseInt(nightMode);
    }
}
