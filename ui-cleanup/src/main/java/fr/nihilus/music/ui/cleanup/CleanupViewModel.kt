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

package fr.nihilus.music.ui.cleanup

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.actions.DeleteTracksAction
import fr.nihilus.music.media.usage.DisposableTrack
import fr.nihilus.music.media.usage.UsageManager
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

internal class CleanupViewModel @Inject constructor(
    private val deleteAction: DeleteTracksAction,
    usageManager: UsageManager
) : ViewModel() {

    val tracks: LiveData<LoadRequest<List<DisposableTrack>>> =
        usageManager.getDisposableTracks()
            .map { LoadRequest.Success(it) as LoadRequest<List<DisposableTrack>> }
            .onStart { emit(LoadRequest.Pending) }
            .asLiveData()

    fun deleteTracks(selectedTracks: List<DisposableTrack>) {
        viewModelScope.launch {
            val targetTrackIds = selectedTracks.map {
                MediaId(TYPE_TRACKS, CATEGORY_ALL, it.trackId)
            }
            deleteAction.delete(targetTrackIds)
        }
    }
}