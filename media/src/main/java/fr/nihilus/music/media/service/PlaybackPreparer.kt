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
import fr.nihilus.music.media.MediaId
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.MediaSettings
import fr.nihilus.music.media.playback.MusicPlayer
import fr.nihilus.music.media.playback.skipTo
import fr.nihilus.music.media.tree.BrowserTree

@ExperimentalMediaApi
internal interface PlaybackPreparer {
    val supportedPrepareActions: Long
    suspend fun onPrepare()
    suspend fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?)
    suspend fun onPrepareFromSearch(query: String?, extras: Bundle?)
}

@Suppress("unused")
@ExperimentalMediaApi
internal class PlaybackPreparerImpl(
    private val player: MusicPlayer,
    private val mediaTree: BrowserTree,
    private val settings: MediaSettings
) : PlaybackPreparer {

    override val supportedPrepareActions: Long
        get() = PlaybackStateCompat.ACTION_PREPARE or
                PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID

    override suspend fun onPrepare() {
        val lastQueueId = settings.queueCounter
        val mediaToPrepare = settings.lastPlayedMediaId?.let(MediaId.Builder::parse)
            ?: MediaId.fromParts(TYPE_TRACKS, CATEGORY_ALL)

        prepareFromMediaId(mediaToPrepare, lastQueueId)
    }

    override suspend fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
        MediaId.parseOrNull(mediaId)?.let { validMediaId ->
            prepareFromMediaId(validMediaId, ++settings.queueCounter)
        }
    }

    private suspend fun prepareFromMediaId(mediaId: MediaId, queueIdentifier: Long) {
        val parentMediaId = MediaId.fromParts(mediaId.type, mediaId.category, track = null)

        val childrenOfRequestedMediaId = mediaTree.getChildren(parentMediaId, null).orEmpty()
        val playQueue = childrenOfRequestedMediaId.asSequence()
            .filter { it.isPlayable && !it.isBrowsable }
            .mapIndexed { queueIndex, item -> QueueItem(item.description, queueIndex.toLong()) }
            .toList()

        if (playQueue.isNotEmpty()) {
            player.prepare(queueIdentifier, playQueue)

            if (mediaId.track != null) {
                val indexOfFirstTrack = playQueue.indexOfFirst { mediaId.encoded == it.description.mediaId }
                player.skipTo(indexOfFirstTrack)
            }
        }
    }

    override suspend fun onPrepareFromSearch(query: String?, extras: Bundle?) {
        // Not supported at the time. Does nothing.
    }
}