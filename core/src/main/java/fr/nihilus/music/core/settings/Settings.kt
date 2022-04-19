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

package fr.nihilus.music.core.settings

import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.playback.RepeatMode
import kotlinx.coroutines.flow.Flow

/**
 * Main entry point for reading and writing app settings.
 */
interface Settings {

    /**
     * Observe changes to the current theme settings.
     */
    val currentTheme: Flow<AppTheme>

    /**
     * The number of time a new playing queue has been built.
     * This may be used to uniquely identify a playing queue.
     */
    val queueIdentifier: Long

    /**
     * Describes how the last played queue should be re-prepared.
     * Defaults to [QueueReloadStrategy.FROM_TRACK].
     */
    val queueReload: QueueReloadStrategy

    /**
     * Whether the last played queue should be re-prepared on app startup.
     */
    val prepareQueueOnStartup: Boolean

    /**
     * The media ID of the last loaded playing queue.
     * Defaults to `null` when no playing queue has been built yet.
     *
     * Modifying this value automatically increments the [queueIdentifier].
     */
    var lastQueueMediaId: MediaId?

    /**
     * The index of the last played item in the last played queue.
     * Defaults to `0` when no item has been played yet.
     */
    var lastQueueIndex: Int

    /**
     * The playback position within the track that was last played, in milliseconds.
     * Defaults to `-1` when no track has been played yet.
     */
    var lastPlayedPosition: Long

    /**
     * Whether shuffle mode is enabled, i.e. tracks in a playlist are played in random order.
     * Defaults to `false`.
     */
    var shuffleModeEnabled: Boolean

    /**
     * The last configured repeat mode.
     * Defaults to [RepeatMode.DISABLED].
     */
    var repeatMode: RepeatMode

    /**
     * Enumeration of values for the [currentTheme] settings.
     */
    enum class AppTheme {
        /**
         * Always use the light theme.
         */
        LIGHT,
        /**
         * Automatically switch to the dark theme when Battery Saver is enabled.
         * This should be the preferred value when running Android P and earlier.
         */
        BATTERY_SAVER_ONLY,
        /**
         * Always use the dark theme.
         */
        DARK,
        /**
         * Use a light or a dark theme depending on the system configuration.
         * This should be the preferred value when running Android Q or higher:
         * this will be set from the theme system settings.
         */
        SYSTEM
    }
}
