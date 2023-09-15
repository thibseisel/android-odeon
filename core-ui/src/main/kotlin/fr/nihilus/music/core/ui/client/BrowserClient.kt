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

package fr.nihilus.music.core.ui.client

import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import fr.nihilus.music.core.media.MediaId
import kotlinx.coroutines.flow.StateFlow

/**
 * Manage interactions with a remote Media Session.
 */
interface BrowserClient {

    /**
     * A flow whose latest value is the current playback state.
     */
    val playbackState: StateFlow<PlaybackStateCompat>

    /**
     * A flow whose latest value is the currently playing track, or `null` if none.
     */
    val nowPlaying: StateFlow<MediaMetadataCompat?>

    /**
     * A flow whose latest value is the current shuffle mode.
     */
    val shuffleMode: StateFlow<Int>

    /**
     * A flow whose latest value is the current repeat mode.
     */
    val repeatMode: StateFlow<Int>

    /**
     * Requests the media service to start or resume playback.
     */
    suspend fun play()

    /**
     * Requests the media service to pause playback.
     */
    suspend fun pause()

    /**
     * Requests the media service to play the item with the specified [mediaId].
     * @param mediaId The media id of a playable item.
     */
    suspend fun playFromMediaId(mediaId: MediaId)

    /**
     * Requests the media service to move its playback position
     * to a given point in the currently playing media.
     *
     * @param positionMs The new position in the current media, in milliseconds.
     */
    suspend fun seekTo(positionMs: Long)

    /**
     * Requests the media service to move to the previous item in the current playlist.
     */
    suspend fun skipToPrevious()

    /**
     * Requests the media service to move to the next item in the current playlist.
     */
    suspend fun skipToNext()

    /**
     * Enable/Disable shuffling of playlists.
     * @param enabled Whether shuffle should be enabled.
     */
    suspend fun setShuffleModeEnabled(enabled: Boolean)

    /**
     * Sets the repeat mode.
     * @param repeatMode The new repeat mode.
     */
    suspend fun setRepeatMode(@PlaybackStateCompat.RepeatMode repeatMode: Int)
}
