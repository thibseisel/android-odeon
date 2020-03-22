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
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
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
import java.util.*
import javax.inject.Inject

private const val MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000L
private const val MAX_QUEUE_SIZE = 10
private const val UNKNOWN_QUEUE_ID = MediaSessionCompat.QueueItem.UNKNOWN_ID.toLong()

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

    private val window = Timeline.Window()
    private var activeQueueItemId = MediaSessionCompat.QueueItem.UNKNOWN_ID.toLong()

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

    override fun getSupportedQueueNavigatorActions(player: Player): Long {
        var enableSkipTo = false
        var enablePrevious = false
        var enableNext = false

        val timeline = player.currentTimeline
        if (!timeline.isEmpty && !player.isPlayingAd) {
            timeline.getWindow(player.currentWindowIndex, window)
            enableSkipTo = timeline.windowCount > 1
            enablePrevious = window.isSeekable || !window.isDynamic || player.hasPrevious()
            enableNext = window.isDynamic || player.hasNext()
        }

        var actions = 0L
        if (enableSkipTo) {
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
        }
        if (enablePrevious) {
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        }
        if (enableNext) {
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        }

        return actions
    }

    override fun onTimelineChanged(player: Player) {
        publishFloatingQueueWindow(player)

        if (!player.currentTimeline.isEmpty) {
            onUpdateMediaSessionMetadata(player)
        }
    }

    override fun onCurrentWindowIndexChanged(player: Player) {
        if (activeQueueItemId == UNKNOWN_QUEUE_ID || player.currentTimeline.windowCount > MAX_QUEUE_SIZE) {
            publishFloatingQueueWindow(player)
        } else if (!player.currentTimeline.isEmpty) {
            activeQueueItemId = player.currentWindowIndex.toLong()
        }

        prefs.lastQueueIndex = player.currentWindowIndex

        if (!player.currentTimeline.isEmpty) {
            onUpdateMediaSessionMetadata(player)
        }
    }

    override fun getActiveQueueItemId(player: Player?): Long = activeQueueItemId

    override fun onSkipToPrevious(player: Player, dispatcher: ControlDispatcher) {
        val timeline = player.currentTimeline
        if (timeline.isEmpty || player.isPlayingAd) {
            return
        }

        val windowIndex = player.currentWindowIndex
        timeline.getWindow(windowIndex, window)
        val previousWindowIndex = player.previousWindowIndex
        if (previousWindowIndex != C.INDEX_UNSET
            && (player.currentPosition <= MAX_POSITION_FOR_SEEK_TO_PREVIOUS ||
                    (window.isDynamic && !window.isSeekable))) {
            dispatcher.dispatchSeekTo(player, previousWindowIndex, C.TIME_UNSET)
        } else {
            dispatcher.dispatchSeekTo(player, windowIndex, 0L)
        }
    }

    override fun onSkipToQueueItem(player: Player, dispatcher: ControlDispatcher, id: Long) {
        val timeline = player.currentTimeline
        if (timeline.isEmpty || player.isPlayingAd) {
            return
        }

        val windowIndex = id.toInt()
        if (windowIndex in 0 until timeline.windowCount) {
            dispatcher.dispatchSeekTo(player, windowIndex, C.TIME_UNSET)
        }
    }

    override fun onSkipToNext(player: Player, dispatcher: ControlDispatcher) {
        val timeline = player.currentTimeline
        if (timeline.isEmpty || player.isPlayingAd) {
            return
        }

        val windowIndex = player.currentWindowIndex
        val nextWindowIndex = player.nextWindowIndex
        if (nextWindowIndex != C.INDEX_UNSET) {
            dispatcher.dispatchSeekTo(player, nextWindowIndex, C.TIME_UNSET)
        } else if (timeline.getWindow(windowIndex, window).isDynamic) {
            dispatcher.dispatchSeekTo(player, windowIndex, C.TIME_UNSET)
        }
    }

    private fun publishFloatingQueueWindow(player: Player) {
        val timeline = player.currentTimeline
        if (timeline.isEmpty) {
            mediaSession.setQueue(emptyList())
            activeQueueItemId = UNKNOWN_QUEUE_ID
            return
        }

        val queue = ArrayDeque<MediaSessionCompat.QueueItem>()
        val queueSize = timeline.windowCount.coerceAtMost(MAX_QUEUE_SIZE)

        // Add the active queue item.
        val currentWindowIndex = player.currentWindowIndex
        queue += MediaSessionCompat.QueueItem(
            getMediaDescription(player, currentWindowIndex),
            currentWindowIndex.toLong()
        )

        // Fill queue alternating with next and/or previous queue items.
        var firstWindowIndex = currentWindowIndex
        var lastWindowIndex = currentWindowIndex
        val shuffleModeEnabled = player.shuffleModeEnabled

        while ((firstWindowIndex != C.INDEX_UNSET || lastWindowIndex != C.INDEX_UNSET)
            && queue.size < queueSize) {
            // Begin with next to has a longer tail than head if an even sized queue needs to be trimmed.
            if (lastWindowIndex != C.INDEX_UNSET) {
                lastWindowIndex = timeline.getNextWindowIndex(lastWindowIndex, Player.REPEAT_MODE_OFF, shuffleModeEnabled)
                if (lastWindowIndex != C.INDEX_UNSET) {
                    queue.add(
                        MediaSessionCompat.QueueItem(
                            getMediaDescription(player, lastWindowIndex),
                            lastWindowIndex.toLong()
                        )
                    )
                }
            }

            if (firstWindowIndex != C.INDEX_UNSET && queue.size < queueSize) {
                firstWindowIndex = timeline.getPreviousWindowIndex(firstWindowIndex, Player.REPEAT_MODE_OFF, shuffleModeEnabled)
                if (firstWindowIndex != C.INDEX_UNSET) {
                    queue.addFirst(
                        MediaSessionCompat.QueueItem(
                            getMediaDescription(player, firstWindowIndex),
                            firstWindowIndex.toLong()
                        )
                    )
                }
            }

            mediaSession.setQueue(queue.toList())
            activeQueueItemId = currentWindowIndex.toLong()
        }
    }

    private fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
        val bufferWindow = player.currentTimeline.getWindow(windowIndex, window)
        return bufferWindow.tag as MediaDescriptionCompat
    }

    private fun onUpdateMediaSessionMetadata(player: Player) {
        val activeMedia = (player.currentTag as? MediaDescriptionCompat) ?: return
        producer.offer(activeMedia)
    }
}