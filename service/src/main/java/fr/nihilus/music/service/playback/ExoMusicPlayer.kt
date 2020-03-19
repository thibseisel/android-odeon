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

import android.content.Context
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import fr.nihilus.music.core.R
import fr.nihilus.music.core.playback.RepeatMode
import fr.nihilus.music.service.AudioTrack
import fr.nihilus.music.service.extensions.doOnPrepared
import kotlin.random.Random

internal class ExoMusicPlayer(
    context: Context,
    private val player: ExoPlayer
) : MusicPlayer {

    private val audioOnlyExtractors = AudioOnlyExtractorsFactory()
    private val appDataSourceFactory = DefaultDataSourceFactory(
        context,
        Util.getUserAgent(context, context.getString(R.string.core_app_name))
    )

    private val bufferWindow = Timeline.Window()

    override val state: MusicPlayer.State
        get() {
            if (player.playbackError != null) {
                return MusicPlayer.State.ERROR
            }

            return when (val playerState = player.playbackState) {
                Player.STATE_IDLE -> MusicPlayer.State.IDLE
                Player.STATE_BUFFERING,
                Player.STATE_ENDED -> MusicPlayer.State.PAUSED
                Player.STATE_READY -> if (player.playWhenReady)
                    MusicPlayer.State.PLAYING else
                    MusicPlayer.State.PAUSED
                else -> error("Unexpected player state: $playerState")
            }
        }

    override val currentPosition: Long
        get() = player.currentPosition

    override val bufferedPosition: Long
        get() = player.bufferedPosition

    override val duration: Long
        get() = player.duration

    override val playbackSpeed: Float
        get() = player.playbackParameters.speed

    override var playWhenReady: Boolean
        get() = player.playWhenReady
        set(shouldPlay) {
            player.playWhenReady = shouldPlay
        }

    override val playlist: List<AudioTrack>
        get() {
            val timeline = player.currentTimeline
            return List(timeline.windowCount) { windowIndex ->
                timeline.getWindow(windowIndex, bufferWindow, true)
                bufferWindow.tag as AudioTrack
            }
        }

    override val currentPlaylistIndex: Int
        get() = player.currentWindowIndex

    override fun prepare(queueId: Long, playlist: List<AudioTrack>, startAtIndex: Int) {
        if (playlist.isEmpty()) {
            return
        }

        val mediaSources = Array(playlist.size) {
            val playableItem = playlist[it]
            ExtractorMediaSource.Factory(appDataSourceFactory)
                .setExtractorsFactory(audioOnlyExtractors)
                .setTag(playableItem)
                .createMediaSource(playableItem.mediaUri)
        }

        val predictableShuffleOrder = if (startAtIndex in playlist.indices) {
            val shuffledIndices = createShuffledIndices(startAtIndex, playlist.size, queueId)
            ShuffleOrder.DefaultShuffleOrder(shuffledIndices, queueId)
        } else {
            ShuffleOrder.DefaultShuffleOrder(playlist.size, queueId)
        }

        // Prepare the new playing queue.
        // Because of an issue with ExoPlayer, shuffle order is reset when player is prepared.
        // As a workaround, wait for the player to be prepared before setting the shuffle order.
        val playlistSource = ConcatenatingMediaSource(false, predictableShuffleOrder, *mediaSources)
        player.prepare(playlistSource)
        player.doOnPrepared {
            playlistSource.setShuffleOrder(predictableShuffleOrder)
        }

        // Start playback at a given position if specified, otherwise at first shuffled index.
        val targetPlaybackPosition = when (startAtIndex) {
            in playlist.indices -> startAtIndex
            else -> predictableShuffleOrder.firstIndex
        }

        player.seekToDefaultPosition(targetPlaybackPosition)
    }

    override fun stop() {
        player.stop()
    }

    override fun seekTo(position: Long) {
        player.seekTo(position)
    }

    override fun setShuffleModeEnabled(enabled: Boolean) {
        player.shuffleModeEnabled = enabled
    }

    override fun setRepeatMode(mode: RepeatMode) {
        player.repeatMode = when (mode) {
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    override fun skipForward() {
        player.next()
    }

    override fun skipBackward() {
        player.previous()
    }

    override fun hasPrevious(): Boolean = player.hasPrevious()

    override fun hasNext(): Boolean = player.hasNext()

    override fun skipToPlaylistPosition(index: Int) {
        player.seekToDefaultPosition(index)
    }

    override fun registerEventListener(listener: MusicPlayer.EventListener) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun unregisterEventListener(listener: MusicPlayer.EventListener) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    /**
     * Create a sequence of consecutive natural numbers between `0` and `length - 1` in shuffled order,
     * starting by the given [firstIndex].
     *
     * @param firstIndex The first value in the produces array. Must be between `0` and [length] (exclusive).
     * @param length The length of the produced array. Must be greater or equal to `0`.
     * @param randomSeed The seed for shuffling numbers.
     *
     * @return An array containing all the natural numbers between `0` and `length - 1` in shuffled order.
     * Its first element is [firstIndex].
     */
    private fun createShuffledIndices(firstIndex: Int, length: Int, randomSeed: Long): IntArray {
        val shuffled = IntArray(length)

        if (length > 0) {
            val random = Random(randomSeed)
            shuffled[0] = firstIndex

            for (i in 1..firstIndex) {
                val swapIndex = random.nextInt(1, i + 1)
                shuffled[i] = shuffled[swapIndex]
                shuffled[swapIndex] = i - 1
            }

            for (i in (firstIndex + 1) until length) {
                val swapIndex = random.nextInt(1, i + 1)
                shuffled[i] = shuffled[swapIndex]
                shuffled[swapIndex] = i
            }
        }

        return shuffled
    }
}