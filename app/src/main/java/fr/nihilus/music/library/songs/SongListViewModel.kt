/*
 * Copyright 2018 Thibault Seisel
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

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.nihilus.music.base.BrowsableContentViewModel
import fr.nihilus.music.client.MediaBrowserConnection
import fr.nihilus.music.media.MediaId
import fr.nihilus.music.media.actions.CustomActions
import fr.nihilus.music.ui.Event
import kotlinx.coroutines.launch
import javax.inject.Inject

class SongListViewModel
@Inject constructor(
    private val connection: MediaBrowserConnection
) : BrowsableContentViewModel(connection) {

    private val _deleteTracksConfirmation = MutableLiveData<Event<Int>>()
    val deleteTracksConfirmation: LiveData<Event<Int>>
        get() = _deleteTracksConfirmation

    init {
        observeChildren(MediaId.ALL_TRACKS)
    }

    fun onSongSelected(song: MediaBrowserCompat.MediaItem) {
        launch {
            connection.playFromMediaId(song.mediaId!!)
        }
    }

    fun deleteSongs(songsToDelete: List<MediaBrowserCompat.MediaItem>) {
        launch {
            val trackMediaIds = Array(songsToDelete.size) { position ->
                songsToDelete[position].mediaId
            }

            val parameters = Bundle(1)
            parameters.putStringArray(CustomActions.EXTRA_MEDIA_IDS, trackMediaIds)
            val result = connection.executeAction(CustomActions.ACTION_DELETE_MEDIA, parameters)

            val deletedTracksCount = result?.getInt(CustomActions.RESULT_TRACK_COUNT) ?: 0
            _deleteTracksConfirmation.value = Event(deletedTracksCount)
        }
    }
}