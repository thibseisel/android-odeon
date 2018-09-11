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

package fr.nihilus.music.media.playback

import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import fr.nihilus.music.media.MediaSettings
import fr.nihilus.music.media.di.ServiceScoped
import fr.nihilus.music.media.musicIdFrom
import fr.nihilus.music.media.repo.MusicRepository
import fr.nihilus.music.media.service.AlbumArtLoader
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject

/**
 * Handle queue navigation actions and update the media session queue.
 */
@ServiceScoped
internal class MediaQueueManager
@Inject constructor(
    private val mediaSession: MediaSessionCompat,
    private val repository: MusicRepository,
    private val prefs: MediaSettings,
    private val iconLoader: AlbumArtLoader
) : MediaSessionConnector.QueueNavigator {

    private var lastMusicId: String? = null

    /**
     * Implementation of [MediaSessionConnector.QueueNavigator] to delegate to.
     * This allows overriding final methods of [TimelineQueueNavigator].
     */
    private val navigator = object : TimelineQueueNavigator(mediaSession) {
        private val windowBuffer = Timeline.Window()

        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            player.currentTimeline.getWindow(windowIndex, windowBuffer, true)
            return windowBuffer.tag as MediaDescriptionCompat
        }
    }

    /**
     * Fixes a bug in ExoPlayer 2.8.1 where skipping to previous/next media
     * in a in a [ConcatenatingMediaSource] cannot be performed when in shuffle mode.
     */
    override fun getSupportedQueueNavigatorActions(player: Player?): Long {
        if (player == null || player.currentTimeline.windowCount < 2) {
            return 0L
        }

        if (player.repeatMode != Player.REPEAT_MODE_OFF) {
            return PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM
        }

        return PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM or when {
            player.previousWindowIndex == C.INDEX_UNSET -> PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            player.nextWindowIndex == C.INDEX_UNSET -> PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            else -> PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
        }
    }

    /**
     * Called when skipping to the previous song in timeline.
     * When repeat mode is REPEAT_ONE, allow skipping to previous item by temporarily setting
     * player's repeat mode to REPEAT_ALL.
     */
    override fun onSkipToPrevious(player: Player?) {
        if (player != null && player.repeatMode == Player.REPEAT_MODE_ONE) {
            player.repeatMode = Player.REPEAT_MODE_ALL
            navigator.onSkipToPrevious(player)
            player.repeatMode = Player.REPEAT_MODE_ONE
        } else {
            navigator.onSkipToPrevious(player)
        }
    }

    /**
     * Called when skipping to the next song in timeline.
     * When repeat mode is REPEAT_ONE, allow skipping to next item by temporarily setting
     * player's repeat mode to REPEAT_ALL.
     */
    override fun onSkipToNext(player: Player?) {
        if (player != null && player.repeatMode == Player.REPEAT_MODE_ONE) {
            player.repeatMode = Player.REPEAT_MODE_ALL
            navigator.onSkipToNext(player)
            player.repeatMode = Player.REPEAT_MODE_ONE
        } else {
            navigator.onSkipToNext(player)
        }
    }

    override fun onSkipToQueueItem(player: Player?, id: Long) {
        navigator.onSkipToQueueItem(player, id)
    }

    override fun onCurrentWindowIndexChanged(player: Player) {
        navigator.onCurrentWindowIndexChanged(player)
        onUpdateMediaSessionMetadata(player)
    }

    override fun getActiveQueueItemId(player: Player?) = navigator.getActiveQueueItemId(player)

    override fun onTimelineChanged(player: Player?) = navigator.onTimelineChanged(player)

    override fun getCommands(): Array<String>? = null

    override fun onCommand(
        player: Player?,
        command: String?,
        extras: Bundle?,
        cb: ResultReceiver?
    ) = Unit

    private fun onUpdateMediaSessionMetadata(player: Player) {
        val activeMediaId = (player.currentTag as MediaDescriptionCompat).mediaId
        prefs.lastPlayedMediaId = activeMediaId
        val musicId = checkNotNull(musicIdFrom(activeMediaId)) {
            "Each playable track should have a music ID"
        }

        if (lastMusicId != musicId) {
            // Only update metadata if it has really changed.
            repository.getMetadata(musicId)
                .subscribeOn(Schedulers.io())
                .flatMap { iconLoader.loadIntoMetadata(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mediaSession::setMetadata)
        }

        // Remember the last change in metadata
        lastMusicId = musicId
    }
}