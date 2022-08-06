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

package fr.nihilus.music.ui.library.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.ui.Event
import fr.nihilus.music.core.ui.actions.DeleteTracksAction
import fr.nihilus.music.core.ui.actions.ExcludeTrackAction
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.core.ui.uiStateIn
import fr.nihilus.music.media.MediaCategory
import fr.nihilus.music.media.MediaContent
import fr.nihilus.music.media.browser.MediaSearchEngine
import fr.nihilus.music.media.browser.SearchQuery
import fr.nihilus.music.ui.library.DeleteTracksConfirmation
import fr.nihilus.music.ui.library.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val KEY_SEARCH_QUERY = "odeon.SearchViewModel.searchQuery"
private const val SEARCH_DELAY = 250L

@HiltViewModel
internal class SearchViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val client: BrowserClient,
    private val engine: MediaSearchEngine,
    private val excludeTracks: ExcludeTrackAction,
    private val deleteTracks: DeleteTracksAction,
) : ViewModel() {

    private val mediaTypeImportance = compareBy<String> { mediaType ->
        when (mediaType) {
            MediaId.TYPE_TRACKS -> 0
            MediaId.TYPE_PLAYLISTS -> 1
            MediaId.TYPE_ARTISTS -> 2
            MediaId.TYPE_ALBUMS -> 3
            else -> 5
        }
    }

    private val _deleteEvent = MutableLiveData<Event<DeleteTracksConfirmation>>()
    val deleteEvent: LiveData<Event<DeleteTracksConfirmation>>
        get() = _deleteEvent

    /**
     * Live UI state.
     */
    val state: StateFlow<SearchScreenUiState> by lazy {
        savedState.getStateFlow(KEY_SEARCH_QUERY, "")
            .mapLatest { query ->
                delay(SEARCH_DELAY)
                SearchScreenUiState(
                    query = query,
                    results = if (query.isNotBlank()) {
                        groupByMediaType(
                            engine.search(
                                SearchQuery.Unspecified(query)
                            )
                        )
                    } else {
                        emptyList()
                    }
                )
            }
            .uiStateIn(
                viewModelScope,
                initialState = SearchScreenUiState(
                    query = savedState[KEY_SEARCH_QUERY] ?: "",
                    results = emptyList()
                )
            )
    }

    /**
     * Change search terms used to filter search results.
     */
    fun search(query: String) {
        savedState[KEY_SEARCH_QUERY] = query
    }

    /**
     * Start playback of the given playable media.
     * This builds a play queue based on the search results.
     */
    fun play(media: MediaId) {
        viewModelScope.launch {
            client.playFromMediaId(media)
        }
    }

    /**
     * Exclude a playable media from the whole music library.
     * That media file won't be deleted.
     */
    fun exclude(media: MediaId) {
        requireNotNull(media.track) { "Attempt to exclude non-track media $media" }
        viewModelScope.launch {
            excludeTracks(media)
        }
    }

    /**
     * Permanently deletes a playable media from the device's storage.
     */
    fun delete(media: MediaId) {
        requireNotNull(media.track) { "Attempt to delete non-track media $media" }
        viewModelScope.launch {
            val result = deleteTracks(listOf(media))
            _deleteEvent.value = Event(
                DeleteTracksConfirmation(media, result)
            )
        }
    }

    private fun groupByMediaType(items: List<MediaContent>): List<SearchResult> {
        val mediaByType = items.groupByTo(sortedMapOf(mediaTypeImportance)) { it.id.type }

        return buildList {
            for ((mediaType, media) in mediaByType) {
                add(SearchResult.SectionHeader(titleFor(mediaType)))
                media.mapTo(this) { content ->
                    if (content is MediaCategory) {
                        SearchResult.Browsable(
                            id = content.id,
                            title = content.title,
                            subtitle = content.subtitle,
                            iconUri = content.iconUri,
                            tracksCount = content.count
                        )
                    } else {
                        SearchResult.Track(
                            id = content.id,
                            title = content.title,
                            iconUri = content.iconUri,
                        )
                    }
                }
            }
        }
    }

    private fun titleFor(mediaType: String) = when (mediaType) {
        MediaId.TYPE_TRACKS -> R.string.all_music
        MediaId.TYPE_ALBUMS -> R.string.action_albums
        MediaId.TYPE_ARTISTS -> R.string.action_artists
        MediaId.TYPE_PLAYLISTS -> R.string.action_playlists
        else -> error("Unexpected media type in search results: $mediaType")
    }
}
