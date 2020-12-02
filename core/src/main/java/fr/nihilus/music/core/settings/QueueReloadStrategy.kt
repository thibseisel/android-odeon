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

/**
 * Available options of the "Reload last played queue" preference.
 */
enum class QueueReloadStrategy(
    /**
     * Whether this strategy allows resuming playback from the same track that was playing
     * when the player last stopped.
     */
    val reloadTrack: Boolean,
    /**
     * Whether this strategy allows resuming playback at the exact same position
     * it was last stopped.
     */
    val reloadPosition: Boolean
) {
    /**
     * Reload queue that was playing when the player last stopped,
     * starting at its first track.
     */
    FROM_START(
        reloadTrack = false,
        reloadPosition = false
    ),

    /**
     * Reload queue starting from the beginning of the track that was playing
     * when the player last stopped.
     */
    FROM_TRACK(
        reloadTrack = true,
        reloadPosition = false
    ),

    /**
     * Reload queue at the exact same position it was when the player last stopped.
     */
    AT_POSITION(
        reloadTrack = true,
        reloadPosition = true
    )
}