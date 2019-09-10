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

package fr.nihilus.music.library.playlists

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.nihilus.music.base.BaseViewModel
import fr.nihilus.music.client.MediaBrowserConnection
import fr.nihilus.music.common.media.CustomActions
import fr.nihilus.music.common.media.InvalidMediaException
import fr.nihilus.music.common.media.MediaId
import fr.nihilus.music.ui.Event
import fr.nihilus.music.ui.LoadRequest
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Describe the result of an action performed on a playlist.
 */
sealed class PlaylistActionResult {

    /**
     * A playlist has been successfully created.
     * @property playlistName The name given to the newly created playlist.
     */
    class Created(val playlistName: String) : PlaylistActionResult()

    /**
     * A playlist has been modified by adding some tracks.
     * @property playlistName The name of the playlist that have been modified.
     * @property addedTracksCount The number of tracks that have been added to the playlist.
     */
    class Edited(val playlistName: String, val addedTracksCount: Int) : PlaylistActionResult()
}

/**
 * A shared ViewModel to handle playlist creation and edition.
 */
class PlaylistManagementViewModel
@Inject constructor(
    private val connection: MediaBrowserConnection
) : BaseViewModel() {
    private val token = MediaBrowserConnection.ClientToken()

    private val _playlistActionResult = MutableLiveData<Event<PlaylistActionResult>>()

    private val _userPlaylists by lazy(LazyThreadSafetyMode.NONE) {
        MutableLiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>>().also {
            it.postValue(LoadRequest.Pending)
            loadUserPlaylists()
        }
    }

    init {
        connection.connect(token)
    }

    val playlistActionResult: LiveData<Event<PlaylistActionResult>>
        get() = _playlistActionResult

    val userPlaylists: LiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>>
        get() = _userPlaylists

    fun createPlaylist(playlistName: String, members: Array<MediaBrowserCompat.MediaItem>) {
        launch {
            val membersTrackIds = Array(members.size) { members[it].mediaId }

            val params = Bundle(2).apply {
                putString(CustomActions.EXTRA_TITLE, playlistName)
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, membersTrackIds)
            }

            connection.executeAction(CustomActions.ACTION_MANAGE_PLAYLIST, params)
            _playlistActionResult.value = Event(
                PlaylistActionResult.Created(playlistName)
            )
        }
    }
    fun addTracksToPlaylist(
        targetPlaylist: MediaBrowserCompat.MediaItem,
        addedTracks: Array<MediaBrowserCompat.MediaItem>
    ) {
        launch {
            val playlistId = targetPlaylist.mediaId
            val newTrackMediaIds = Array(addedTracks.size) { addedTracks[it].mediaId }

            val params = Bundle(2).apply {
                putString(CustomActions.EXTRA_PLAYLIST_ID, playlistId)
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, newTrackMediaIds)
            }

            connection.executeAction(CustomActions.ACTION_MANAGE_PLAYLIST, params)
            _playlistActionResult.value = Event(
                PlaylistActionResult.Edited(
                    targetPlaylist.description.title?.toString().orEmpty(),
                    addedTracks.size
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        connection.disconnect(token)
    }

    private fun loadUserPlaylists(): Job = launch {
        try {
            connection.subscribe(MediaId.ALL_PLAYLISTS).consumeEach { playlists ->
                _userPlaylists.postValue(LoadRequest.Success(playlists))
            }
        } catch (ime: InvalidMediaException) {
            _userPlaylists.postValue(LoadRequest.Error(ime))
        }
    }
}