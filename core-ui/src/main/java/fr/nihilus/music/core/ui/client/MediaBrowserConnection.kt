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
    private val _nowPlaying = ConflatedBroadcastChannel<MediaMetadataCompat?>()
    private val _shuffleMode = ConflatedBroadcastChannel<@PlaybackStateCompat.ShuffleMode Int>()
    private val _repeatMode = ConflatedBroadcastChannel<@PlaybackStateCompat.RepeatMode Int>()

    /** A flow whose latest value is the current playback state. */
    val playbackState: Flow<PlaybackStateCompat> = _playbackState.asFlow()
    /** A flow whose latest value is the currently playing track, or `null` if none. */
    val nowPlaying: Flow<MediaMetadataCompat?> = _nowPlaying.asFlow()
    /** A flow whose latest value is the current shuffle mode. */
    val shuffleMode: Flow<Int> = _shuffleMode.asFlow()
    /** A flow whose latest value is the current repeat mode. */
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

    /**
     * Retrieve children of a specified browsable item from the media browser tree,
     * observing changes to those children.
     * The latest value emitted by the returned flow is always the latest up-to-date children.
     * A new list is emitted whenever it changes.
     *
     * The flow will fail with a [MediaSubscriptionException] if the requested [parentId]
     * does not exists or is not a browsable item in the hierarchy.
     *
     * @param parentId The media id of a browsable item.
     * @return a flow of children of the specified browsable item.
     */
    fun getChildren(parentId: String): Flow<List<MediaItem>> = callbackFlow<List<MediaItem>> {
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

    /**
     * Requests the media service to start or resume playback.
     */
    suspend fun play() {
        val controller = deferredController.await()
        controller.transportControls.play()
    }

    /**
     * Requests the media service to pause playback.
     */
    suspend fun pause() {
        val controller = deferredController.await()
        controller.transportControls.pause()
    }

    /**
     * Requests the media service to play the item with the specified [mediaId].
     * @param mediaId The media id of a playable item.
     */
    suspend fun playFromMediaId(mediaId: String) {
        val controller = deferredController.await()
        controller.transportControls.playFromMediaId(mediaId, null)
    }

    /**
     * Requests the media service to move its playback position
     * to a given point in the currently playing media.
     *
     * @param positionMs The new position in the current media, in milliseconds.
     */
    suspend fun seekTo(positionMs: Long) {
        val controller = deferredController.await()
        controller.transportControls.seekTo(positionMs)
    }

    /**
     * Requests the media service to move to the previous item in the current playlist.
     */
    suspend fun skipToPrevious() {
        val controller = deferredController.await()
        controller.transportControls.skipToPrevious()
    }

    /**
     * Requests the media service to move to the next item in the current playlist.
     */
    suspend fun skipToNext() {
        val controller = deferredController.await()
        controller.transportControls.skipToNext()
    }

    /**
     * Enable/Disable shuffling of playlists.
     * @param enabled Whether shuffle should be enabled.
     */
    suspend fun setShuffleModeEnabled(enabled: Boolean) {
        val controller = deferredController.await()
        controller.transportControls.setShuffleMode(
            when {
                enabled -> PlaybackStateCompat.SHUFFLE_MODE_ALL
                else -> PlaybackStateCompat.SHUFFLE_MODE_NONE
            }
        )
    }

    /**
     * Sets the repeat mode.
     * @param repeatMode The new repeat mode.
     */
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

    /**
     * Requests the media service to execute a custom action.
     *
     * @param name The name of the action to execute.
     * This should be one of the `CustomActions.ACTION_*` constants.
     * @param params The parameters required for the execution of the custom action,
     * as specified in the documentation or the action name.
     *
     * @return The result of the execution of the action, if any.
     * @throws CustomActionException if the execution of the requested action failed.
     *
     * @see CustomActions
     */
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

    /**
     * Thrown when a custom action execution failed.
     * @param actionName The name of the executed custom action that failed.
     * @param errorMessage An optional error message describing the error.
     */
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
