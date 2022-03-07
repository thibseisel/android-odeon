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
import dagger.hilt.android.lifecycle.HiltViewModel
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.ui.actions.DeleteTracksAction
import fr.nihilus.music.media.provider.DeleteTracksResult
import fr.nihilus.music.media.usage.DisposableTrack
import fr.nihilus.music.media.usage.UsageManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
internal class CleanupViewModel @Inject constructor(
    private val deleteAction: DeleteTracksAction,
    usageManager: UsageManager
) : ViewModel() {

    private val pendingEvent = MutableStateFlow<DeleteTracksResult?>(null)
    val state: LiveData<ViewState> = combine(
        usageManager.getDisposableTracks().onStart { emit(emptyList()) },
        pendingEvent
    ) { tracks, event -> ViewState(tracks, event) }
        .asLiveData()

    fun deleteTracks(selectedTrackIds: List<Long>) {
        viewModelScope.launch {
            val targetTrackIds = selectedTrackIds.map { trackId ->
                MediaId(TYPE_TRACKS, CATEGORY_ALL, trackId)
            }
            pendingEvent.value = deleteAction.delete(targetTrackIds)
        }
    }

    fun acknowledgeResult() {
        pendingEvent.value = null
    }

    data class ViewState(
        val tracks: List<DisposableTrack>,
        val result: DeleteTracksResult?,
    )
}
