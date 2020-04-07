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

package fr.nihilus.music.devmenu.features

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.spotify.manager.SpotifyManager
import javax.inject.Inject

internal class UnlinkedTrackViewModel @Inject constructor(
    private val manager: SpotifyManager
) : ViewModel() {

    /**
     * The list of tracks that are not in sync with Spotify (yet).
     */
    val unlinkedTracks: LiveData<LoadRequest<List<Track>>> = liveData {
        emit(LoadRequest.Pending)

        val unlinkedTracks = manager.listUnlinkedTracks()
        emit(LoadRequest.Success(unlinkedTracks))
    }
}