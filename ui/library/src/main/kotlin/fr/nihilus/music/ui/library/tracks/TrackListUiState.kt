/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.ui.library.tracks

import android.net.Uri
import fr.nihilus.music.core.media.MediaId
import kotlin.time.Duration

/**
 * UI state of [AllTracksFragment].
 */
internal data class TrackListUiState(
    /**
     * List of all tracks in the whole music library.
     * Tracks are sorted alphabetically.
     */
    val tracks: List<TrackUiState>,
    /**
     * Whether the list of all tracks is currently being loaded.
     */
    val isLoadingTracks: Boolean,
    /**
     * Event to be handled by the UI as soon as possible.
     * This could be a message to be displayed or a permission to be granted.
     * `null` if there are no events waiting to be handled.
     */
    val pendingEvent: TrackEvent?,
)

/**
 * UI representation of an audio track from the music library.
 */
internal data class TrackUiState(
    /**
     * Unique identifier of the track.
     * Could be used to operate on that track.
     */
    val id: MediaId,
    /**
     * User-readable name of the track.
     */
    val title: String,
    /**
     * Name of the artist that produced that track.
     */
    val artist: String,
    /**
     * Time it takes to listen to the full track.
     */
    val duration: Duration,
    /**
     * Optional URI pointing to an decorative artwork.
     */
    val artworkUri: Uri?,
)
