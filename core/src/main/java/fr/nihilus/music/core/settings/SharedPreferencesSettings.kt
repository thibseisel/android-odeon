/*
 * Copyright 2021 Thibault Seisel
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
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.nihilus.music.core.R
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.parse
import fr.nihilus.music.core.playback.RepeatMode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import org.jetbrains.annotations.TestOnly
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Implementation of the [Settings] repository that stores settings in [SharedPreferences].
 *
 * @param context The application context.
 * @param preferences The main interface for reading shared preferences.
 */
@Singleton
internal class SharedPreferencesSettings @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferences: Provider<SharedPreferences>
) : Settings {

    /**
     * Because [SharedPreferences.registerOnSharedPreferenceChangeListener] does not keep
     * strong references to registered listeners, some may not be called due to being garbage collected.
     * Keeping registered listeners to this Set ensures that no references are lost.
     */
    private val preferenceListeners = mutableSetOf<SharedPreferences.OnSharedPreferenceChangeListener>()

    override val currentTheme: Flow<Settings.AppTheme>
        get() {
            val prefKey = context.getString(R.string.pref_key_theme)
            val themeValues = context.resources.getStringArray(R.array.prefs_theme_values)
            val defaultThemeValue = context.getString(R.string.pref_theme_default_value)

            return preferenceFlow(prefKey)
                .map { prefs ->
                    when (val themeValue = prefs.getString(prefKey, defaultThemeValue)) {
                        themeValues[0] -> Settings.AppTheme.LIGHT
                        themeValues[1] -> Settings.AppTheme.DARK
                        themeValues[2] -> Settings.AppTheme.BATTERY_SAVER_ONLY
                        themeValues[3] -> Settings.AppTheme.SYSTEM
                        else -> error("Unexpected value for $prefKey preference: $themeValue")
                    }
                }
        }

    override val queueIdentifier: Long
        get() = preferences.get().getLong(PREF_KEY_QUEUE_IDENTIFIER, 0L)

    override val queueReload: QueueReloadStrategy
        get() {
            val prefValues = context.resources.getStringArray(R.array.prefs_reload_queue_values)

            val prefKey = context.getString(R.string.pref_key_reload_queue)
            return when (val value = preferences.get().getString(prefKey, null)) {
                prefValues[0] -> QueueReloadStrategy.FROM_START
                prefValues[1] -> QueueReloadStrategy.FROM_TRACK
                prefValues[2] -> QueueReloadStrategy.AT_POSITION
                null -> QueueReloadStrategy.FROM_TRACK
                else -> error("Unexpected value for $prefKey preference: $value")
            }
        }

    override val prepareQueueOnStartup: Boolean
        get() = preferences.get().getBoolean(
            context.getString(R.string.pref_key_prepare_on_startup),
            context.resources.getBoolean(R.bool.pref_default_prepare_on_startup)
        )

    override var lastQueueMediaId: MediaId?
        get() = preferences.get().getString(PREF_KEY_LAST_PLAYED, null)?.parse()
        set(mediaId) {
            preferences.get().edit()
                .putString(PREF_KEY_LAST_PLAYED, mediaId?.encoded)
                .putLong(PREF_KEY_QUEUE_IDENTIFIER, queueIdentifier + 1)
                .apply()
        }

    override var lastQueueIndex: Int
        get() = preferences.get().getInt(PREF_KEY_QUEUE_INDEX, 0)
        set(indexInQueue) = preferences.get().edit()
            .putInt(PREF_KEY_QUEUE_INDEX, indexInQueue)
            .apply()

    override var lastPlayedPosition: Long
        get() = preferences.get().getLong(PREF_KEY_QUEUE_POSITION, -1L)
        set(position) = preferences.get().edit()
            .putLong(PREF_KEY_QUEUE_POSITION, position)
            .apply()

    override var shuffleModeEnabled: Boolean
        get() = preferences.get().getBoolean(PREF_KEY_SHUFFLE_MODE_ENABLED, false)
        set(enabled) = preferences.get().edit()
            .putBoolean(PREF_KEY_SHUFFLE_MODE_ENABLED, enabled)
            .apply()

    override var repeatMode: RepeatMode
        get() {
            val repeatModeCode = preferences.get().getInt(PREF_KEY_REPEAT_MODE, RepeatMode.DISABLED.code)
            return RepeatMode.values().first { it.code == repeatModeCode }
        }
        set(mode) = preferences.get().edit()
            .putInt(PREF_KEY_REPEAT_MODE, mode.code)
            .apply()

    private fun preferenceFlow(watchedKey: String) = callbackFlow<SharedPreferences> {
        val valueListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (watchedKey == key) {
                offer(prefs)
            }
        }

        // Request to emit the current value.
        val prefs = preferences.get()
        offer(prefs)

        // Register a listener to be called when the preference value changes.
        preferenceListeners += valueListener
        prefs.registerOnSharedPreferenceChangeListener(valueListener)

        // Make sure to unregister the listener when flow is cancelled.
        awaitClose {
            prefs.unregisterOnSharedPreferenceChangeListener(valueListener)
            preferenceListeners -= valueListener
        }
    }.conflate()
}

@TestOnly internal const val PREF_KEY_SHUFFLE_MODE_ENABLED = "shuffle_mode_enabled"
@TestOnly internal const val PREF_KEY_REPEAT_MODE = "repeat_mode"
@TestOnly internal const val PREF_KEY_LAST_PLAYED = "last_played"
@TestOnly internal const val PREF_KEY_QUEUE_IDENTIFIER = "load_counter"
@TestOnly internal const val PREF_KEY_QUEUE_INDEX = "last_played_index"
@TestOnly internal const val PREF_KEY_QUEUE_POSITION = "last_played_position_ms"
