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

package fr.nihilus.music.client

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.RemoteException
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import fr.nihilus.music.MediaControllerRequest
import fr.nihilus.music.R
import fr.nihilus.music.doIfPresent
import fr.nihilus.music.media.extensions.isPlaying
import fr.nihilus.music.media.service.MusicService
import fr.nihilus.music.utils.filter
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

/**
 * The playback state used as an alternative to `null`.
 */
private val EMPTY_PLAYBACK_STATE = PlaybackStateCompat.Builder()
    .setState(PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f, 0L)
    .build()

/**
 * A ViewModel that abstracts the connection to a MediaBrowserService.
 */
@Deprecated("This ViewModel, because it is generic and shared across all Activities and Fragments, has serious flaws. " +
        "It should be replaced by specific ViewModel implementations that references MediaBrowserConnection.")
class BrowserViewModel
@Inject constructor(context: Context) : ViewModel() {

    private val _playbackState = MutableLiveData<PlaybackStateCompat>()
        .apply { postValue(EMPTY_PLAYBACK_STATE) }
    val playbackState: LiveData<PlaybackStateCompat>
        get() = _playbackState

    private val _currentMetadata = MutableLiveData<MediaMetadataCompat>()
    val currentMetadata: LiveData<MediaMetadataCompat?>
        get() = _currentMetadata

    private val _repeatMode = MutableLiveData<@PlaybackStateCompat.RepeatMode Int>()
    val repeatMode: LiveData<Int>
        get() = _repeatMode

    private val _shuffleMode = MutableLiveData<@PlaybackStateCompat.ShuffleMode Int>()
    val shuffleMode: LiveData<Int>
        get() = _shuffleMode

    private val _playerError = MutableLiveData<CharSequence>()
    val playerError: LiveData<CharSequence>
        get() = _playerError.filter { it != null }

    private val mediaBrowser: MediaBrowserCompat
    private val mControllerCallback = ControllerCallback()
    private val subscriptionCache = HashMap<String, LiveData<List<MediaBrowserCompat.MediaItem>>>()

    private val requestQueue: Queue<MediaControllerRequest> = LinkedList()
    private lateinit var controller: MediaControllerCompat

    init {
        val componentName = ComponentName(context, MusicService::class.java)
        val connectionCallback = ConnectionCallback(context)
        mediaBrowser = MediaBrowserCompat(context, componentName, connectionCallback, null)
    }

    /**
     * Initiates a connection to the media browser service.
     */
    fun connect() {
        if (!mediaBrowser.isConnected) {
            Timber.d("Attempt to connect to MediaSession with MediaBrowser...")
            mediaBrowser.connect()
        }
    }

    /**
     * Post a request to execute instructions on a media controller.
     * If the media browser is not actually connected to its service, requests are delayed
     * then processed in order as soon as the media browser (re)connects.
     *
     * The provided controller instance is guaranteed to be connected to the service
     * at the time this method is called, but should not be cached as it may become
     * invalid at a later time.
     *
     * @param request The request to issue.
     */
    fun post(request: MediaControllerRequest) {
        if (mediaBrowser.isConnected) {
            // If connected, satisfy request immediately
            request.invoke(controller)
            return
        }

        // Otherwise, enqueue the request until media browser is connected
        requestQueue.offer(request)
    }

    /**
     * Post a command to be sent to the MediaBrowserService.
     * If the media browser is not actually connected to its service, requests are delayed
     * then processed in order as soon as the media browser (re)connects.
     *
     * If the specified command is not supported, the passed callback function will be called with
     * the result code [R.id.abc_error_unknown_command].
     *
     * @param commandName The name of the command to execute.
     * @param params The parameters to be passed to the command.
     * They may be required or optional depending on the command to execute.
     * @param onResultReceived The function to be called when the command has been processed.
     */
    inline fun postCommand(
        commandName: String,
        params: Bundle?,
        crossinline onResultReceived: (resultCode: Int, resultData: Bundle?) -> Unit
    ) = post { controller ->
        controller.sendCommand(commandName, params, object : ResultReceiver(Handler()) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                onResultReceived.invoke(resultCode, resultData)
            }
        })
    }

    /**
     * Subscribes to a stream of media items that are children of a given media id.
     */
    fun subscribeTo(parentId: String): LiveData<List<MediaBrowserCompat.MediaItem>> {
        return subscriptionCache.getOrPut(parentId) {
            SubscriptionLiveData(mediaBrowser, parentId)
        }
    }

    fun togglePlayPause() = post {
        val isPlaying = _playbackState.value?.isPlaying ?: false
        if (isPlaying) {
            it.transportControls.pause()
        } else {
            it.transportControls.play()
        }
    }

    fun toggleShuffleMode() = post {
        val currentShuffleMode = _shuffleMode.value ?: PlaybackStateCompat.SHUFFLE_MODE_NONE
        it.transportControls.setShuffleMode(
            if (currentShuffleMode == PlaybackStateCompat.SHUFFLE_MODE_ALL)
                PlaybackStateCompat.SHUFFLE_MODE_NONE
            else
                PlaybackStateCompat.SHUFFLE_MODE_ALL
        )
    }

    fun toggleRepeatMode() = post {
        val currentRepeatMode = _repeatMode.value ?: PlaybackStateCompat.REPEAT_MODE_NONE
        it.transportControls.setRepeatMode((currentRepeatMode + 1) % 3)
    }

    /**
     * @see MediaBrowserCompat.disconnect
     */
    private fun disconnect() {
        if (mediaBrowser.isConnected) {
            Timber.d("Disconnecting from the MediaSession")
            mediaBrowser.disconnect()
        }
    }

    override fun onCleared() {
        disconnect()
    }

    private inner class ConnectionCallback(
        context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {

        private val contextRef = WeakReference<Context>(context)

        override fun onConnected() = try {
            contextRef.doIfPresent {
                Timber.v("onConnected: browser is now connected to MediaBrowserService.")
                val controller = MediaControllerCompat(it, mediaBrowser.sessionToken)
                controller.registerCallback(mControllerCallback)
                this@BrowserViewModel.controller = controller
                _currentMetadata.value = controller.metadata
                _playbackState.value = controller.playbackState ?: EMPTY_PLAYBACK_STATE
                _shuffleMode.value = controller.shuffleMode
                _repeatMode.value = controller.repeatMode

                while (requestQueue.isNotEmpty()) {
                    val request = requestQueue.poll()
                    request.invoke(controller)
                }
            }
        } catch (re: RemoteException) {
            Timber.e(re, "onConnected: failed to create a MediaController")
        }

        override fun onConnectionSuspended() {
            Timber.i("onConnectionSuspended: disconnected from MediaBrowserService.")
            controller.unregisterCallback(mControllerCallback)
        }

        override fun onConnectionFailed() {
            Timber.e("onConnectionFailed: can't connect to MediaBrowserService.")
        }
    }

    private inner class ControllerCallback : MediaControllerCompat.Callback() {
        private val meaningfulStatuses = intArrayOf(
            PlaybackStateCompat.STATE_NONE,
            PlaybackStateCompat.STATE_STOPPED,
            PlaybackStateCompat.STATE_PAUSED,
            PlaybackStateCompat.STATE_PLAYING,
            PlaybackStateCompat.STATE_ERROR
        )

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            val newState = state ?: EMPTY_PLAYBACK_STATE
            // Only report for interesting statuses.
            // Transient statuses such as SKIPPING, BUFFERING are ignored for UI display
            if (newState.state in meaningfulStatuses) {
                _playbackState.postValue(state)
            }

            // Report player errors if any.
            _playerError.postValue(newState.errorMessage)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            _currentMetadata.postValue(metadata)
        }

        override fun onRepeatModeChanged(@PlaybackStateCompat.RepeatMode mode: Int) {
            _repeatMode.value = mode
        }

        override fun onShuffleModeChanged(@PlaybackStateCompat.ShuffleMode mode: Int) {
            _shuffleMode.value = mode
        }

        override fun onSessionDestroyed() {
            Timber.i("onSessionDestroyed")
        }
    }
}