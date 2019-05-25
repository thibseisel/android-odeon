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

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import fr.nihilus.music.media.actions.CustomActions
import fr.nihilus.music.media.extensions.isPrepared
import fr.nihilus.music.media.service.MusicService
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.sendBlocking
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * The playback state used as an alternative to `null`.
 */
private val EMPTY_PLAYBACK_STATE = PlaybackStateCompat.Builder()
    .setState(PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f, 0L)
    .build()

/**
 * Thrown when subscribing for children of a given media failed for some reason.
 *
 * @param parentId The parent media of the subscribed children.
 */
class MediaSubscriptionException(val parentId: String) : Exception() {
    override val message: String?
        get() = "Unable to load children of parent $parentId."
}

/**
 * Maintain a client-side connection to this application's media session,
 * allowing to browser available media and send commands to the session transport controls.
 *
 * Rather than initiate a new connection for each client,
 * this class will initiate a single connection when the first client connects,
 * sharing it across all clients.
 * The connection is disposed when the last client disconnects.
 */
@Singleton
class MediaBrowserConnection
@Inject constructor(applicationContext: Context) {

    private val connectedClients = mutableSetOf<ClientToken>()
    private val controllerCallback = ClientControllerCallback()
    private val connectionCallback = ConnectionCallback(applicationContext)

    @Volatile
    private var deferredController = CompletableDeferred<MediaControllerCompat>()

    private val mediaBrowser = MediaBrowserCompat(
        applicationContext,
        ComponentName(applicationContext, MusicService::class.java),
        connectionCallback,
        null
    )

    private val _playbackState = MutableLiveData<PlaybackStateCompat>()
    val playbackState: LiveData<PlaybackStateCompat>
        get() = _playbackState

    private val _nowPlaying = MutableLiveData<MediaMetadataCompat?>()
    val nowPlaying: LiveData<MediaMetadataCompat?>
        get() = _nowPlaying

    private val _shuffleMode = MutableLiveData<@PlaybackStateCompat.ShuffleMode Int>()
    val shuffleMode: LiveData<Int>
        get() = _shuffleMode

    private val _repeatMode = MutableLiveData<@PlaybackStateCompat.RepeatMode Int>()
    val repeatMode: LiveData<Int>
        get() = _repeatMode

    init {
        _playbackState.postValue(EMPTY_PLAYBACK_STATE)
        _shuffleMode.postValue(PlaybackStateCompat.SHUFFLE_MODE_INVALID)
        _repeatMode.postValue(PlaybackStateCompat.REPEAT_MODE_INVALID)
    }

    /**
     * Initiate connection to the media browser with the given [client]`.
     * Operations on the Media Session are only available once the media browser is connected.
     *
     * Make sure to [disconnect] the [client] from the media browser
     * when it is no longer needed to avoid wasting resources.
     *
     * @param client A token used to identify clients that connects to the media browser.
     */
    fun connect(client: ClientToken) {
        if (connectedClients.isEmpty()) {
            mediaBrowser.connect()
        }

        connectedClients += client
    }

    /**
     * Disconnect the given [client] from the media browser.
     *
     * @param client The same token used when connecting with [connect].
     */
    fun disconnect(client: ClientToken) {
        if (connectedClients.remove(client) && connectedClients.isEmpty()) {
            mediaBrowser.disconnect()
            deferredController = CompletableDeferred()
        }
    }

    suspend fun subscribe(parentId: String): ReceiveChannel<List<MediaBrowserCompat.MediaItem>> {
        // It seems that the (un)subscription does not work properly when MediaBrowser is disconnected.
        // Wait for the media browser to be connected before registering subscription.
        deferredController.await()

        return Channel<List<MediaBrowserCompat.MediaItem>>(capacity = Channel.CONFLATED).also {
            val callback = object : MediaBrowserCompat.SubscriptionCallback() {
                override fun onChildrenLoaded(
                    parentId: String,
                    children: List<MediaBrowserCompat.MediaItem>
                ) = it.sendBlocking(children)

                override fun onChildrenLoaded(
                    parentId: String,
                    children: List<MediaBrowserCompat.MediaItem>,
                    options: Bundle
                ) = onChildrenLoaded(parentId, children)

                override fun onError(parentId: String) {
                    Timber.e("Failed to load children of %s", parentId)
                    it.close(MediaSubscriptionException(parentId))
                }

                override fun onError(parentId: String, options: Bundle) = onError(parentId)
            }

            mediaBrowser.subscribe(parentId, callback)
            it.invokeOnClose {
                mediaBrowser.unsubscribe(parentId, callback)
            }
        }
    }

    /**
     * Retrieve information of a single item from the media browser.
     *
     * @param itemId The media id of the item to retrieve.
     * @return A media item with the same media id as the one requested,
     * or `null` if no such item exists or an error occurred.
     */
    suspend fun getItem(itemId: String): MediaBrowserCompat.MediaItem? {
        deferredController.await()

        return suspendCoroutine { continuation ->
            mediaBrowser.getItem(itemId, object : MediaBrowserCompat.ItemCallback() {
                override fun onItemLoaded(item: MediaBrowserCompat.MediaItem?) {
                    continuation.resume(item)
                }

                override fun onError(itemId: String) {
                    continuation.resume(null)
                }
            })
        }
    }

    suspend fun play() {
        val controller = deferredController.await()
        controller.transportControls.play()
    }

    suspend fun pause() {
        val controller = deferredController.await()
        controller.transportControls.pause()
    }

    suspend fun playFromMediaId(mediaId: String) {
        val controller = deferredController.await()
        controller.transportControls.playFromMediaId(mediaId, null)
    }

    suspend fun seekTo(positionMs: Long) {
        val controller = deferredController.await()
        controller.transportControls.seekTo(positionMs)
    }

    suspend fun skipToPrevious() {
        val controller = deferredController.await()
        controller.transportControls.skipToPrevious()
    }

    suspend fun skipToNext() {
        val controller = deferredController.await()
        controller.transportControls.skipToNext()
    }

    suspend fun setShuffleModeEnabled(enabled: Boolean) {
        val controller = deferredController.await()
        controller.transportControls.setShuffleMode(
            if (enabled)
                PlaybackStateCompat.SHUFFLE_MODE_ALL else
                PlaybackStateCompat.SHUFFLE_MODE_NONE
        )
    }

    suspend fun setRepeatMode(@PlaybackStateCompat.RepeatMode repeatMode: Int) {
        if (
            repeatMode == PlaybackStateCompat.REPEAT_MODE_NONE ||
            repeatMode == PlaybackStateCompat.REPEAT_MODE_ONE ||
            repeatMode == PlaybackStateCompat.REPEAT_MODE_ALL
        ) {
            val controller = deferredController.await()
            controller.transportControls.setRepeatMode(repeatMode)
        }
    }

    suspend fun sendCommand(name: String, params: Bundle?): CommandResult {
        val controller = deferredController.await()

        return suspendCoroutine {
            controller.sendCommand(name, params, object : ResultReceiver(Handler()) {
                override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                    it.resume(CommandResult(resultCode, resultData))
                }
            })
        }
    }

    suspend fun executeAction(name: String, params: Bundle?): Bundle? {
        // Wait until connected or the action will fail.
        deferredController.await()

        return suspendCoroutine {
            mediaBrowser.sendCustomAction(name, params, object : MediaBrowserCompat.CustomActionCallback() {
                override fun onResult(action: String?, extras: Bundle?, resultData: Bundle?) {
                    it.resume(resultData)
                }

                override fun onError(action: String?, extras: Bundle?, data: Bundle?) {
                    checkNotNull(action) { "Failing custom action should have a name" }
                    checkNotNull(data) { "Service should have sent a Bundle explaining the error" }

                    it.resumeWithException(
                        CustomActionException(
                            action,
                            data.getInt(CustomActions.EXTRA_ERROR_CODE, -1),
                            data.getString(CustomActions.EXTRA_ERROR_MESSAGE)
                        )
                    )
                }
            })
        }
    }

    private inner class ConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            Timber.i("MediaBrowser is connected.")
            val controller = MediaControllerCompat(context, mediaBrowser.sessionToken).also {
                it.registerCallback(controllerCallback)
                _playbackState.postValue(it.playbackState ?: EMPTY_PLAYBACK_STATE)
                _nowPlaying.postValue(it.metadata)
                _repeatMode.value = it.repeatMode
                _shuffleMode.value = it.shuffleMode
            }

            // Trigger all operations waiting for the browser to be connected.
            deferredController.complete(controller)

            // Prepare last played playlist if nothing to play.
            if (controller.playbackState?.isPrepared != true) {
                controller.transportControls.prepare()
            }
        }

        override fun onConnectionSuspended() {
            Timber.i("Connection to the MediaBrowserService has been suspended.")
            if (deferredController.isCompleted) {
                val controller = deferredController.getCompleted()
                controller.unregisterCallback(controllerCallback)
            } else {
                deferredController.cancel()
            }

            deferredController = CompletableDeferred()
        }

        override fun onConnectionFailed() {
            Timber.wtf("Failed to connect to the MediaBrowserService.")
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
                PlaybackStateCompat.STATE_ERROR -> _playbackState.postValue(newState)
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            _nowPlaying.postValue(metadata)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.value = repeatMode
        }

        override fun onShuffleModeChanged(shuffleMode: Int) {
            _shuffleMode.value = shuffleMode
        }

        override fun onSessionDestroyed() {
            Timber.i("MediaSession has been destroyed.")
            connectionCallback.onConnectionSuspended()
        }
    }

    /**
     * Defines data required to maintain a connection to a client-side MediaBrowser connection.
     */
    class ClientToken

    data class CommandResult(
        val resultCode: Int,
        val resultData: Bundle?
    )

    class CustomActionException(
        val actionName: String,
        val errorCode: Int,
        val errorMessage: String?
    ) : Exception("Custom action $actionName failed: $errorMessage")

}
