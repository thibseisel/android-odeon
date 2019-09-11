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

package fr.nihilus.music.service.playback

import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.session.PlaybackStateCompat.*
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.PlaybackParameters
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector.*
import com.google.android.exoplayer2.util.ErrorMessageProvider
import fr.nihilus.music.service.ExperimentalMediaApi
import fr.nihilus.music.service.MediaSessionHolder
import fr.nihilus.music.service.RepeatMode
import javax.inject.Inject

@ExperimentalMediaApi
internal interface PlaybackControl {
    fun prepare()
    fun prepareFromMediaId(mediaId: String?)
    fun prepareFromSearch(query: String?, extras: Bundle?)
    fun playFromMediaId(mediaId: String?)
    fun playFromSearch(query: String?, extras: Bundle?)
    fun play()
    fun pause()
    fun stop()
    fun skipToNext()
    fun skipToPrevious()
    fun skipToQueueItem(queueItemId: Long)
    fun seekTo(positionMs: Long)
    fun setRepeatMode(mode: RepeatMode)
    fun setShuffleMode(mode: Int)
}

@ExperimentalMediaApi
internal class PlaybackControlImpl
@Inject constructor(
    private val session: MediaSessionHolder,
    private val player: Player,
    private val preparer: PlaybackPreparer,
    private val controller: PlaybackController,
    private val queueNavigator: QueueNavigator,
    private val errorMessageProvider: ErrorMessageProvider<ExoPlaybackException>
) : PlaybackControl {

    init {
        val listener = PlayerEventDispatcher()
        player.addListener(listener)
    }

    override fun prepare() {
        if (canDispatchToPlaybackPreparer(ACTION_PREPARE)) {
            player.stop()
            player.playWhenReady = false
            preparer.onPrepare()
        }
    }

    override fun prepareFromMediaId(mediaId: String?) {
        if (canDispatchToPlaybackPreparer(ACTION_PREPARE_FROM_MEDIA_ID)) {
            player.stop()
            player.playWhenReady = false
            preparer.onPrepareFromMediaId(mediaId, null)
        }
    }

    override fun prepareFromSearch(query: String?, extras: Bundle?) {
        if (canDispatchToPlaybackPreparer(ACTION_PREPARE_FROM_SEARCH)) {
            player.stop()
            player.playWhenReady = false
            preparer.onPrepareFromSearch(query, extras)
        }
    }

    override fun playFromMediaId(mediaId: String?) {
        if (canDispatchToPlaybackPreparer(ACTION_PLAY_FROM_MEDIA_ID)) {
            player.stop()
            player.playWhenReady = true
            preparer.onPrepareFromMediaId(mediaId, null)
        }
    }

    override fun playFromSearch(query: String?, extras: Bundle?) {
        if (canDispatchToPlaybackPreparer(ACTION_PLAY_FROM_SEARCH)) {
            player.stop()
            player.playWhenReady = true
            preparer.onPrepareFromSearch(query, extras)
        }
    }

    override fun play() {
        if (canDispatchToPlaybackController(ACTION_PLAY)) {
            controller.onPlay(player)
        }
    }

    override fun pause() {
        if (canDispatchToPlaybackController(ACTION_PAUSE)) {
            controller.onPause(player)
        }
    }

    override fun stop() {
        if (canDispatchToPlaybackController(ACTION_STOP)) {
            controller.onStop(player)
        }
    }

    override fun skipToNext() {
        if (canDispatchToQueueNavigator(ACTION_SKIP_TO_NEXT)) {
            queueNavigator.onSkipToNext(player)
        }
    }

    override fun skipToPrevious() {
        if (canDispatchToQueueNavigator(ACTION_SKIP_TO_PREVIOUS)) {
            queueNavigator.onSkipToPrevious(player)
        }
    }

    override fun skipToQueueItem(queueItemId: Long) {
        if (canDispatchToQueueNavigator(ACTION_SKIP_TO_QUEUE_ITEM)) {
            queueNavigator.onSkipToQueueItem(player, queueItemId)
        }
    }

    override fun seekTo(positionMs: Long) {
        if (canDispatchToPlaybackController(ACTION_SEEK_TO)) {
            controller.onSeekTo(player, positionMs)
        }
    }

    override fun setRepeatMode(mode: RepeatMode) {
        if (canDispatchToPlaybackController(ACTION_SET_REPEAT_MODE)) {
            controller.onSetRepeatMode(
                player, when (mode) {
                    RepeatMode.OFF -> REPEAT_MODE_NONE
                    RepeatMode.ONE -> REPEAT_MODE_ONE
                    RepeatMode.ALL -> REPEAT_MODE_ALL
                }
            )
        }
    }

    override fun setShuffleMode(mode: Int) {
        if (canDispatchToPlaybackController(ACTION_SET_SHUFFLE_MODE)) {
            controller.onSetRepeatMode(player, mode)
        }
    }

    private fun updatePlaybackState() {
        val builder = Builder()
        val playerState = player.playbackState
        val playerError = if (playerState == Player.STATE_IDLE) player.playbackError else null

        val sessionPlaybackState = if (playerError != null) STATE_ERROR else mapPlaybackState(
            playerState,
            player.playWhenReady
        )
        if (playerError != null) {
            val message = errorMessageProvider.getErrorMessage(playerError)
            builder.setErrorMessage(message.first, message.second)
        }

        val activeQueueId = queueNavigator.getActiveQueueItemId(player)
        builder.setActions(buildPlaybackActions())
            .setActiveQueueItemId(activeQueueId)
            .setBufferedPosition(player.bufferedPosition)
            .setState(
                sessionPlaybackState,
                player.currentPosition,
                player.playbackParameters.speed,
                SystemClock.elapsedRealtime()
            )
        session.publishPlaybackState(builder.build())
    }

    @State
    private fun mapPlaybackState(playerState: Int, playWhenReady: Boolean): Int =
        when (playerState) {
            Player.STATE_BUFFERING -> STATE_BUFFERING
            Player.STATE_READY -> if (playWhenReady) STATE_PLAYING else STATE_PAUSED
            Player.STATE_ENDED -> STATE_STOPPED
            else -> STATE_NONE
        }

    private fun buildPlaybackActions(): Long {
        var actions =
            (PlaybackController.ACTIONS and controller.getSupportedPlaybackActions(player))
        actions = actions or (PlaybackPreparer.ACTIONS and preparer.supportedPrepareActions)
        actions =
            actions or (QueueNavigator.ACTIONS and queueNavigator.getSupportedQueueNavigatorActions(
                player
            ))
        return actions
    }

    private fun updateSessionMetadata() {
        // Do nothing at the moment.
    }

    private fun canDispatchToPlaybackPreparer(@Actions action: Long): Boolean =
        preparer.supportedPrepareActions and PlaybackPreparer.ACTIONS and action != 0L

    private fun canDispatchToPlaybackController(@Actions action: Long): Boolean =
        controller.getSupportedPlaybackActions(player) and PlaybackController.ACTIONS and action != 0L

    private fun canDispatchToQueueNavigator(@Actions action: Long): Boolean =
        queueNavigator.getSupportedQueueNavigatorActions(player) and QueueNavigator.ACTIONS and action != 0L

    private inner class PlayerEventDispatcher : Player.EventListener {
        private var currentWindowIndex = 0
        private var currentWindowCount = 0

        override fun onTimelineChanged(
            timeline: Timeline?,
            manifest: Any?, @Player.TimelineChangeReason reason: Int
        ) {
            val windowCount = player.currentTimeline.windowCount
            val windowIndex = player.currentWindowIndex
            queueNavigator.onTimelineChanged(player)
            updatePlaybackState()

            currentWindowCount = windowCount
            currentWindowIndex = windowIndex
            updateSessionMetadata()
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            updatePlaybackState()
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            session.publishRepeatMode(
                when (repeatMode) {
                    Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                    Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                    else -> RepeatMode.OFF
                }
            )
            updatePlaybackState()
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            session.publishShuffleModeEnabled(shuffleModeEnabled)
            updatePlaybackState()
        }

        override fun onPositionDiscontinuity(@Player.DiscontinuityReason reason: Int) {
            if (currentWindowIndex != player.currentWindowIndex) {
                queueNavigator.onCurrentWindowIndexChanged(player)
                currentWindowIndex = player.currentWindowIndex
                updatePlaybackState()
                updateSessionMetadata()
            } else {
                updatePlaybackState()
            }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
            updatePlaybackState()
        }
    }
}