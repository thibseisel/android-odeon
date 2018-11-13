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

import android.os.Looper
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.audio.AudioListener
import com.google.android.exoplayer2.audio.AuxEffectInfo
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelectionArray

open class FakeAudioPlayer : BasePlayer(), ExoPlayer, Player.AudioComponent {

    /*
     * CONTROL OF PLAYER STATE.
     */

    protected var _playWhenReady = false
    protected var _playbackState: Int = Player.STATE_IDLE
    protected var _playbackParameters: PlaybackParameters = PlaybackParameters.DEFAULT

    protected var _repeatMode = Player.REPEAT_MODE_OFF
    protected var _shuffleModeEnabled = false

    override fun getPlayWhenReady(): Boolean = _playWhenReady
    override fun setPlayWhenReady(playWhenReady: Boolean) {
        _playWhenReady = playWhenReady
        notifyPlayerStateChanged(playWhenReady, _playbackState)
    }

    override fun getPlaybackState(): Int = _playbackState
    private fun setPlaybackState(playbackState: Int) {
        _playbackState = playbackState
        notifyPlayerStateChanged(_playWhenReady, playbackState)
    }

    override fun getPlaybackParameters(): PlaybackParameters = _playbackParameters
    override fun setPlaybackParameters(playbackParameters: PlaybackParameters?) {
        _playbackParameters = playbackParameters ?: PlaybackParameters.DEFAULT
    }

    override fun retry() {
        throw UnsupportedOperationException("Fake method not implemented")
    }

    override fun stop(reset: Boolean) {
        _playbackState = Player.STATE_IDLE
    }

    override fun release() {}

    override fun getShuffleModeEnabled(): Boolean = _shuffleModeEnabled
    override fun setShuffleModeEnabled(shuffleModeEnabled: Boolean) {
        _shuffleModeEnabled = shuffleModeEnabled
        listeners.forEach { it.onShuffleModeEnabledChanged(shuffleModeEnabled) }
    }

    override fun getRepeatMode(): Int = _repeatMode
    override fun setRepeatMode(repeatMode: Int) {
        _repeatMode = repeatMode
        listeners.forEach { it.onRepeatModeChanged(repeatMode) }
    }

    override fun getPlaybackError(): ExoPlaybackException? = null

    /*
     * CONTROL OF MEDIA TIMELINE
     */

    protected var _position: Long = C.TIME_UNSET

    override fun prepare(mediaSource: MediaSource?) = prepare(mediaSource, true, true)
    override fun prepare(mediaSource: MediaSource?, resetPosition: Boolean, resetState: Boolean) {
        // TODO Actual preparation of MediaSource
        setPlaybackState(Player.STATE_READY)
    }

    override fun getCurrentTimeline(): Timeline {
        throw UnsupportedOperationException("Fake method not implemented")
    }

    override fun getCurrentWindowIndex(): Int = C.INDEX_UNSET

    override fun getCurrentPeriodIndex(): Int = 0

    /*
     * CONTROL OF PLAYBACK POSITION
     */

    protected var _seekParameters: SeekParameters = SeekParameters.DEFAULT

    override fun getDuration(): Long = C.TIME_UNSET

    override fun getCurrentPosition(): Long = 0L

    override fun getBufferedPosition(): Long = 0L
    override fun getTotalBufferedDuration(): Long = 0L

    override fun seekTo(windowIndex: Int, positionMs: Long) {
        _position = positionMs
    }

    override fun getSeekParameters(): SeekParameters = _seekParameters
    override fun setSeekParameters(seekParameters: SeekParameters?) {
        _seekParameters = seekParameters ?: SeekParameters.DEFAULT
    }

    /*
     * PLAYER LISTENER
     */

    protected val listeners = mutableSetOf<Player.EventListener>()

    override fun addListener(listener: Player.EventListener?) {
        if (listener != null) {
            listeners += listener
        }
    }

    override fun removeListener(listener: Player.EventListener?) {
        if (listener != null) {
            listeners -= listener
        }
    }

    private fun notifyPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        listeners.forEach { it.onPlayerStateChanged(playWhenReady, playbackState) }
    }

    /*
     * AUDIO RENDERING
     */

    override fun getRendererCount(): Int = 0
    override fun getRendererType(index: Int): Int = C.TRACK_TYPE_AUDIO
    override fun getApplicationLooper(): Looper = Looper.getMainLooper()
    override fun getPlaybackLooper(): Looper = Looper.getMainLooper()

    /*
     * AUDIO-RELATED BEHAVIOR
     */

    protected var _audioAttributes = AudioAttributes.DEFAULT
    protected var _volume: Float = 1f

    override fun getAudioComponent(): Player.AudioComponent? = this

    override fun getVolume(): Float = _volume
    override fun setVolume(audioVolume: Float) {
        _volume = audioVolume
    }

    override fun getAudioAttributes(): AudioAttributes = _audioAttributes

    override fun getAudioSessionId(): Int = C.AUDIO_SESSION_ID_UNSET

    @Suppress("OverridingDeprecatedMember")
    override fun setAudioAttributes(audioAttributes: AudioAttributes?) {
        setAudioAttributes(audioAttributes, false)
    }

    override fun setAudioAttributes(audioAttributes: AudioAttributes?, handleAudioFocus: Boolean) {
        _audioAttributes = audioAttributes
    }

    override fun setAuxEffectInfo(auxEffectInfo: AuxEffectInfo?) {
        throw UnsupportedOperationException("Fake method not implemented")
    }

    override fun clearAuxEffectInfo() {
        throw UnsupportedOperationException("Fake method not implemented")
    }

    override fun addAudioListener(listener: AudioListener?) {
        throw UnsupportedOperationException("Fake method not implemented")
    }

    override fun removeAudioListener(listener: AudioListener?) {
        throw UnsupportedOperationException("Fake method not implemented")
    }

    /*
     * VIDEO-RELATED BEHAVIOR
     */

    /** Text output is not supported. Always returns `null`. */
    override fun getTextComponent(): Player.TextComponent? = null
    /** Video output is not supported. Always returns `null`. */
    override fun getVideoComponent(): Player.VideoComponent? = null

    /*
     * PLAYER MESSAGES
     */

    @Suppress("deprecation", "OverridingDeprecatedMember")
    override fun sendMessages(vararg messages: ExoPlayer.ExoPlayerMessage?) {
        throw UnsupportedOperationException("Fake method not implemented")
    }

    @Suppress("deprecation", "OverridingDeprecatedMember")
    override fun blockingSendMessages(vararg messages: ExoPlayer.ExoPlayerMessage?) {
        throw UnsupportedOperationException("Fake method not implemented")
    }

    override fun createMessage(target: PlayerMessage.Target?): PlayerMessage {
        throw UnsupportedOperationException("Fake method not implemented")
    }

    /*
     * ADS-RELATED BEHAVIOR
     */

    /** This fake implementation never play ads, and therefore always return `false`. */
    override fun isPlayingAd(): Boolean = false
    /** As this implementation never plays ads, returns the same result as [getCurrentPosition]. */
    override fun getContentPosition(): Long = currentPosition
    /** As this implementation never plays ads, return the same result as [getBufferedPosition] */
    override fun getContentBufferedPosition(): Long = bufferedPosition
    /** As this implementation never plays ads, always return [C.INDEX_UNSET]. */
    override fun getCurrentAdGroupIndex(): Int = C.INDEX_UNSET
    /** As this implementation never plays ads, always return [C.INDEX_UNSET]. */
    override fun getCurrentAdIndexInAdGroup(): Int = C.INDEX_UNSET

    /*
     * MISCELLANEOUS
     */

    override fun getCurrentTrackGroups(): TrackGroupArray {
        throw UnsupportedOperationException("Fake method not implemented")
    }

    override fun getCurrentManifest(): Any? = null

    override fun getCurrentTrackSelections(): TrackSelectionArray {
        throw UnsupportedOperationException("Fake method not implemented")
    }

    override fun isLoading(): Boolean = false

}