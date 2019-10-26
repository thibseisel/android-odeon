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

package fr.nihilus.music.library.cleanup

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.nihilus.music.common.media.CustomActions
import fr.nihilus.music.common.media.MediaId
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.client.BrowserClient
import fr.nihilus.music.core.ui.client.MediaSubscriptionException
import fr.nihilus.music.core.ui.extensions.consumeAsLiveData
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import javax.inject.Inject

class CleanupViewModel
@Inject constructor(
    private val client: BrowserClient
) : ViewModel() {

    val tracks: LiveData<LoadRequest<List<MediaItem>>> by lazy {
        client.getChildren(MediaId.encode(MediaId.TYPE_TRACKS, MediaId.CATEGORY_DISPOSABLE))
            .map { LoadRequest.Success(it) as LoadRequest<List<MediaItem>> }
            .onStart { emit(LoadRequest.Pending) }
            .catch { if (it is MediaSubscriptionException) emit(LoadRequest.Error(it)) }
            .consumeAsLiveData(viewModelScope)
    }

    fun deleteTracks(selectedTracks: List<MediaItem>) {
        viewModelScope.launch {
            client.executeAction(CustomActions.ACTION_DELETE_MEDIA, Bundle(1).apply {
                val deletedMediaIds = Array(selectedTracks.size) { selectedTracks[it].mediaId }
                putStringArray(CustomActions.EXTRA_MEDIA_IDS, deletedMediaIds)
            })
        }
    }
}