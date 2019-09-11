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
import fr.nihilus.music.common.media.CustomActions
import fr.nihilus.music.core.ui.client.BrowsableContentViewModel
import fr.nihilus.music.core.ui.client.MediaBrowserConnection
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

class MembersViewModel
@Inject constructor(
    private val connection: MediaBrowserConnection
): BrowsableContentViewModel(connection) {
    private var observeMembersJob: Job? = null

    fun loadTracksOfPlaylist(playlist: MediaBrowserCompat.MediaItem) {
        observeMembersJob?.cancel()
        observeMembersJob = observeChildren(playlist.mediaId!!)
    }

    fun deletePlaylist(playlist: MediaBrowserCompat.MediaItem) {
        launch {
            val params = Bundle(1).apply {
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, arrayOf(playlist.mediaId))
            }

            connection.executeAction(CustomActions.ACTION_DELETE_MEDIA, params)
        }
    }
}
