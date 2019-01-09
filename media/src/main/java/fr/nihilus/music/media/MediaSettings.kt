/*
 * Copyright 2018 Thibault Seisel
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

package fr.nihilus.music.media

import android.content.SharedPreferences
import android.support.v4.media.session.PlaybackStateCompat
import fr.nihilus.music.media.di.ServiceScoped
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.sendBlocking
import javax.inject.Inject

internal interface MediaSettings {

    /**
     * The number of time a new playing queue has been built.
     * This may be used to uniquely identify a playing queue.
     */
    var queueCounter: Long

    /**
     * The media ID of the last played item.
     * This will be null if no item has been played.
     */
    var lastPlayedMediaId: String?

    /**
     * The last configured shuffle mode.
     * When shuffle mode is enabled, tracks in a playlist are read in random order.
     * This property value should be an `PlaybackStateCompat.SHUFFLE_MODE_*` constant.
     */
    @get:PlaybackStateCompat.ShuffleMode
    @set:PlaybackStateCompat.ShuffleMode
    var shuffleMode: Int

    /**
     * The last configured repeat mode.
     * This property value should be an `PlaybackStateCompat.REPEAT_MODE_*` constant.
     */
    @get:PlaybackStateCompat.RepeatMode
    @set:PlaybackStateCompat.RepeatMode
    var repeatMode: Int

    /**
     * Observe changes of the skip silence preference.
     * The first received value should be whether the option is actually enabled.
     * Further values are received whenever skip silence is enabled or disabled.
     */
    val skipSilenceUpdates: ReceiveChannel<Boolean>
}

private const val KEY_SHUFFLE_MODE = "shuffle_mode"
private const val KEY_REPEAT_MODE = "repeat_mode"
private const val KEY_LAST_PLAYED = "last_played"
private const val KEY_DATABASE_INIT = "should_init_dabatase"
private const val KEY_QUEUE_COUNTER = "load_counter"
private const val KEY_SKIP_SILENCE = "skip_silence"

/**
 * An implementation of [MediaSettings] that reads and writes settings
 * from a set of shared preferences stored on the device.
 *
 * @param prefs The SharedPreferences from which read/write settings.
 */
@ServiceScoped
internal class SharedPreferencesMediaSettings
@Inject constructor(
    private val prefs: SharedPreferences
) : MediaSettings {

    override var queueCounter: Long
        get() = prefs.getLong(KEY_QUEUE_COUNTER, 0L)
        set(value) = prefs.edit().putLong(KEY_QUEUE_COUNTER, value).apply()

    override var lastPlayedMediaId: String?
        get() = prefs.getString(KEY_LAST_PLAYED, null)
        set(value) = prefs.edit().putString(KEY_LAST_PLAYED, value).apply()

    override var shuffleMode: Int
        get() = prefs.getInt(KEY_SHUFFLE_MODE, PlaybackStateCompat.SHUFFLE_MODE_NONE)
        set(shuffleMode) = prefs.edit().putInt(KEY_SHUFFLE_MODE, shuffleMode).apply()

    override var repeatMode: Int
        get() = prefs.getInt(KEY_REPEAT_MODE, PlaybackStateCompat.REPEAT_MODE_NONE)
        set(repeatMode) = prefs.edit().putInt(KEY_REPEAT_MODE, repeatMode).apply()

    override val skipSilenceUpdates: ReceiveChannel<Boolean>
        get() = Channel<Boolean>(capacity = Channel.CONFLATED).also { out ->
            val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                if (key == KEY_SKIP_SILENCE) {
                    val shouldSkipSilence = prefs.getBoolean(key, false)
                    out.sendBlocking(shouldSkipSilence)
                }
            }

            prefs.registerOnSharedPreferenceChangeListener(listener)
            out.invokeOnClose {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }

            val currentValue = prefs.getBoolean(KEY_SKIP_SILENCE, false)
            out.sendBlocking(currentValue)
        }
}