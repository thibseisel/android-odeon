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
import fr.nihilus.music.media.CATEGORY_PLAYLISTS
import fr.nihilus.music.media.command.EditPlaylistCommand
import fr.nihilus.music.media.musicIdFrom
import fr.nihilus.music.media.utils.MediaID
import fr.nihilus.music.ui.Event
import fr.nihilus.music.ui.LoadRequest
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class PlaylistEditionResult {
    data class Success(val trackCount: Int, val playlistTitle: CharSequence?) : PlaylistEditionResult()
    data class AlreadyTaken(val requestedPlaylistName: String) : PlaylistEditionResult()
    data class NonExistingPlaylist(val requestedPlaylistId: String) : PlaylistEditionResult()
}

class AddToPlaylistViewModel
@Inject constructor(
    private val connection: MediaBrowserConnection
) : BaseViewModel() {

    private val _targetPlaylists = MutableLiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>>()
    val targetPlaylists: LiveData<LoadRequest<List<MediaBrowserCompat.MediaItem>>>
        get() = _targetPlaylists

    private val _playlistUpdateResult = MutableLiveData<Event<PlaylistEditionResult>>()
    val playlistUpdateResult: LiveData<Event<PlaylistEditionResult>>
        get() = _playlistUpdateResult

    init {
        launch {
            _targetPlaylists.postValue(LoadRequest.Pending())
            connection.subscribe(CATEGORY_PLAYLISTS).consumeEach { playlistUpdates ->
                val userCreatedPlaylists = playlistUpdates.filter {
                    MediaID.getIdRoot(it.mediaId!!) == CATEGORY_PLAYLISTS
                }

                _targetPlaylists.postValue(LoadRequest.Success(userCreatedPlaylists))
            }
        }
    }

    fun addTracksToPlaylist(
        targetPlaylist: MediaBrowserCompat.MediaItem,
        addedTracks: Array<MediaBrowserCompat.MediaItem>
    ) {
        launch {
            val playlistId = MediaID.categoryValueOf(targetPlaylist.mediaId!!).toLong()
            val newTrackIds = LongArray(addedTracks.size) {
                musicIdFrom(addedTracks[it].mediaId)?.toLong() ?: -1L
            }

            val params = Bundle(2).apply {
                putLong(EditPlaylistCommand.PARAM_PLAYLIST_ID, playlistId)
                putLongArray(EditPlaylistCommand.PARAM_NEW_TRACKS, newTrackIds)
            }

            val (resultCode, _) = connection.sendCommand(EditPlaylistCommand.CMD_NAME, params)
            when (resultCode) {
                R.id.abc_result_success -> {
                    // TODO The command should send the number of added tracks
                    _playlistUpdateResult.value = Event(
                        PlaylistEditionResult.Success(
                            addedTracks.size,
                            targetPlaylist.description.title
                        )
                    )
                }

                EditPlaylistCommand.CODE_ERROR_PLAYLIST_NOT_EXISTS -> {
                    _playlistUpdateResult.value = Event(
                        PlaylistEditionResult.NonExistingPlaylist(
                            targetPlaylist.mediaId!!
                        )
                    )
                }
            }
        }
    }
}
