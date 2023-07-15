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

package fr.nihilus.music.core.ui.client

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import fr.nihilus.music.core.lifecycle.ApplicationLifecycle
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.settings.Settings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maintain a client-side connection to this application's media session,
 * allowing to browser available media and send commands to the session transport controls.
 */
@Singleton
internal class BrowserClientImpl @Inject constructor(
    @ApplicationContext applicationContext: Context,
    @ApplicationLifecycle private val owner: LifecycleOwner,
    private val settings: Settings,
) : BrowserClient, DefaultLifecycleObserver {

    private val controllerCallback = ClientControllerCallback()
    private val connectionCallback = ConnectionCallback(applicationContext)

    @Volatile private var deferredController = CompletableDeferred<MediaControllerCompat>()

    init {
        owner.lifecycle.addObserver(this)
    }

    private val mediaBrowser = MediaBrowserCompat(
        applicationContext,
        ComponentName(applicationContext, "fr.nihilus.music.service.MusicService"),
        connectionCallback,
        null
    )

    private val _playbackState = MutableStateFlow<PlaybackStateCompat>(EMPTY_PLAYBACK_STATE)
    private val _nowPlaying = MutableStateFlow<MediaMetadataCompat?>(null)
    private val _shuffleMode = MutableStateFlow(PlaybackStateCompat.SHUFFLE_MODE_NONE)
    private val _repeatMode = MutableStateFlow(PlaybackStateCompat.REPEAT_MODE_NONE)

    override val playbackState: StateFlow<PlaybackStateCompat> = _playbackState
    override val nowPlaying: StateFlow<MediaMetadataCompat?> = _nowPlaying
    override val shuffleMode: StateFlow<Int> = _shuffleMode
    override val repeatMode: StateFlow<Int> = _repeatMode

    override fun onStart(owner: LifecycleOwner) {
        if (!mediaBrowser.isConnected) {
            Timber.tag("BrowserClientImpl").i("Connecting to service...")
            mediaBrowser.connect()
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        if (mediaBrowser.isConnected) {
            Timber.tag("BrowserClientImpl").i("Disconnecting from service...")
            mediaBrowser.disconnect()
            deferredController = CompletableDeferred()
        }
    }

    override suspend fun play() {
        val controller = deferredController.await()
        controller.transportControls.play()
    }

    override suspend fun pause() {
        val controller = deferredController.await()
        controller.transportControls.pause()
    }

    override suspend fun playFromMediaId(mediaId: MediaId) {
        val controller = deferredController.await()
        controller.transportControls.playFromMediaId(mediaId.encoded, null)
    }

    override suspend fun seekTo(positionMs: Long) {
        val controller = deferredController.await()
        controller.transportControls.seekTo(positionMs)
    }

    override suspend fun skipToPrevious() {
        val controller = deferredController.await()
        controller.transportControls.skipToPrevious()
    }

    override suspend fun skipToNext() {
        val controller = deferredController.await()
        controller.transportControls.skipToNext()
    }

    override suspend fun setShuffleModeEnabled(enabled: Boolean) {
        val controller = deferredController.await()
        controller.transportControls.setShuffleMode(
            when {
                enabled -> PlaybackStateCompat.SHUFFLE_MODE_ALL
                else -> PlaybackStateCompat.SHUFFLE_MODE_NONE
            }
        )
    }

    override suspend fun setRepeatMode(@PlaybackStateCompat.RepeatMode repeatMode: Int) {
        if (repeatMode == PlaybackStateCompat.REPEAT_MODE_NONE || repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE || repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL) {
            val controller = deferredController.await()
            controller.transportControls.setRepeatMode(repeatMode)
        }
    }

    private inner class ConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            Timber.tag("BrowserClientImpl").i("MediaBrowser is connected.")
            val controller = MediaControllerCompat(context, mediaBrowser.sessionToken).also {
                it.registerCallback(controllerCallback)
                _playbackState.value = it.playbackState ?: EMPTY_PLAYBACK_STATE
                _nowPlaying.value = it.metadata
                _repeatMode.value = it.repeatMode
                _shuffleMode.value = it.shuffleMode
            }

            // Trigger all operations waiting for the browser to be connected.
            deferredController.complete(controller)

            // Prepare last played playlist if nothing to play.
            if (controller.playbackState?.isPrepared != true && settings.prepareQueueOnStartup) {
                controller.transportControls.prepare()
            }
        }

        override fun onConnectionSuspended() {
            Timber.tag("BrowserClientImpl").i("Connection to the service has been suspended.")
            if (deferredController.isCompleted) {
                val controller = deferredController.getCompleted()
                controller.unregisterCallback(controllerCallback)
            } else {
                deferredController.cancel()
            }

            deferredController = CompletableDeferred()
        }

        override fun onConnectionFailed() {
            error("Failed to connect to the MediaBrowserService.")
        }
    }

    private inner class ClientControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            val newState = state ?: EMPTY_PLAYBACK_STATE

            when (newState.state) {
                PlaybackStateCompat.STATE_NONE,
                PlaybackStateCompat.STATE_STOPPED,
                PlaybackStateCompat.STATE_PAUSED,
                PlaybackStateCompat.STATE_PLAYING,
                PlaybackStateCompat.STATE_ERROR -> _playbackState.value = newState
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            _nowPlaying.value = metadata
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }

        override fun onShuffleModeChanged(shuffleMode: Int) {
            _shuffleMode.value = shuffleMode
        }

        override fun onSessionDestroyed() {
            Timber.tag("BrowserClientImpl").i("MediaSession has been destroyed.")
            connectionCallback.onConnectionSuspended()
        }
    }
}

/**
 * The playback state used as an alternative to `null`.
 */
private val EMPTY_PLAYBACK_STATE = PlaybackStateCompat.Builder()
    .setState(PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f, 0L)
    .build()

private val PlaybackStateCompat.isPrepared
    get() = (state == PlaybackStateCompat.STATE_BUFFERING) ||
            (state == PlaybackStateCompat.STATE_PLAYING) ||
            (state == PlaybackStateCompat.STATE_PAUSED)
