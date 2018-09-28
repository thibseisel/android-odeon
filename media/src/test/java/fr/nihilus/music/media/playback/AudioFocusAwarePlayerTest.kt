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

import android.content.Context
import android.media.AudioManager
import android.os.Build
import com.google.android.exoplayer2.SimpleExoPlayer
import fr.nihilus.music.media.PropertyStub
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.BDDMockito.*
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnit
import org.mockito.junit.MockitoRule
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowAudioManager

@RunWith(RobolectricTestRunner::class)
class AudioFocusAwarePlayerTest {

    @[Rule JvmField] val mockitoRule: MockitoRule = MockitoJUnit.rule()

    @Mock private lateinit var wrappedPlayer: SimpleExoPlayer
    private lateinit var player: AudioFocusAwarePlayer

    private lateinit var audioManager: AudioManager

    /** Allow altering behavior of the platform's [AudioManager] without mocking it. */
    private lateinit var shadowAudioManager: ShadowAudioManager

    @Before
    fun setUp() {
        // Get an AudioManager from the test context, spy on it and retrieve its shadow.
        val context: Context = RuntimeEnvironment.application
        (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).let {
            audioManager = spy(it)
            shadowAudioManager = Shadows.shadowOf(it)
        }

        // Create an instance of the player under test.
        setupPlayWhenReadyStubProperty()
        player = AudioFocusAwarePlayer(audioManager, wrappedPlayer)
    }

    @Test
    fun beforeStartingPlayback_requestsAudioFocus() {
        // Given a player
        // When requested to start playback
        player.playWhenReady = true

        // Requests audio focus gain for music before playing.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            then(audioManager).should().requestAudioFocus(any())
        } else {
            @Suppress("deprecation")
            then(audioManager).should().requestAudioFocus(
                any(),
                eq(AudioManager.STREAM_MUSIC),
                eq(AudioManager.AUDIOFOCUS_GAIN)
            )
        }
    }

    @Test
    fun beforeStartingPlayback_andFocusFailed_shouldNotStartPlayback() {
        // Given an already acquired audio focus
        shadowAudioManager.setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_FAILED)

        // When requested to start playback
        player.playWhenReady = true

        // Focus is denied and the wrapped player is not requested to play
        then(wrappedPlayer).should(never()).playWhenReady = eq(true)
    }

    @Test
    fun whenFocusGranted_shouldStartPlayback() {
        // Given an available audio focus
        shadowAudioManager.setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_GRANTED)

        // When requested to start playback
        player.playWhenReady = true

        // Focus is granted and the wrapped player is requested to play
        then(wrappedPlayer).should().playWhenReady = eq(true)
    }

    @Test
    fun whenPausingWhileNotPlaying_andFocusNotGranted_pausesPlayback() {
        // Given an acquired audio focus
        shadowAudioManager.setNextFocusRequestResponse(AudioManager.AUDIOFOCUS_REQUEST_FAILED)

        // When pausing playback while not playing
        player.playWhenReady = false

        // Forward request to the wrapped player
        then(wrappedPlayer).should().playWhenReady = eq(false)
    }

    @Test
    fun whenPausingWhileFocused_shouldAbandonFocus() {
        // Given a player that has audio focus
        givenPlayerWithFocus()

        // When pausing playback
        player.playWhenReady = false

        // It should abandon audio focus.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            then(audioManager).should().abandonAudioFocusRequest(any())
        } else {
            @Suppress("deprecation")
            then(audioManager).should().abandonAudioFocus(any())
        }
    }

    @Test
    fun whenFocusChangedToTransientDuck_shouldKeepPlayingAtLowerVolume() {
        // Given a player that has audio focus
        givenPlayerWithFocus()

        // When audio focus is lost and duck is allowed
        player.audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

        // It should keep playing at a lower volume.
        then(wrappedPlayer).should(never()).playWhenReady = eq(false)
        then(wrappedPlayer).should().volume = floatThat { it < 1.0f }
    }

    @Test
    fun whenFocusGainedAfterDuck_shouldRestoreVolume() {
        // Given a player that has lost audio focus but was allowed to play at lower volume
        givenPlayerWithFocus(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK)

        // When audio focus is restored
        player.audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        // Volume should be restored to normal.
        then(wrappedPlayer).should().volume = eq(1.0f)
    }

    @Test
    fun whenFocusChangedToTransient_shouldPausePlayback() {
        // Given a player that has audio focus
        givenPlayerWithFocus()

        // When audio focus is lost temporarily (for example during a phone call)
        player.audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)

        // It should pause playback.
        then(wrappedPlayer).should().playWhenReady = eq(false)
    }

    @Test
    fun whenFocusGainedAfterTransient_shouldResumePlayback() {
        // Given a player that has lost audio focus temporarily
        givenPlayerWithFocus(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)

        // When audio focus is restored
        player.audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        // It should resume playback.
        then(wrappedPlayer).should().playWhenReady = eq(true)
    }

    @Test
    fun whenFocusIsLost_shouldPausePlaybackAndAbandonFocus() {
        // Given a player that has audio focus
        givenPlayerWithFocus()

        // When audio focus is lost permanently
        player.audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)

        // It should pause playback.
        then(wrappedPlayer).should().playWhenReady = eq(false)

        // It should abandon audio focus.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            then(audioManager).should().abandonAudioFocusRequest(any())
        } else {
            @Suppress("deprecation")
            then(audioManager).should().abandonAudioFocus(any())
        }
    }

    @Test
    fun whenStopPlayer_shouldAbandonAudioFocus() {
        // Given a player that has audio focus
        givenPlayerWithFocus()

        // When stopping player
        player.stop()

        // Player should abandon audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            then(audioManager).should().abandonAudioFocusRequest(any())
        } else {
            @Suppress("deprecation")
            then(audioManager).should().abandonAudioFocus(any())
        }
    }

    private fun givenPlayerWithFocus(audioFocus: Int = AudioManager.AUDIOFOCUS_GAIN) {
        // Start playback and acquire Audio Focus
        player.playWhenReady = true

        // Make the player lose its focus if requested
        when (audioFocus) {
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
            AudioManager.AUDIOFOCUS_LOSS -> {
                player.audioFocusListener.onAudioFocusChange(audioFocus)
            }
        }

        // Forget interactions with the wrapped player
        // so that initialization code does not change test results.
        Mockito.reset(wrappedPlayer)
        setupPlayWhenReadyStubProperty()
    }

    private fun setupPlayWhenReadyStubProperty() {
        val playWhenReady = PropertyStub(false)
        given(wrappedPlayer.playWhenReady).will(playWhenReady.getValue)
        will(playWhenReady.setValue).given(wrappedPlayer).playWhenReady = anyBoolean()
    }
}