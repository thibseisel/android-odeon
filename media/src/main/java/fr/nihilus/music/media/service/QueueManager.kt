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

import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import fr.nihilus.music.media.playback.MusicPlayer
import fr.nihilus.music.media.playback.UNKNOWN_TRACK_INDEX
import fr.nihilus.music.media.playback.seekTo

@ExperimentalMediaApi
internal interface QueueManager {
    fun getSupportedNavigatorActions(player: MusicPlayer): Long
    fun getActiveQueueItemId(player: MusicPlayer): Long
    fun onCurrentTrackChanged(trackIndex: Int)
    fun onTrackCompletion(completedTrackIndex: Int)
    fun onQueueUpdated(session: MediaSession)
    fun onSkipToPrevious(player: MusicPlayer)
    fun onSkipToNext(player: MusicPlayer)
    fun onSkipToQueueItem(player: MusicPlayer, itemId: Long)
}

private const val MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000L

@ExperimentalMediaApi
internal class QueueManagerImpl : QueueManager {
    private var currentQueue = emptyList<MediaSessionCompat.QueueItem>()
    private var currentQueueItemId: Long = MediaSessionCompat.QueueItem.UNKNOWN_ID.toLong()

    override fun getSupportedNavigatorActions(player: MusicPlayer): Long {
        var actions = 0L
        if (player.queueSize > 0) {
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
        }

        if (player.hasPrevious()) {
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        }

        if (player.hasNext()) {
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        }

        return actions
    }

    override fun getActiveQueueItemId(player: MusicPlayer): Long {
        return currentQueueItemId
    }

    override fun onCurrentTrackChanged(trackIndex: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onTrackCompletion(completedTrackIndex: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onQueueUpdated(session: MediaSession) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onSkipToPrevious(player: MusicPlayer) {
        if (player.queueSize == 0) return
        val previousTrackIndex = player.previousTrackIndex
        if (previousTrackIndex != UNKNOWN_TRACK_INDEX && player.currentPosition <= MAX_POSITION_FOR_SEEK_TO_PREVIOUS) {
            player.previous()
        } else {
            player.seekTo(0L)
        }
    }

    override fun onSkipToQueueItem(player: MusicPlayer, itemId: Long) {
        TODO()
    }

    override fun onSkipToNext(player: MusicPlayer) {
        if (player.queueSize == 0) return
        val nextTrackIndex = player.nextTrackIndex
        if (nextTrackIndex != UNKNOWN_TRACK_INDEX) {
            player.next()
        }
    }

}

