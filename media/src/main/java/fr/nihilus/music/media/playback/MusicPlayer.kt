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

package fr.nihilus.music.media.playback

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import androidx.collection.ArrayMap
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.ShuffleOrder
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import fr.nihilus.music.media.R
import fr.nihilus.music.media.service.ExperimentalMediaApi

internal const val UNKNOWN_TRACK_INDEX: Int = C.INDEX_UNSET

@Suppress("unused")
@ExperimentalMediaApi
internal interface MusicPlayer {
    val state: State

    val currentTrackIndex: Int
    val currentPosition: Long
    val bufferedPosition: Long
    val contentDuration: Long
    val queueSize: Int

    val previousTrackIndex: Int
    val nextTrackIndex: Int

    var volume: Float
    var playWhenReady: Boolean
    var shuffleModeEnabled: Boolean
    var repeatMode: RepeatMode

    fun prepare(queueIdentifier: Long, queue: List<MediaSessionCompat.QueueItem>)

    fun hasPrevious(): Boolean
    fun hasNext(): Boolean
    fun previous()
    fun next()
    fun stop()
    fun seekTo(trackIndex: Int, positionMs: Long)
    fun addListener(listener: EventListener)
    fun removeListener(listener: EventListener)
    fun release()

    enum class State {
        // TODO Should we mirror ExoPlayer's state, or directly map to media session states?
    }

    enum class RepeatMode {
        OFF, ONE, ALL
    }

    interface EventListener {
        fun onPlayerStateChanged(newState: State) = Unit
        fun onRepeatModeChanged(repeatMode: RepeatMode) = Unit
        fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) = Unit
        fun onQueueChanged(reason: Int) = Unit
        fun onTrackChanged(trackIndex: Int, reason: Int) = Unit
        fun onTrackCompletion(completedTrackIndex: Int) = Unit
    }
}

@ExperimentalMediaApi
internal fun MusicPlayer.skipTo(trackIndex: Int) {
    seekTo(trackIndex, 0L)
}

@ExperimentalMediaApi
internal fun MusicPlayer.seekTo(positionMs: Long) {
    seekTo(currentTrackIndex, positionMs)
}

@Suppress("unused")
@ExperimentalMediaApi
internal class ExoMusicPlayer(
    context: Context,
    private val exoPlayer: ExoPlayer
) : MusicPlayer {

    private val listenerMappings: MutableMap<MusicPlayer.EventListener, ExoListenerAdapter> = ArrayMap()

    private val audioOnlyExtractors = AudioOnlyExtractorsFactory()
    private val appDataSourceFactory = DefaultDataSourceFactory(
        context,
        Util.getUserAgent(context, context.getString(R.string.app_name))
    )

    override val state: MusicPlayer.State
        get() = TODO("Available states are not yet defined")

    override val currentTrackIndex: Int
        get() = exoPlayer.currentWindowIndex

    override val currentPosition: Long
        get() = exoPlayer.contentPosition

    override val bufferedPosition: Long
        get() = exoPlayer.bufferedPosition

    override val contentDuration: Long
        get() = exoPlayer.contentDuration

    override val queueSize: Int
        get() = exoPlayer.currentTimeline.windowCount

    override val previousTrackIndex: Int
        get() = exoPlayer.previousWindowIndex

    override val nextTrackIndex: Int
        get() = exoPlayer.nextWindowIndex

    override var playWhenReady: Boolean
        get() = exoPlayer.playWhenReady
        set(value) { exoPlayer.playWhenReady = value }

    override var volume: Float
        get() = exoPlayer.audioComponent?.volume ?: 0f
        set(value) { exoPlayer.audioComponent?.volume = value.coerceIn(0f, 1f) }

    override var shuffleModeEnabled: Boolean
        get() = exoPlayer.shuffleModeEnabled
        set(value) { exoPlayer.shuffleModeEnabled = value }

    override var repeatMode: MusicPlayer.RepeatMode
        get() = exoPlayer.repeatMode.toEnum()
        set(value) { exoPlayer.repeatMode = value.toExoRepeatMode() }

    override fun prepare(
        queueIdentifier: Long,
        queue: List<MediaSessionCompat.QueueItem>
    ) {
        val mediaSources = Array(queue.size) { trackIndex ->
            val itemDescription = queue[trackIndex].description
            val mediaFileUri = checkNotNull(itemDescription.mediaUri) {
                "Every queue item should have a media uri."
            }

            ExtractorMediaSource.Factory(appDataSourceFactory)
                .setExtractorsFactory(audioOnlyExtractors)
                .createMediaSource(mediaFileUri)
        }

        // Defines a shuffle order for the loaded media sources that is predictable.
        // Its ordering is derived from the specified queue identifier.
        val predictableShuffleOrder = ShuffleOrder.DefaultShuffleOrder(
            mediaSources.size,
            queueIdentifier
        )

        // Concatenate all media source to play them all in the same Timeline.
        val concatenatedSource = ConcatenatingMediaSource(
            false,
            predictableShuffleOrder,
            *mediaSources
        )

        exoPlayer.prepare(concatenatedSource)
    }

    override fun hasPrevious(): Boolean = exoPlayer.hasPrevious()

    override fun hasNext(): Boolean = exoPlayer.hasNext()

    override fun previous() = exoPlayer.previous()

    override fun next() = exoPlayer.next()

    override fun stop() = exoPlayer.stop()

    override fun seekTo(trackIndex: Int, positionMs: Long) {
        exoPlayer.seekTo(trackIndex, positionMs)
    }

    override fun addListener(listener: MusicPlayer.EventListener) {
        if (!listenerMappings.containsKey(listener)) {
            val exoListener = ExoListenerAdapter(exoPlayer, listener)
            exoPlayer.addListener(exoListener)
            listenerMappings[listener] = exoListener
        }
    }

    override fun removeListener(listener: MusicPlayer.EventListener) {
        val exoListener = listenerMappings.remove(listener)
        if (exoListener != null) {
            exoPlayer.removeListener(exoListener)
        }
    }

    override fun release() = exoPlayer.release()

    private class ExoListenerAdapter(
        private val player: Player,
        private val listener: MusicPlayer.EventListener
    ) : Player.EventListener {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            TODO("Dispatch to onPlayerStateChanged")
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            listener.onRepeatModeChanged(repeatMode.toEnum())
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            listener.onShuffleModeEnabledChanged(shuffleModeEnabled)
        }

        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
            // TODO To what should we dispatch ?
        }

        override fun onPositionDiscontinuity(reason: Int) {
            // TODO Maybe dispatch other events ?

            if (reason == Player.DISCONTINUITY_REASON_PERIOD_TRANSITION) {
                val previousTrackIndex = player.previousWindowIndex
                if (previousTrackIndex != C.INDEX_UNSET) {
                    listener.onTrackCompletion(previousTrackIndex)
                }
            }
        }
    }

}

@ExperimentalMediaApi
private fun Int.toEnum(): MusicPlayer.RepeatMode = when(this) {
    Player.REPEAT_MODE_ALL -> MusicPlayer.RepeatMode.ALL
    Player.REPEAT_MODE_ONE -> MusicPlayer.RepeatMode.ONE
    else -> MusicPlayer.RepeatMode.OFF
}

@ExperimentalMediaApi
private fun MusicPlayer.RepeatMode.toExoRepeatMode() = when(this) {
    MusicPlayer.RepeatMode.OFF -> Player.REPEAT_MODE_OFF
    MusicPlayer.RepeatMode.ONE -> Player.REPEAT_MODE_ONE
    MusicPlayer.RepeatMode.ALL -> Player.REPEAT_MODE_ALL
}

