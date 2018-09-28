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

import android.annotation.TargetApi
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.annotation.VisibleForTesting
import android.support.v4.media.AudioAttributesCompat
import com.google.android.exoplayer2.ExoPlayer
import fr.nihilus.music.media.di.ServiceScoped
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named

private const val VOLUME_NORMAL = 1.0f
private const val VOLUME_DUCK = 0.2f

/**
 * An ExoPlayer implementation that takes care requesting audio focus whenever it needs to play.
 * If audio focus is not granted, the player will not start playback, and will lower its volume or
 * pause if another application needs the focus while possessing it.
 */
@ServiceScoped
internal class AudioFocusAwarePlayer
@Inject constructor(
    private val audioManager: AudioManager,
    @Named("WrappedPlayer") private val player: ExoPlayer
) : ExoPlayer by player {

    private val audioAttributes = AudioAttributesCompat.Builder()
        .setContentType(AudioAttributesCompat.CONTENT_TYPE_MUSIC)
        .setUsage(AudioAttributesCompat.USAGE_MEDIA)
        .build()

    @get:RequiresApi(Build.VERSION_CODES.O)
    private val audioFocusRequest: AudioFocusRequest by lazy { buildFocusRequest() }

    /**
     * Reflects the *intent* to play.
     * This is used to restore [setPlayWhenReady] after recovering from a transient focus loss.
     */
    private var shouldPlayWhenReady = false

    /**
     * Notify that the audio focus for this player has changed.
     * Visibility has been relaxed so that focus changes can be simulated during tests.
     */
    @VisibleForTesting
    @Suppress("MemberVisibilityCanBePrivate")
    internal val audioFocusListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (shouldPlayWhenReady || player.playWhenReady) {
                    player.playWhenReady = true
                    player.audioComponent!!.volume = VOLUME_NORMAL
                }
                shouldPlayWhenReady = false
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (player.playWhenReady) {
                    player.audioComponent!!.volume = VOLUME_DUCK
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Save the current state of playback so the _intention_ to play can be properly
                // restored to the app.
                shouldPlayWhenReady = player.playWhenReady
                player.playWhenReady = false
            }
            AudioManager.AUDIOFOCUS_LOSS -> {
                // This will chain through to abandonAudioFocus().
                AudioFocusAwarePlayer@playWhenReady = false
            }
        }
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) {
            requestAudioFocus()
        } else {
            if (shouldPlayWhenReady) {
                shouldPlayWhenReady = false
            }

            player.playWhenReady = false
            abandonAudioFocus()
        }
    }

    private fun requestAudioFocus() {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.requestAudioFocus(audioFocusRequest)
        } else {
            @Suppress("deprecation")
            audioManager.requestAudioFocus(
                audioFocusListener,
                audioAttributes.legacyStreamType,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        // Call the listener whenever focus is granted - event the first time!
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            shouldPlayWhenReady = true
            audioFocusListener.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        } else {
            Timber.i("Unable to start playback: audio focus request has been denied.")
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest)
        } else {
            @Suppress("deprecation")
            audioManager.abandonAudioFocus(audioFocusListener)
        }
    }

    override fun stop() {
        player.stop()
        abandonAudioFocus()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun buildFocusRequest(): AudioFocusRequest {
        return AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(audioAttributes.unwrap() as AudioAttributes)
            .setOnAudioFocusChangeListener(audioFocusListener)
            .build()
    }
}