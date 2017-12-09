/*
 * Copyright 2017 Thibault Seisel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nihilus.music.settings;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatDelegate;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.nihilus.music.R;

@Singleton
public class PreferenceDao {
    private static final String DEFAULT_NIGHT_MODE = "-1";
    private static final String KEY_SHUFFLE_MODE = "shuffle_mode";
    private static final String KEY_REPEAT_MODE = "repeat_mode";
    private static final String KEY_DAILY_SONG = "daily_song_id";
    private static final String KEY_DAILY_UPDATE = "last_daily_update";
    private static final String KEY_LAST_PLAYED = "last_played";
    private static final String KEY_STARTUP_SCREEN = "startup_screen";

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

    @PlaybackStateCompat.ShuffleMode
    public int getShuffleMode() {
        return mPrefs.getInt(KEY_SHUFFLE_MODE, PlaybackStateCompat.SHUFFLE_MODE_NONE);
    }

    public void setShuffleMode(@PlaybackStateCompat.ShuffleMode int shuffleMode) {
        mPrefs.edit().putInt(KEY_SHUFFLE_MODE, shuffleMode).apply();
    }

    @PlaybackStateCompat.RepeatMode
    public int getRepeatMode() {
        return mPrefs.getInt(KEY_REPEAT_MODE, PlaybackStateCompat.REPEAT_MODE_NONE);
    }

    public void setRepeatMode(@PlaybackStateCompat.RepeatMode int repeatMode) {
        mPrefs.edit().putInt(KEY_REPEAT_MODE, repeatMode).apply();
    }

    public String getStartupScreenMediaId() {
        return mPrefs.getString(KEY_STARTUP_SCREEN,
                mAppContext.getString(R.string.pref_default_startup_screen));
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
}
