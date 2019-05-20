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
import fr.nihilus.music.media.actions.CustomActions
import fr.nihilus.music.ui.Event
import kotlinx.coroutines.launch
import javax.inject.Inject

class NewPlaylistViewModel
@Inject constructor(
    private val connection: MediaBrowserConnection
) : BaseViewModel() {

    private val _createPlaylistResult = MutableLiveData<Event<PlaylistEditionResult>>()
    val createPlaylistResult: LiveData<Event<PlaylistEditionResult>>
        get() = _createPlaylistResult

    fun createPlaylist(playlistName: String, members: Array<MediaBrowserCompat.MediaItem>) {
        launch {
            val membersTrackIds = Array(members.size) { members[it].mediaId }

            val params = Bundle(2).apply {
                putString(CustomActions.EXTRA_TITLE, playlistName)
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, membersTrackIds)
            }

            connection.executeAction(CustomActions.ACTION_MANAGE_PLAYLIST, params)
            _createPlaylistResult.value = Event(
                PlaylistEditionResult.Success(
                    members.size,
                    playlistName
                )
            )
        }
    }
}