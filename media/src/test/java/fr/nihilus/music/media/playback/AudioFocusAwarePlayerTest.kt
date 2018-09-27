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

import android.media.AudioManager
import android.os.Build
import com.google.android.exoplayer2.SimpleExoPlayer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioFocusAwarePlayerTest {

    @[Rule JvmField] val mockitoRule: MockitoRule = MockitoJUnit.rule().silent()

    @Mock private lateinit var wrappedPlayer: SimpleExoPlayer
    @Mock private lateinit var deniedFocusAudioManager: AudioManager
    @Mock private lateinit var grantedFocusAudioManager: AudioManager

    @Before
    fun setUpMocks() {
        // Mock the behavior or an AudioManager that always deny granting audio focus.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            given(deniedFocusAudioManager.requestAudioFocus(any()))
                .willReturn(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        } else {
            @Suppress("deprecation")
            given(deniedFocusAudioManager.requestAudioFocus(any(), anyInt(), anyInt()))
                .willReturn(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        }

        // Mock the behavior or an AudioManager that always grants audio focus.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            given(grantedFocusAudioManager.requestAudioFocus(any()))
                .willReturn(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        } else {
            @Suppress("deprecation")
            given(grantedFocusAudioManager.requestAudioFocus(any(), anyInt(), anyInt()))
                .willReturn(AudioManager.AUDIOFOCUS_REQUEST_FAILED)
        }
    }

    @Test
    fun beforeStartingPlayback_requestsAudioFocus() {
        // Given a new player
        val player = AudioFocusAwarePlayer(grantedFocusAudioManager, wrappedPlayer)

        // When requested to start playback
        player.playWhenReady = true

        // Requests audio focus gain for music before playing.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            then(grantedFocusAudioManager).should().requestAudioFocus(any())
        } else {
            @Suppress("deprecation")
            then(grantedFocusAudioManager).should().requestAudioFocus(
                any(),
                eq(AudioManager.STREAM_MUSIC),
                eq(AudioManager.AUDIOFOCUS_GAIN)
            )
        }
    }

    @Test
    fun beforeStartingPlayback_andFocusFailed_shouldNotStartPlayback() {
        // Given a new player
        val player = AudioFocusAwarePlayer(deniedFocusAudioManager, wrappedPlayer)

        // When requested to start playback
        player.playWhenReady = true

        // Focus is denied and the wrapped player is not requested to play
        then(wrappedPlayer).should(never()).playWhenReady = eq(true)
    }

    @Test
    fun whenFocusGranted_shouldStartPlayback() {
        // Given a new player
        val player = AudioFocusAwarePlayer(grantedFocusAudioManager, wrappedPlayer)

        // When requested to start playback
        player.playWhenReady = true

        // Focus is granted and the wrapped player is requested to play
        then(wrappedPlayer).should().playWhenReady = eq(true)
    }

    @Test
    fun whenPausingWhileNotPlaying_andFocusNotGranted_pausesPlayback() {
        // Given a new player
        val player = AudioFocusAwarePlayer(grantedFocusAudioManager, wrappedPlayer)

        // When pausing playback while not playing
        player.playWhenReady = false

        // Forward request to the wrapped player
        then(wrappedPlayer).should().playWhenReady = eq(false)
    }

    @Test
    fun whenPausingWhileFocused_shouldAbandonFocus() {
        // Given a player that has audio focus
        val player = AudioFocusAwarePlayer(grantedFocusAudioManager, wrappedPlayer)
        player.playWhenReady = true

        // When pausing playback
        player.playWhenReady = false

        // It should abandon audio focus.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            then(grantedFocusAudioManager).should().abandonAudioFocusRequest(any())
        } else {
            @Suppress("deprecation")
            then(grantedFocusAudioManager).should().abandonAudioFocus(any())
        }
    }

    @Test
    fun whenFocusChangedToTransientDuck_shouldKeepPlayingAtLowerVolume() {
        // Given a player that has audio focus
        val player = AudioFocusAwarePlayer(grantedFocusAudioManager, wrappedPlayer)
        player.audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        // When audio focus is lost and duck is allowed
        player.audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

        // It should keep playing at a lower volume.
        then(wrappedPlayer).should(never()).playWhenReady = eq(false)
        then(wrappedPlayer).should().volume = floatThat { it < 1.0f }
    }

    @Test
    fun whenFocusGainedAfterDuck_shouldRestoreVolume() {
        // Given a player that has lost audio focus but was allowed to play at lower volume
        val player = AudioFocusAwarePlayer(grantedFocusAudioManager, wrappedPlayer)
        player.audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

        // When audio focus is restored
        player.audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        // Volume should be restored to normal.
        then(wrappedPlayer).should().volume = eq(1.0f)
    }

    @Test
    fun whenFocusChangedToTransient_shouldPausePlayback() {
        // Given a player that has audio focus
        val player = AudioFocusAwarePlayer(grantedFocusAudioManager, wrappedPlayer)
        player.audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        // When audio focus is lost temporarily (for example during a phone call)
        player.audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)

        // It should pause playback.
        then(wrappedPlayer).should().playWhenReady = eq(false)
    }

    @Test
    fun whenFocusGainedAfterTransient_shouldResumePlayback() {
        // Given a player that has lost audio focus temporarily
        val player = AudioFocusAwarePlayer(grantedFocusAudioManager, wrappedPlayer)
        player.audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)

        // When audio focus is restored
        player.audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        // It should resume playback.
        then(wrappedPlayer).should().playWhenReady = eq(true)
    }

    @Test
    fun whenFocusIsLost_shouldPausePlaybackAndAbandonFocus() {
        // Given a player that has audio focus
        val player = AudioFocusAwarePlayer(grantedFocusAudioManager, wrappedPlayer)
        player.audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        // When audio focus is lost permanently
        player.audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)

        // It should pause playback.
        then(wrappedPlayer).should().playWhenReady = eq(false)

        // It should abandon audio focus.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            then(grantedFocusAudioManager).should().abandonAudioFocusRequest(any())
        } else {
            @Suppress("deprecation")
            then(grantedFocusAudioManager).should().abandonAudioFocus(any())
        }
    }

    @Test
    fun whenStopPlayer_shouldAbandonAudioFocus() {
        // Given a player that has audio focus
        val player = AudioFocusAwarePlayer(grantedFocusAudioManager, wrappedPlayer)
        player.playWhenReady = true

        // When stopping player
        player.stop()

        // Player should abandon audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            then(grantedFocusAudioManager).should().abandonAudioFocusRequest(any())
        } else {
            @Suppress("deprecation")
            then(grantedFocusAudioManager).should().abandonAudioFocus(any())
        }
    }
}