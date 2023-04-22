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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.ui.actions.DeleteTracksAction
import fr.nihilus.music.core.ui.actions.ExcludeTrackAction
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.core.ui.uiStateIn
import fr.nihilus.music.media.AudioTrack
import fr.nihilus.music.media.browser.BrowserTree
import fr.nihilus.music.media.tracks.DeleteTracksResult.Deleted
import fr.nihilus.music.media.tracks.DeleteTracksResult.RequiresPermission
import fr.nihilus.music.media.tracks.DeleteTracksResult.RequiresUserConsent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * State holder for [AllTracksFragment].
 */
@HiltViewModel
internal class TracksViewModel @Inject constructor(
    private val browser: BrowserTree,
    private val controller: BrowserClient,
    private val deleteTracks: DeleteTracksAction,
    private val excludeTracks: ExcludeTrackAction,
) : ViewModel() {

    /**
     * Live list of all audio tracks available to the application.
     */
    private val tracks: Flow<List<TrackUiState>>
        get() = browser.getChildren(MediaId.ALL_TRACKS).map { tracks ->
            tracks.filterIsInstance<AudioTrack>().map {
                TrackUiState(
                    id = it.id,
                    title = it.title,
                    artist = it.artist,
                    duration = it.duration.milliseconds,
                    artworkUri = it.iconUri,
                )
            }
        }

    /**
     * Live queue of events to be handled by the UI.
     */
    private val events = MutableStateFlow<List<TrackEvent>>(emptyList())

    /**
     * Whole UI state.
     */
    val state: StateFlow<TrackListUiState> by lazy {
        combine(tracks, events) { tracks, events ->
            TrackListUiState(
                tracks = tracks, isLoadingTracks = false, pendingEvent = events.firstOrNull()
            )
        }
            .uiStateIn(
                viewModelScope,
                initialState = TrackListUiState(
                    tracks = emptyList(),
                    isLoadingTracks = true,
                    pendingEvent = null
                )
            )
    }

    /**
     * Prepare the music player to play all tracks in the music library.
     * @param trackId Identifier of the track that should be played immediately.
     */
    fun playTrack(trackId: MediaId) {
        viewModelScope.launch {
            controller.playFromMediaId(trackId)
        }
    }

    /**
     * Permanently deletes a track from the device's storage.
     * This operation may not result in the target track being immediately deleted; you should check
     * the result of the delete operation in [TrackListUiState.pendingEvent] and handle the event
     * appropriately.
     * @param trackId Track to be deleted.
     */
    fun deleteTrack(trackId: MediaId) {
        viewModelScope.launch {
            val result = deleteTracks(listOf(trackId))
            events.update { pendingEvents ->
                pendingEvents + when (result) {
                    is Deleted -> TrackEvent.TrackSuccessfullyDeleted
                    is RequiresPermission -> TrackEvent.RequiresStoragePermission(trackId)
                    is RequiresUserConsent -> TrackEvent.RequiresUserConsent(result.intent)
                }
            }
        }
    }

    /**
     * Removes a track from the music library.
     * Unlike [deleteTrack], that track won't be deleted from the device's storage, only hidden
     * everywhere in the app.
     * @param trackId Track to exclude from the music library.
     */
    fun excludeTrack(trackId: MediaId) {
        viewModelScope.launch {
            excludeTracks(trackId)
        }
    }

    /**
     * Mark the current [pending event][TrackListUiState.pendingEvent] as being handled.
     * This will remove that event from the event queue and proceed with the next event, if any.
     */
    fun consumeEvent() {
        events.update { it.drop(1) }
    }
}
