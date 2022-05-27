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

package fr.nihilus.music.library.songs

import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaItems
import fr.nihilus.music.core.media.parse
import fr.nihilus.music.core.ui.actions.DeleteTracksAction
import fr.nihilus.music.core.ui.actions.ExcludeTrackAction
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.core.ui.uiStateIn
import fr.nihilus.music.media.provider.DeleteTracksResult.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

/**
 * State holder for [AllTracksFragment].
 */
@HiltViewModel
internal class TracksViewModel @Inject constructor(
    private val client: BrowserClient,
    private val deleteAction: DeleteTracksAction,
    private val excludeAction: ExcludeTrackAction,
) : ViewModel() {

    /**
     * Live list of all audio tracks available to the application.
     * Its value is `null` while tracks are currently being loaded.
     */
    private val tracks: Flow<List<TrackUiState>?>
        get() = client.getChildren(MediaId.ALL_TRACKS)
            .map<List<MediaBrowserCompat.MediaItem>, List<TrackUiState>?> { tracks ->
                tracks.map(MediaBrowserCompat.MediaItem::toTrack)
            }
            .catch { emit(emptyList()) }
            .onStart { emit(null) }

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
                tracks = tracks.orEmpty(),
                isLoadingTracks = tracks == null,
                pendingEvent = events.firstOrNull()
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
            client.playFromMediaId(trackId)
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
            val result = deleteAction.delete(listOf(trackId))
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
            excludeAction.exclude(trackId)
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

private fun MediaBrowserCompat.MediaItem.toTrack(): TrackUiState = TrackUiState(
    id = mediaId.parse(),
    title = description.title?.toString() ?: "",
    artist = description.subtitle?.toString() ?: "",
    duration = description.extras!!.getLong(MediaItems.EXTRA_DURATION).milliseconds,
    artworkUri = description.iconUri
)
