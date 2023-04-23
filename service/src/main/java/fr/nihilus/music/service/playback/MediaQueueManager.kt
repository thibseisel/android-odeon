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

package fr.nihilus.music.service.playback

import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import dagger.hilt.android.scopes.ServiceScoped
import fr.nihilus.music.core.settings.Settings
import fr.nihilus.music.media.AudioTrack
import fr.nihilus.music.service.MediaSessionConnector
import java.util.ArrayDeque
import javax.inject.Inject

private const val MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000L
private const val MAX_QUEUE_SIZE = 10
private const val UNKNOWN_QUEUE_ID = MediaSessionCompat.QueueItem.UNKNOWN_ID.toLong()

/**
 * Handle queue navigation actions and update the media session queue.
 */
@ServiceScoped
internal class MediaQueueManager @Inject constructor(
    private val mediaSession: MediaSessionCompat,
    private val prefs: Settings
) : MediaSessionConnector.QueueNavigator {

    private val window = Timeline.Window()
    private var activeQueueItemId = MediaSessionCompat.QueueItem.UNKNOWN_ID.toLong()

    override fun getSupportedQueueNavigatorActions(player: Player): Long {
        var enableSkipTo = false
        var enablePrevious = false
        var enableNext = false

        val timeline = player.currentTimeline
        if (!timeline.isEmpty && !player.isPlayingAd) {
            timeline.getWindow(player.currentMediaItemIndex, window)
            enableSkipTo = timeline.windowCount > 1
            enablePrevious =
                player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)
                        || !window.isLive()
                        || player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)
            enableNext = (window.isLive() && window.isDynamic)
                    || player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)
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
    }

    override fun onCurrentMediaItemIndexChanged(player: Player) {
        if (activeQueueItemId == UNKNOWN_QUEUE_ID || player.currentTimeline.windowCount > MAX_QUEUE_SIZE) {
            publishFloatingQueueWindow(player)
        } else if (!player.currentTimeline.isEmpty) {
            activeQueueItemId = player.currentMediaItemIndex.toLong()
        }

        prefs.lastQueueIndex = player.currentMediaItemIndex
    }

    override fun getActiveQueueItemId(player: Player?): Long = activeQueueItemId

    override fun onSkipToPrevious(player: Player) {
        val timeline = player.currentTimeline
        if (timeline.isEmpty || player.isPlayingAd) {
            return
        }

        val itemIndex = player.currentMediaItemIndex
        timeline.getWindow(itemIndex, window)
        val previousItemIndex = player.previousMediaItemIndex
        if (previousItemIndex != C.INDEX_UNSET && (player.currentPosition <= MAX_POSITION_FOR_SEEK_TO_PREVIOUS || (window.isDynamic && !window.isSeekable))) {
            player.seekTo(previousItemIndex, C.TIME_UNSET)
        } else {
            player.seekTo(itemIndex, 0L)
        }
    }

    override fun onSkipToQueueItem(player: Player, id: Long) {
        val timeline = player.currentTimeline
        if (timeline.isEmpty || player.isPlayingAd) {
            return
        }

        val itemIndex = id.toInt()
        if (itemIndex in 0 until timeline.windowCount) {
            player.seekTo(itemIndex, C.TIME_UNSET)
        }
    }

    override fun onSkipToNext(player: Player) {
        val timeline = player.currentTimeline
        if (timeline.isEmpty || player.isPlayingAd) {
            return
        }

        val itemIndex = player.currentMediaItemIndex
        val nextItemIndex = player.nextMediaItemIndex
        if (nextItemIndex != C.INDEX_UNSET) {
            player.seekTo(nextItemIndex, C.TIME_UNSET)
        } else if (timeline.getWindow(itemIndex, window).isDynamic) {
            player.seekTo(itemIndex, C.TIME_UNSET)
        }
    }

    private fun publishFloatingQueueWindow(player: Player) {
        val timeline = player.currentTimeline
        if (timeline.isEmpty) {
            mediaSession.setQueue(emptyList())
            activeQueueItemId = UNKNOWN_QUEUE_ID
            return
        }

        val builder = MediaDescriptionCompat.Builder()
        val queue = ArrayDeque<MediaSessionCompat.QueueItem>()
        val queueSize = timeline.windowCount.coerceAtMost(MAX_QUEUE_SIZE)

        // Add the active queue item.
        val currentItemIndex = player.currentMediaItemIndex
        queue += MediaSessionCompat.QueueItem(
            getMediaDescription(player, currentItemIndex, builder), currentItemIndex.toLong()
        )

        // Fill queue alternating with next and/or previous queue items.
        var firstItemIndex = currentItemIndex
        var lastItemIndex = currentItemIndex
        val shuffleModeEnabled = player.shuffleModeEnabled

        while ((firstItemIndex != C.INDEX_UNSET || lastItemIndex != C.INDEX_UNSET) && queue.size < queueSize) {
            // Begin with next to has a longer tail than head if an even sized queue needs to be trimmed.
            if (lastItemIndex != C.INDEX_UNSET) {
                lastItemIndex = timeline.getNextWindowIndex(
                    lastItemIndex, Player.REPEAT_MODE_OFF, shuffleModeEnabled
                )
                if (lastItemIndex != C.INDEX_UNSET) {
                    queue.add(
                        MediaSessionCompat.QueueItem(
                            getMediaDescription(player, lastItemIndex, builder),
                            lastItemIndex.toLong()
                        )
                    )
                }
            }

            if (firstItemIndex != C.INDEX_UNSET && queue.size < queueSize) {
                firstItemIndex = timeline.getPreviousWindowIndex(
                    firstItemIndex, Player.REPEAT_MODE_OFF, shuffleModeEnabled
                )
                if (firstItemIndex != C.INDEX_UNSET) {
                    queue.addFirst(
                        MediaSessionCompat.QueueItem(
                            getMediaDescription(player, firstItemIndex, builder),
                            firstItemIndex.toLong()
                        )
                    )
                }
            }

            mediaSession.setQueue(queue.toList())
            activeQueueItemId = currentItemIndex.toLong()
        }
    }

    private fun getMediaDescription(
        player: Player, itemIndex: Int, builder: MediaDescriptionCompat.Builder
    ): MediaDescriptionCompat {
        val currentItem = player.getMediaItemAt(itemIndex)
        val track = currentItem.localConfiguration?.tag as AudioTrack

        return builder.setMediaId(track.id.encoded).setTitle(track.title).setSubtitle(track.artist)
            .setIconUri(track.iconUri).build()
    }
}
