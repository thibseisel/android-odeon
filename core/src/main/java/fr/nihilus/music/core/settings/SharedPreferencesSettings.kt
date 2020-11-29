/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.core.settings

import android.content.Context
import android.content.SharedPreferences
import fr.nihilus.music.core.R
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.toMediaId
import fr.nihilus.music.core.playback.RepeatMode
import fr.nihilus.music.core.settings.Settings.QueueReloadStrategy
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import org.jetbrains.annotations.TestOnly
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of the [Settings] repository that stores settings in [SharedPreferences].
 *
 * @param context The application context.
 * @param preferences The main interface for reading shared preferences.
 */
@Singleton
internal class SharedPreferencesSettings @Inject constructor(
    private val context: Context,
    private val preferences: SharedPreferences
) : Settings {

    /**
     * Because [SharedPreferences.registerOnSharedPreferenceChangeListener] does not keep
     * strong references to registered listeners, some may not be called due to being garbage collected.
     * Keeping registered listeners to this Set ensures that no references are lost.
     */
    private val preferenceListeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override val currentTheme: Flow<Settings.AppTheme> = preferenceFlow(PREF_KEY_THEME)
        .map { prefs ->
            val defaultThemeValue = context.getString(R.string.pref_theme_default_value)

            when (val themeValue = prefs.getString(PREF_KEY_THEME, defaultThemeValue)) {
                context.getString(R.string.pref_theme_light_value) -> Settings.AppTheme.LIGHT
                context.getString(R.string.pref_theme_battery_value) -> Settings.AppTheme.BATTERY_SAVER_ONLY
                context.getString(R.string.pref_theme_dark_value) -> Settings.AppTheme.DARK
                context.getString(R.string.pref_theme_system_value) -> Settings.AppTheme.SYSTEM
                else -> error("Unexpected value for $PREF_KEY_THEME preference: $themeValue")
            }
        }

    override val queueIdentifier: Long
        get() = preferences.getLong(PREF_KEY_QUEUE_IDENTIFIER, 0L)

    override val queueReload: QueueReloadStrategy
        get() {
            val prefValues = context.resources.getStringArray(R.array.prefs_reload_queue_values)
            return when (val value = preferences.getString(PREF_KEY_RELOAD_QUEUE, null)) {
                prefValues[0] -> QueueReloadStrategy.NO_RELOAD
                prefValues[1] -> QueueReloadStrategy.FROM_START
                prefValues[2] -> QueueReloadStrategy.FROM_TRACK
                prefValues[3] -> QueueReloadStrategy.AT_POSITION
                null -> QueueReloadStrategy.FROM_TRACK
                else -> error("Unexpected value for $PREF_KEY_RELOAD_QUEUE preference: $value")
            }
        }

    override var lastQueueMediaId: MediaId?
        get() = preferences.getString(PREF_KEY_LAST_PLAYED, null)?.toMediaId()
        set(mediaId) {
            preferences.edit()
                .putString(PREF_KEY_LAST_PLAYED, mediaId?.encoded)
                .putLong(PREF_KEY_QUEUE_IDENTIFIER, queueIdentifier + 1)
                .apply()
        }

    override var lastQueueIndex: Int
        get() = preferences.getInt(PREF_KEY_QUEUE_INDEX, 0)
        set(indexInQueue) = preferences.edit().putInt(PREF_KEY_QUEUE_INDEX, indexInQueue).apply()

    override var lastPlayedPosition: Long
        get() = preferences.getLong(PREF_KEY_QUEUE_POSITION, -1L)
        set(position) = preferences.edit().putLong(PREF_KEY_QUEUE_POSITION, position).apply()

    override var shuffleModeEnabled: Boolean
        get() = preferences.getBoolean(PREF_KEY_SHUFFLE_MODE_ENABLED, false)
        set(enabled) = preferences.edit()
            .putBoolean(PREF_KEY_SHUFFLE_MODE_ENABLED, enabled)
            .apply()

    override var repeatMode: RepeatMode
        get() {
            val repeatModeCode = preferences.getInt(PREF_KEY_REPEAT_MODE, RepeatMode.DISABLED.code)
            return RepeatMode.values().first { it.code == repeatModeCode }
        }
        set(mode) = preferences.edit()
            .putInt(PREF_KEY_REPEAT_MODE, mode.code)
            .apply()

    val skipSilence: Flow<Boolean> = preferenceFlow(PREF_KEY_SKIP_SILENCE)
        .map { it.getBoolean(PREF_KEY_SKIP_SILENCE, false) }

    private fun preferenceFlow(watchedKey: String) = callbackFlow<SharedPreferences> {
        val valueListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (watchedKey == key) {
                offer(prefs)
            }
        }

        // Request to emit the current value.
        offer(preferences)

        // Register a listener to be called when the preference value changes.
        preferenceListeners += valueListener
        preferences.registerOnSharedPreferenceChangeListener(valueListener)

        // Make sure to unregister the listener when flow is cancelled.
        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(valueListener)
            preferenceListeners -= valueListener
        }
    }.conflate()
}

@TestOnly internal const val PREF_KEY_THEME = "pref_theme"
@TestOnly internal const val PREF_KEY_SKIP_SILENCE = "skip_silence"
@TestOnly internal const val PREF_KEY_SHUFFLE_MODE_ENABLED = "shuffle_mode_enabled"
@TestOnly internal const val PREF_KEY_REPEAT_MODE = "repeat_mode"
@TestOnly internal const val PREF_KEY_LAST_PLAYED = "last_played"
@TestOnly internal const val PREF_KEY_QUEUE_IDENTIFIER = "load_counter"
@TestOnly internal const val PREF_KEY_QUEUE_INDEX = "last_played_index"
@TestOnly internal const val PREF_KEY_QUEUE_POSITION = "last_played_position_ms"
@TestOnly internal const val PREF_KEY_RELOAD_QUEUE = "reload_queue"
