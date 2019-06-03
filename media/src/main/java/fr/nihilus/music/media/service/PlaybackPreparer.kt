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

package fr.nihilus.music.media.service

import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat.QueueItem
import android.support.v4.media.session.PlaybackStateCompat
import fr.nihilus.music.media.InvalidMediaException
import fr.nihilus.music.media.MediaId
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.MediaSettings
import fr.nihilus.music.media.playback.MusicPlayer
import fr.nihilus.music.media.playback.skipTo
import fr.nihilus.music.media.toMediaId
import fr.nihilus.music.media.tree.BrowserTree
import timber.log.Timber

@ExperimentalMediaApi
internal interface PlaybackPreparer {
    val supportedPrepareActions: Long
    suspend fun onPrepare()
    suspend fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?)
    suspend fun onPrepareFromSearch(query: String?, extras: Bundle?)
}