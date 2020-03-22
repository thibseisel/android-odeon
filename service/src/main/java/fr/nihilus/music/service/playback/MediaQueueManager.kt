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

package fr.nihilus.music.service.playback

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import fr.nihilus.music.core.settings.Settings
import fr.nihilus.music.service.MediaSessionConnector
import fr.nihilus.music.service.ServiceScoped
import fr.nihilus.music.service.metadata.IconDownloader
import fr.nihilus.music.service.metadata.metadataProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handle queue navigation actions and update the media session queue.
 */
@ServiceScoped
internal class MediaQueueManager
@Inject constructor(
    scope: CoroutineScope,
    private val mediaSession: MediaSessionCompat,
    private val prefs: Settings,
    downloader: IconDownloader
) : MediaSessionConnector.QueueNavigator {

    private val producer: SendChannel<MediaDescriptionCompat>
    init {
        val metadata = Channel<MediaMetadataCompat>()
        producer = scope.metadataProducer(downloader, metadata)

        scope.launch {
            metadata.consumeEach { upToDateMetadata ->
                mediaSession.setMetadata(upToDateMetadata)
            }
        }
    }

    /**
     * Implementation of [MediaSessionConnector.QueueNavigator] to delegate to.
     * This allows overriding final methods of [TimelineQueueNavigator].
     */
    private val navigator = object : TimelineQueueNavigator(mediaSession) {
        private val windowBuffer = Timeline.Window()

        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            player.currentTimeline.getWindow(windowIndex, windowBuffer)
            return windowBuffer.tag as MediaDescriptionCompat
        }
    }

    override fun getSupportedQueueNavigatorActions(player: Player): Long =
        navigator.getSupportedQueueNavigatorActions(player)

    override fun onSkipToPrevious(player: Player, dispatcher: ControlDispatcher) {
        navigator.onSkipToPrevious(player, dispatcher)
    }

    override fun onSkipToNext(player: Player, dispatcher: ControlDispatcher) {
        navigator.onSkipToNext(player, dispatcher)
    }

    override fun onSkipToQueueItem(player: Player, dispatcher: ControlDispatcher, id: Long) {
        navigator.onSkipToQueueItem(player, dispatcher, id)
    }

    override fun onCurrentWindowIndexChanged(player: Player) {
        navigator.onCurrentWindowIndexChanged(player)
        prefs.lastQueueIndex = player.currentWindowIndex

        if (!player.currentTimeline.isEmpty) {
            onUpdateMediaSessionMetadata(player)
        }
    }

    override fun getActiveQueueItemId(player: Player?) = navigator.getActiveQueueItemId(player)

    override fun onTimelineChanged(player: Player) {
        navigator.onTimelineChanged(player)

        if (!player.currentTimeline.isEmpty) {
            onUpdateMediaSessionMetadata(player)
        }
    }

    private fun onUpdateMediaSessionMetadata(player: Player) {
        val activeMedia = (player.currentTag as? MediaDescriptionCompat) ?: return
        producer.offer(activeMedia)
    }
}