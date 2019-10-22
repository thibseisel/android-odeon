/*
 * Copyright 2019 Thibault Seisel
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

package fr.nihilus.music.common.settings

import android.content.Context
import android.content.SharedPreferences
import android.support.v4.media.session.PlaybackStateCompat
import fr.nihilus.music.common.R
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of the [Settings] repository that stores settings in [SharedPreferences].
 *
 * @param context The application context.
 * @param preferences The main interface for reading shared preferences.
 */
@Singleton
internal class SharedPreferencesSettings
@Inject constructor(
    private val context: Context,
    private val preferences: SharedPreferences
) : Settings {

    override val currentTheme: Flow<Settings.AppTheme>
        get() = preferenceFlow(KEY_THEME) { prefs ->
            val defaultThemeValue = context.getString(R.string.pref_theme_default_value)

            when (val themeValue = prefs.getString(KEY_THEME, defaultThemeValue)) {
                context.getString(R.string.pref_theme_light_value) -> Settings.AppTheme.LIGHT
                context.getString(R.string.pref_theme_battery_value) -> Settings.AppTheme.BATTERY_SAVER_ONLY
                context.getString(R.string.pref_theme_dark_value) -> Settings.AppTheme.DARK
                context.getString(R.string.pref_theme_system_value) -> Settings.AppTheme.SYSTEM
                else -> error("Unexpected value for $KEY_THEME preference: $themeValue")
            }
        }

    override val queueIdentifier: Long
        get() = preferences.getLong(KEY_QUEUE_COUNTER, 0L)

    override var lastQueueMediaId: String?
        get() = preferences.getString(KEY_LAST_PLAYED, null)
        set(mediaId) {
            preferences.edit()
                .putString(KEY_LAST_PLAYED, mediaId)
                .putLong(KEY_QUEUE_COUNTER, queueIdentifier + 1)
                .apply()
        }

    override var lastQueueIndex: Int
        get() = preferences.getInt(KEY_QUEUE_INDEX, 0)
        set(indexInQueue) = preferences.edit().putInt(KEY_QUEUE_INDEX, indexInQueue).apply()

    override var shuffleMode: Int
        get() = preferences.getInt(KEY_SHUFFLE_MODE, PlaybackStateCompat.SHUFFLE_MODE_NONE)
        set(mode) = preferences.edit().putInt(KEY_SHUFFLE_MODE, mode).apply()

    override var shuffleModeEnabled: Boolean
        get() = if (preferences.contains(KEY_SHUFFLE_MODE) && !preferences.contains(KEY_SHUFFLE_MODE_ENABLED)) {
            // Because the old shuffle_mode preference has been deprecated,
            // infer the value of this preference from it.
            val oldShuffleMode = preferences.getInt(KEY_SHUFFLE_MODE, -1)
            val isEnabled = (oldShuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL)

            // Set the preference and remove the deprecated key.
            preferences.edit()
                .putBoolean(KEY_SHUFFLE_MODE_ENABLED, isEnabled)
                .remove(KEY_SHUFFLE_MODE)
                .apply()

            isEnabled
        } else preferences.getBoolean(KEY_SHUFFLE_MODE_ENABLED, false)

        set(enabled) = preferences.edit()
            .putBoolean(KEY_SHUFFLE_MODE_ENABLED, enabled)
            .apply()

    override var repeatMode: RepeatMode
        get() = when (preferences.getInt(KEY_REPEAT_MODE, PlaybackStateCompat.REPEAT_MODE_NONE)) {
            PlaybackStateCompat.REPEAT_MODE_ALL,
            PlaybackStateCompat.REPEAT_MODE_GROUP -> RepeatMode.ALL
            PlaybackStateCompat.REPEAT_MODE_ONE -> RepeatMode.ONE
            else -> RepeatMode.DISABLED
        }
        set(mode) = preferences.edit()
            .putInt(KEY_REPEAT_MODE, when (mode) {
                RepeatMode.ALL -> PlaybackStateCompat.REPEAT_MODE_ALL
                RepeatMode.ONE -> PlaybackStateCompat.REPEAT_MODE_ONE
                else -> PlaybackStateCompat.REPEAT_MODE_NONE
            })
            .apply()

    override val skipSilence: Flow<Boolean>
        get() = preferenceFlow(KEY_SKIP_SILENCE) { it.getBoolean(KEY_SKIP_SILENCE, false) }

    @ExperimentalCoroutinesApi
    private fun <T> preferenceFlow(
        watchedKey: String,
        valueProvider: (prefs: SharedPreferences) -> T
    ): Flow<T> = callbackFlow {
        val valueListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (watchedKey == key) {
                val preferenceValue = valueProvider(prefs)
                offer(preferenceValue)
            }
        }

        preferences.registerOnSharedPreferenceChangeListener(valueListener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(valueListener) }
    }.conflate()
}

private const val KEY_THEME = "pref_theme"
private const val KEY_SKIP_SILENCE = "skip_silence"
private const val KEY_SHUFFLE_MODE = "shuffle_mode"
private const val KEY_SHUFFLE_MODE_ENABLED = "shuffle_mode_enabled"
private const val KEY_REPEAT_MODE = "repeat_mode"
private const val KEY_LAST_PLAYED = "last_played"
private const val KEY_QUEUE_COUNTER = "load_counter"
private const val KEY_QUEUE_INDEX = "last_played_index"
