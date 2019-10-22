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

package fr.nihilus.music.core.ui.client

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import fr.nihilus.music.common.AppScope
import fr.nihilus.music.common.media.CustomActions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Maintain a client-side connection to this application's media session,
 * allowing to browser available media and send commands to the session transport controls.
 *
 * Rather than initiate a new connection for each client,
 * this class will initiate a single connection when the first client connects,
 * sharing it across all clients.
 * The connection is disposed when the last client disconnects.
 */
@AppScope
class MediaBrowserConnection
@Inject constructor(applicationContext: Context) {

    private val connectedClients = mutableSetOf<ClientToken>()
    private val controllerCallback = ClientControllerCallback()
    private val connectionCallback = ConnectionCallback(applicationContext)

    @Volatile private var deferredController = CompletableDeferred<MediaControllerCompat>()

    private val mediaBrowser = MediaBrowserCompat(
        applicationContext,
        ComponentName(applicationContext, "fr.nihilus.music.service.MusicService"),
        connectionCallback,
        null
    )

    private val _playbackState = ConflatedBroadcastChannel<PlaybackStateCompat>(EMPTY_PLAYBACK_STATE)
    val playbackState: Flow<PlaybackStateCompat> = _playbackState.asFlow()

    private val _nowPlaying = ConflatedBroadcastChannel<MediaMetadataCompat?>()
    val nowPlaying: Flow<MediaMetadataCompat?> = _nowPlaying.asFlow()

    private val _shuffleMode = ConflatedBroadcastChannel<@PlaybackStateCompat.ShuffleMode Int>()
    val shuffleMode: Flow<Int> = _shuffleMode.asFlow()

    private val _repeatMode = ConflatedBroadcastChannel<@PlaybackStateCompat.RepeatMode Int>()
    val repeatMode: Flow<Int> = _repeatMode.asFlow()

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

    fun subscribe(parentId: String): Flow<List<MediaItem>> = callbackFlow<List<MediaItem>> {
        // It seems that the (un)subscription does not work properly when MediaBrowser is disconnected.
        // Wait for the media browser to be connected before registering subscription.
        deferredController.await()

        val subscription = ChannelSubscription(channel)
        mediaBrowser.subscribe(parentId, subscription)
        awaitClose { mediaBrowser.unsubscribe(parentId, subscription) }
    }.conflate()

    /**
     * Retrieve information of a single item from the media browser.
     *
     * @param itemId The media id of the item to retrieve.
     * @return A media item with the same media id as the one requested,
     * or `null` if no such item exists or an error occurred.
     */
    suspend fun getItem(itemId: String): MediaItem? {
        deferredController.await()

        return suspendCoroutine { continuation ->
            mediaBrowser.getItem(itemId, object : MediaBrowserCompat.ItemCallback() {
                override fun onItemLoaded(item: MediaItem?) {
                    continuation.resume(item)
                }

                override fun onError(itemId: String) {
                    continuation.resume(null)
                }
            })
        }
    }

    /**
     * Search the given [terms][query] in the whole music library.
     *
     * @param query The searched terms.
     * @return A list of media that matches the query.
     */
    suspend fun search(query: String): List<MediaItem> {
        deferredController.await()

        return suspendCoroutine { continuation ->
            mediaBrowser.search(query, null, object : MediaBrowserCompat.SearchCallback() {

                override fun onSearchResult(query: String, extras: Bundle?, items: List<MediaItem>) {
                    continuation.resume(items)
                }

                override fun onError(query: String, extras: Bundle?) {
                    error("Unexpected failure when searching \"$query\".")
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
            when {
                enabled -> PlaybackStateCompat.SHUFFLE_MODE_ALL
                else -> PlaybackStateCompat.SHUFFLE_MODE_NONE
            }
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

                    val errorMessage = data.getString(CustomActions.EXTRA_ERROR_MESSAGE)
                    it.resumeWithException(CustomActionException(action, errorMessage))
                }
            })
        }
    }

    /**
     * A subscription that sends updates to media children to a [SendChannel].
     */
    private class ChannelSubscription(
        private val channel: SendChannel<List<MediaItem>>
    ) : MediaBrowserCompat.SubscriptionCallback() {

        override fun onChildrenLoaded(parentId: String, children: List<MediaItem>) {
            channel.offer(children)
        }

        override fun onChildrenLoaded(
            parentId: String,
            children: List<MediaItem>,
            options: Bundle
        ) = onChildrenLoaded(parentId, children)

        override fun onError(parentId: String) {
            channel.close(MediaSubscriptionException(parentId))
        }

        override fun onError(parentId: String, options: Bundle) = onError(parentId)
    }

    private inner class ConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            Timber.i("MediaBrowser is connected.")
            val controller = MediaControllerCompat(context, mediaBrowser.sessionToken).also {
                it.registerCallback(controllerCallback)
                _playbackState.offer(it.playbackState ?: EMPTY_PLAYBACK_STATE)
                _nowPlaying.offer(it.metadata)
                _repeatMode.offer(it.repeatMode)
                _shuffleMode.offer(it.shuffleMode)
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
                PlaybackStateCompat.STATE_ERROR -> _playbackState.offer(newState)
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            _nowPlaying.offer(metadata)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _repeatMode.offer(repeatMode)
        }

        override fun onShuffleModeChanged(shuffleMode: Int) {
            _shuffleMode.offer(shuffleMode)
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

    class CustomActionException(
        actionName: String,
        errorMessage: String?
    ) : Exception("Custom action $actionName failed: $errorMessage")
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

/**
 * Thrown when subscribing for children of a given media failed for some reason.
 *
 * @param parentId The parent media of the subscribed children.
 */
class MediaSubscriptionException(parentId: String) : Exception() {
    override val message: String? = "Unable to load children of parent $parentId."
}
