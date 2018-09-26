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
import android.support.test.runner.AndroidJUnit4
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.common.truth.Truth
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class AudioFocusAwarePlayerTest {

    @Mock lateinit var internalPlayer: SimpleExoPlayer
    @Mock lateinit var context: Context
    @Mock lateinit var audioManager: AudioManager

    var focusChangeListener: AudioManager.OnAudioFocusChangeListener? = null
    var noisyReceiverRegistered = false

    internal lateinit var player: AudioFocusAwarePlayer

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        initMockContext()
        player = AudioFocusAwarePlayer(audioManager, internalPlayer)
    }

    @Test
    fun whenFocusFailed_shouldNotStartPlayback() {
        // Audio focus request will fail
        initMockAudioFocus(granted = false)

        // Attempt to start playback
        player.playWhenReady = true

        // Wrapped player should neither start nor pause playback
        verify(internalPlayer, never()).playWhenReady = anyBoolean()
        Truth.assertWithMessage("Noisy receiver should not have been registered")
            .that(noisyReceiverRegistered).isFalse()
    }

    @Test
    fun whenFocusGranted_shouldStartPlayback() {
        // Audio focus will be granted
        initMockAudioFocus(granted = true)

        // Attempt to start playback
        player.setPlayWhenReady(true)

        // Wrapped player should start playback when ready
        verify(internalPlayer).playWhenReady = true
        Truth.assertThat(noisyReceiverRegistered).isTrue()
    }

    @Test
    fun whenPause_shouldPausePlayback() {
        // Start playback
        initMockAudioFocus()
        player.setPlayWhenReady(true)

        // Pause playback
        player.setPlayWhenReady(false)

        // Wrapped player should have paused playback
        verify(internalPlayer).setPlayWhenReady(false)
        Truth.assertThat(noisyReceiverRegistered).isFalse()
    }

    @Test
    fun whenStop_shouldStopPlayer() {
        initMockAudioFocus()
        player.stop()
        verify(internalPlayer).stop()
    }

    @Test
    fun whenStopWhilePlaying_shouldStop() {
        initMockAudioFocus()
        player.playWhenReady = true

        player.stop()

        verify(internalPlayer).stop()
        Truth.assertThat(noisyReceiverRegistered).isFalse()
    }

    @Test
    fun whenStartPlaying_shouldRequestAudioFocus() {
        initMockAudioFocus()
        player.playWhenReady = true

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            verify(audioManager).requestAudioFocus(any())
        } else {
            @Suppress("DEPRECATION")
            verify(audioManager).requestAudioFocus(any(), anyInt(), anyInt())
        }
    }

    private fun initMockContext() {
        `when`(context.registerReceiver(any(), any())).then {
            noisyReceiverRegistered = true
            return@then null
        }

        `when`(context.unregisterReceiver(any())).then {
            noisyReceiverRegistered = false
            return@then null
        }
    }

    @Suppress("DEPRECATION")
    private fun initMockAudioFocus(granted: Boolean = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            `when`(audioManager.requestAudioFocus(any())).then {
                //focusChangeListener = it.getArgument<AudioFocusRequest>(0)
                if (granted) AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                else AudioManager.AUDIOFOCUS_REQUEST_FAILED
            }

            `when`(audioManager.abandonAudioFocusRequest(any())).then {
                focusChangeListener = null
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } else {
            `when`(audioManager.requestAudioFocus(any(), anyInt(), anyInt())).then {
                focusChangeListener = it.getArgument(0)
                if (granted) AudioManager.AUDIOFOCUS_REQUEST_GRANTED
                else AudioManager.AUDIOFOCUS_REQUEST_FAILED
            }

            `when`(audioManager.abandonAudioFocus(any())).then {
                focusChangeListener = null
                AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        }
    }

    @After
    fun tearDown() {
        noisyReceiverRegistered = false
        focusChangeListener = null
    }
}