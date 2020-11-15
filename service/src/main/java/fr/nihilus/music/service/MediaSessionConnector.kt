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

package fr.nihilus.music.service

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.util.component1
import androidx.core.util.component2
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.util.ErrorMessageProvider
import com.google.android.exoplayer2.util.Util
import fr.nihilus.music.service.metadata.IconDownloader
import fr.nihilus.music.service.metadata.metadataProducer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val REWIND_MS = 5000L
private const val FAST_FORWARD_MS = 15000L

internal const val PREPARER_ACTIONS = (
        PlaybackStateCompat.ACTION_PREPARE
                or PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID
                or PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH
                or PlaybackStateCompat.ACTION_PREPARE_FROM_URI
                or PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID
                or PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH
                or PlaybackStateCompat.ACTION_PLAY_FROM_URI
        )

private const val BASE_PLAYBACK_ACTIONS = (
        PlaybackStateCompat.ACTION_PLAY_PAUSE
                or PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_STOP
                or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
                or PlaybackStateCompat.ACTION_SET_REPEAT_MODE
        )

internal const val PLAYBACK_ACTIONS = (
        PlaybackStateCompat.ACTION_PLAY_PAUSE
                or PlaybackStateCompat.ACTION_PLAY
                or PlaybackStateCompat.ACTION_PAUSE
                or PlaybackStateCompat.ACTION_SEEK_TO
                or PlaybackStateCompat.ACTION_FAST_FORWARD
                or PlaybackStateCompat.ACTION_REWIND
                or PlaybackStateCompat.ACTION_STOP
                or PlaybackStateCompat.ACTION_SET_REPEAT_MODE
                or PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE
        )

@ServiceScoped
internal class MediaSessionConnector @Inject constructor(
    scope: CoroutineScope,
    private val mediaSession: MediaSessionCompat,
    private val player: Player,
    private val playbackPreparer: PlaybackPreparer,
    private val queueNavigator: QueueNavigator,
    private val errorMessageProvider: ErrorMessageProvider<ExoPlaybackException>,
    private val iconDownloader: IconDownloader
) {
    private val looper = Util.getCurrentOrMainLooper()
    private val componentListener = ComponentListener()

    private val metadataProducer = scope.startMetadataUpdater()

    init {
        require(player.applicationLooper === looper)
        player.addListener(componentListener)
        mediaSession.setCallback(componentListener, Handler(looper))
        invalidateMediaSessionPlaybackState()
        invalidateMediaSessionMetadata()
    }

    private fun invalidateMediaSessionMetadata() {
        val nowPlayingTrack = player.currentTag as? AudioTrack
        if (nowPlayingTrack != null) {
            metadataProducer.offer(nowPlayingTrack)
        }
    }

    private fun invalidateMediaSessionPlaybackState() {
        val builder = PlaybackStateCompat.Builder()

        val playbackError = player.playerError
        val reportError = playbackError != null
        val sessionPlaybackState = when {
            reportError -> PlaybackStateCompat.STATE_ERROR
            else -> getMediaSessionPlaybackState(player.playbackState, player.playWhenReady)
        }

        if (playbackError != null) {
            val (errorCode, errorMessage) = errorMessageProvider.getErrorMessage(playbackError)
            builder.setErrorMessage(errorCode, errorMessage)
        }

        val activeQueueItemId = queueNavigator.getActiveQueueItemId(player)
        val playbackParameters = player.playbackParameters
        val sessionPlaybackSpeed = if (player.isPlaying) playbackParameters.speed else 0f

        builder
            .setActions(buildPrepareActions() or buildPlaybackActions(player))
            .setActiveQueueItemId(activeQueueItemId)
            .setBufferedPosition(player.bufferedPosition)
            .setState(
                sessionPlaybackState,
                player.currentPosition,
                sessionPlaybackSpeed,
                SystemClock.elapsedRealtime()
            )

        mediaSession.setRepeatMode(when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> PlaybackStateCompat.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ALL -> PlaybackStateCompat.REPEAT_MODE_ALL
            else -> PlaybackStateCompat.REPEAT_MODE_NONE
        })

        mediaSession.setShuffleMode(when (player.shuffleModeEnabled) {
            true -> PlaybackStateCompat.SHUFFLE_MODE_ALL
            false -> PlaybackStateCompat.SHUFFLE_MODE_NONE
        })

        mediaSession.setPlaybackState(builder.build())
    }

    private fun invalidateMediaSessionQueue() {
        queueNavigator.onTimelineChanged(player)
    }

    private fun buildPrepareActions(): Long =
        PREPARER_ACTIONS and playbackPreparer.getSupportedPrepareActions()

    private fun buildPlaybackActions(player: Player): Long {
        var enableSeeking = false
        var enableRewind = false
        var enableFastForward = false

        val timeline = player.currentTimeline
        if (!timeline.isEmpty && !player.isPlayingAd) {
            enableSeeking = player.isCurrentWindowSeekable
            enableRewind = enableSeeking
            enableFastForward = enableSeeking
        }

        var playbackActions = BASE_PLAYBACK_ACTIONS
        if (enableSeeking) {
            playbackActions = playbackActions or PlaybackStateCompat.ACTION_SEEK_TO
        }
        if (enableFastForward) {
            playbackActions = playbackActions or PlaybackStateCompat.ACTION_FAST_FORWARD
        }
        if (enableRewind) {
            playbackActions = playbackActions or PlaybackStateCompat.ACTION_REWIND
        }

        playbackActions = playbackActions and PLAYBACK_ACTIONS

        var actions = playbackActions
        actions = actions or queueNavigator.getSupportedQueueNavigatorActions(player)
        return actions
    }

    private fun canDispatchPlaybackAction(action: Long): Boolean =
        PLAYBACK_ACTIONS and action != 0L

    private fun canDispatchToPlaybackPreparer(action: Long): Boolean =
        playbackPreparer.getSupportedPrepareActions() and action != 0L

    private fun canDispatchToQueueNavigator(action: Long): Boolean =
        queueNavigator.getSupportedQueueNavigatorActions(player) and action != 0L

    private fun rewind(player: Player) {
        if (player.isCurrentWindowSeekable) {
            seekToOffset(player, -REWIND_MS)
        }
    }

    private fun fastForward(player: Player) {
        if (player.isCurrentWindowSeekable) {
            seekToOffset(player, FAST_FORWARD_MS)
        }
    }

    private fun seekToOffset(player: Player, offsetMs: Long) {
        var positionMs = player.currentPosition + offsetMs
        val durationMs = player.duration
        if (durationMs != C.TIME_UNSET) {
            positionMs = positionMs.coerceAtMost(durationMs)
        }

        positionMs = positionMs.coerceAtLeast(0)
        seekTo(player, player.currentWindowIndex, positionMs)
    }

    private fun seekTo(player: Player, windowIndex: Int, positionMs: Long) {
        player.seekTo(windowIndex, positionMs)
    }

    private fun getMediaSessionPlaybackState(
        exoPlayerPlaybackState: Int,
        playWhenReady: Boolean
    ): Int = when (exoPlayerPlaybackState) {
        Player.STATE_BUFFERING -> when {
            playWhenReady -> PlaybackStateCompat.STATE_BUFFERING
            else -> PlaybackStateCompat.STATE_PAUSED
        }
        Player.STATE_READY -> when {
            playWhenReady -> PlaybackStateCompat.STATE_PLAYING
            else -> PlaybackStateCompat.STATE_PAUSED
        }
        Player.STATE_ENDED -> PlaybackStateCompat.STATE_STOPPED
        else -> PlaybackStateCompat.STATE_NONE
    }

    private fun CoroutineScope.startMetadataUpdater(): SendChannel<AudioTrack> {
        val metadataOutput = Channel<MediaMetadataCompat>()
        val producerActor = metadataProducer(iconDownloader, metadataOutput)

        launch {
            metadataOutput.consumeEach {
                mediaSession.setMetadata(it)
            }
        }

        return producerActor
    }

    interface PlaybackPreparer {
        fun getSupportedPrepareActions(): Long
        fun onPrepare(playWhenReady: Boolean)
        fun onPrepareFromMediaId(mediaId: String?, playWhenReady: Boolean, extras: Bundle?)
        fun onPrepareFromSearch(query: String?, playWhenReady: Boolean, extras: Bundle?)
        fun onPrepareFromUri(uri: Uri?, playWhenReady: Boolean, extras: Bundle?)
    }

    interface QueueNavigator {
        fun getSupportedQueueNavigatorActions(player: Player): Long
        fun onTimelineChanged(player: Player)
        fun onCurrentWindowIndexChanged(player: Player)
        fun getActiveQueueItemId(player: Player?): Long
        fun onSkipToPrevious(player: Player)
        fun onSkipToNext(player: Player)
        fun onSkipToQueueItem(player: Player, id: Long)
    }

    private inner class ComponentListener : MediaSessionCompat.Callback(), Player.EventListener {
        private var currentWindowIndex = 0
        private var currentWindowCount = 0

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            val windowCount = player.currentTimeline.windowCount
            val windowIndex = player.currentWindowIndex

            queueNavigator.onTimelineChanged(player)
            invalidateMediaSessionPlaybackState()

            currentWindowCount = windowCount
            currentWindowIndex = windowIndex
            invalidateMediaSessionMetadata()
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            invalidateMediaSessionPlaybackState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            invalidateMediaSessionPlaybackState()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            invalidateMediaSessionPlaybackState()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            invalidateMediaSessionPlaybackState()
            invalidateMediaSessionQueue()
        }

        override fun onPositionDiscontinuity(reason: Int) {
            if (currentWindowIndex != player.currentWindowIndex) {
                queueNavigator.onCurrentWindowIndexChanged(player)
                currentWindowIndex = player.currentWindowIndex

                // Update playback state after queueNavigator.onCurrentWindowIndexChanged
                // has been called and before updating metadata.
                invalidateMediaSessionPlaybackState()
                invalidateMediaSessionMetadata()
                return
            }

            invalidateMediaSessionPlaybackState()
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            invalidateMediaSessionPlaybackState()
        }

        override fun onPlay() {
            if (canDispatchPlaybackAction(PlaybackStateCompat.ACTION_PLAY)) {
                if (player.playbackState == Player.STATE_IDLE) {
                    playbackPreparer.onPrepare(true)
                } else if (player.playbackState == Player.STATE_ENDED) {
                    seekTo(player, player.currentWindowIndex, C.TIME_UNSET)
                }

                player.playWhenReady = true
            }
        }

        override fun onPause() {
            if (canDispatchPlaybackAction(PlaybackStateCompat.ACTION_PAUSE)) {
                player.playWhenReady = false
            }
        }

        override fun onSeekTo(positionMs: Long) {
            if (canDispatchPlaybackAction(PlaybackStateCompat.ACTION_SEEK_TO)) {
                seekTo(player, player.currentWindowIndex, positionMs)
            }
        }

        override fun onFastForward() {
            if (canDispatchPlaybackAction(PlaybackStateCompat.ACTION_FAST_FORWARD)) {
                fastForward(player)
            }
        }

        override fun onRewind() {
            if (canDispatchPlaybackAction(PlaybackStateCompat.ACTION_REWIND)) {
                rewind(player)
            }
        }

        override fun onStop() {
            if (canDispatchPlaybackAction(PlaybackStateCompat.ACTION_STOP)) {
                player.stop(true)
            }
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            if (canDispatchPlaybackAction(PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE)) {
                val shuffleModeEnabled = (shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL || shuffleMode == PlaybackStateCompat.SHUFFLE_MODE_GROUP)
                player.shuffleModeEnabled = shuffleModeEnabled
            }
        }

        override fun onSetRepeatMode(repeatMode: Int) {
            if (canDispatchPlaybackAction(PlaybackStateCompat.ACTION_SET_REPEAT_MODE)) {
                val newMode = when (repeatMode) {
                    PlaybackStateCompat.REPEAT_MODE_ALL,
                    PlaybackStateCompat.REPEAT_MODE_GROUP -> Player.REPEAT_MODE_ALL
                    PlaybackStateCompat.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ONE
                    else -> Player.REPEAT_MODE_OFF
                }

                player.repeatMode = newMode
            }
        }

        override fun onSkipToNext() {
            if (canDispatchToQueueNavigator(PlaybackStateCompat.ACTION_SKIP_TO_NEXT)) {
                queueNavigator.onSkipToNext(player)
            }
        }

        override fun onSkipToPrevious() {
            if (canDispatchToQueueNavigator(PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)) {
                queueNavigator.onSkipToPrevious(player)
            }
        }

        override fun onSkipToQueueItem(id: Long) {
            if (canDispatchToQueueNavigator(PlaybackStateCompat.ACTION_SKIP_TO_QUEUE_ITEM)) {
                queueNavigator.onSkipToQueueItem(player, id)
            }
        }

        override fun onPrepare() {
            if (canDispatchToPlaybackPreparer(PlaybackStateCompat.ACTION_PREPARE)) {
                playbackPreparer.onPrepare(false)
            }
        }

        override fun onPrepareFromMediaId(mediaId: String?, extras: Bundle?) {
            if (canDispatchToPlaybackPreparer(PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID)) {
                playbackPreparer.onPrepareFromMediaId(mediaId, false, extras)
            }
        }

        override fun onPrepareFromSearch(query: String?, extras: Bundle?) {
            if (canDispatchToPlaybackPreparer(PlaybackStateCompat.ACTION_PREPARE_FROM_SEARCH)) {
                playbackPreparer.onPrepareFromSearch(query, false, extras)
            }
        }

        override fun onPrepareFromUri(uri: Uri?, extras: Bundle?) {
            if (canDispatchToPlaybackPreparer(PlaybackStateCompat.ACTION_PREPARE_FROM_URI)) {
                playbackPreparer.onPrepareFromUri(uri, false, extras)
            }
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            if (canDispatchToPlaybackPreparer(PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID)) {
                playbackPreparer.onPrepareFromMediaId(mediaId, true, extras)
            }
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            if (canDispatchToPlaybackPreparer(PlaybackStateCompat.ACTION_PLAY_FROM_SEARCH)) {
                playbackPreparer.onPrepareFromSearch(query, true, extras)
            }
        }

        override fun onPlayFromUri(uri: Uri?, extras: Bundle?) {
            if (canDispatchToPlaybackPreparer(PlaybackStateCompat.ACTION_PLAY_FROM_URI)) {
                playbackPreparer.onPrepareFromUri(uri, true, extras)
            }
        }
    }
}