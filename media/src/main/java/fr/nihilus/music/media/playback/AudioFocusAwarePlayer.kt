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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.audio.AudioAttributes
import fr.nihilus.music.media.di.ServiceScoped
import timber.log.Timber
import javax.inject.Inject

/**
 * The volume level to use when we lose audio focus,
 * but are allowed to reduce the volume instead of stopping playback.
 */
private const val VOLUME_DUCK = 0.2f
/** The volume level to use when we have audio focus. */
private const val VOLUME_NORMAL = 1.0f

/** We don't have audio focus and can't duck (play at a low volume). */
private const val AUDIO_NO_FOCUS_NO_DUCK = 0
/** We don't have focus, but we can duck (play at a low volume). */
private const val AUDIO_NO_FOCUS_CAN_DUCK = 1
/** We have full audio focus. We are allowed to play loudly. */
private const val AUDIO_FOCUSED = 2

/**
 * An ExoPlayer implementation that takes care requesting audio focus whenever it needs to play.
 * If audio focus is not granted, the player will not start playback, and will lower its volume or
 * pause if another application needs the focus while possessing it.
 *
 * Playback will also automatically stop if headphones are disconnected to avoid playing loudly
 * through the phone's speakers when not intended.
 */
@ServiceScoped
internal class AudioFocusAwarePlayer
@Inject constructor(
    private val context: Context,
    private val exoPlayer: SimpleExoPlayer
) : ExoPlayer by exoPlayer, AudioManager.OnAudioFocusChangeListener {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var currentFocus = AUDIO_NO_FOCUS_NO_DUCK
    private var focusRequest: AudioFocusRequest? = null
    private var playOnFocusGain = false

    private val audioNoisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
    private var noisyReceiverRegistered = false

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) onPlay()
        else onPause()
    }

    private fun onPlay() {
        val focusGranted = requestAudioFocus()
        if (focusGranted) {
            registerAudioNoisyReceiver()
            exoPlayer.playWhenReady = true
            configurePlayerState()
        }
    }

    private fun onPause() {
        exoPlayer.playWhenReady = false
        unregisterAudioNoisyReceiver()
    }

    override fun stop() {
        exoPlayer.stop()
        unregisterAudioNoisyReceiver()
        giveUpAudioFocus()
    }

    private fun requestAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(androidAttributesOf(exoPlayer.audioAttributes))
                .setOnAudioFocusChangeListener(this)
                .build()
            audioManager.requestAudioFocus(focusRequest)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                this, AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        currentFocus =
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) AUDIO_FOCUSED
                else AUDIO_NO_FOCUS_NO_DUCK

        currentFocus =
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) AUDIO_FOCUSED
                else AUDIO_NO_FOCUS_NO_DUCK

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    private fun giveUpAudioFocus(): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (focusRequest != null) {
                audioManager.abandonAudioFocusRequest(focusRequest).also { focusRequest = null }
            } else AudioManager.AUDIOFOCUS_REQUEST_FAILED
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(this)
        }

        return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun onAudioFocusChange(newFocus: Int) {
        currentFocus = when (newFocus) {
            AudioManager.AUDIOFOCUS_GAIN -> AUDIO_FOCUSED
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> AUDIO_NO_FOCUS_CAN_DUCK
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                playOnFocusGain = true
                AUDIO_NO_FOCUS_NO_DUCK
            }
            AudioManager.AUDIOFOCUS_LOSS -> AUDIO_NO_FOCUS_NO_DUCK
            else -> {
                Timber.w("Unhandled focus change: %d", newFocus)
                AUDIO_NO_FOCUS_NO_DUCK
            }
        }

        configurePlayerState()
    }

    private fun configurePlayerState() {
        if (currentFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // We don't have audio focus and can't duck, so we have to pause
            this.playWhenReady = false
        } else {
            // We're permitted to play, but only if we "duck" (play quietly)
            exoPlayer.volume =
                    if (currentFocus == AUDIO_NO_FOCUS_CAN_DUCK) VOLUME_DUCK
                    else VOLUME_NORMAL

            // If we were playing when we lost focus, we need to resume playing.
            if (playOnFocusGain) {
                exoPlayer.playWhenReady = true
                playOnFocusGain = false
            }
        }
    }

    private val mAudioNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                this@AudioFocusAwarePlayer.playWhenReady = false
            }
        }
    }

    private fun registerAudioNoisyReceiver() {
        if (!noisyReceiverRegistered) {
            context.registerReceiver(mAudioNoisyReceiver, audioNoisyIntentFilter)
            noisyReceiverRegistered = true
        }
    }

    private fun unregisterAudioNoisyReceiver() {
        if (noisyReceiverRegistered) {
            context.unregisterReceiver(mAudioNoisyReceiver)
            noisyReceiverRegistered = false
        }
    }

    override fun release() {
        // Make sure to release the broadcast receiver to avoid memory leaks
        stop()
        exoPlayer.release()
    }
}

/**
 * Convert ExoPlayer's audio attributes to the Android framework's equivalent representation.
 * @param attrs exoplayer audio attributes
 * @return android framework equivalent audio attributes
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
private fun androidAttributesOf(attrs: AudioAttributes): android.media.AudioAttributes {
    return android.media.AudioAttributes.Builder()
        .setUsage(attrs.usage)
        .setContentType(attrs.contentType)
        .setFlags(attrs.flags)
        .build()
}