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

import android.Manifest
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
import fr.nihilus.music.common.os.PermissionDeniedException
import fr.nihilus.music.common.os.RuntimePermissions
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

interface MediaClient {
    val nowPlaying: Flow<MediaMetadataCompat?>
    val playbackState: Flow<PlaybackStateCompat>
    val repeatMode: Flow<RepeatMode>
    val shuffleModeEnabled: Flow<Boolean>

    fun connect()
    fun disconnect()

    fun getChildren(parentId: String): Flow<List<MediaItem>>
    suspend fun getItem(itemId: String): MediaItem
    suspend fun search(query: String): List<MediaItem>

    suspend fun play()
    suspend fun playFromMediaId(mediaId: String)
    suspend fun pause()
    suspend fun skipToNext()
    suspend fun skipToPrevious()
    suspend fun seekTo(positionMs: Long)

    suspend fun setShuffleMode(enabled: Boolean)
    suspend fun setRepeatMode(mode: RepeatMode)

    suspend fun executeAction(name: String, params: Bundle?): Bundle?
}

enum class RepeatMode {
    DISABLED, ONE, ALL
}

@AppScope
internal class MediaClientImpl
@Inject constructor(
    private val context: Context,
    private val permissions: RuntimePermissions
) : MediaClient {

    private val connectionCallback = ConnectionCallback()
    private val controllerCallback = ClientControllerCallback()

    private val browser = MediaBrowserCompat(
        context,
        ComponentName(context.applicationContext, "fr.nihilus.music.service.MusicService"),
        connectionCallback,
        null
    )

    private val _nowPlaying = ConflatedBroadcastChannel<MediaMetadataCompat?>()
    private val _playbackState = ConflatedBroadcastChannel<PlaybackStateCompat>(EMPTY_PLAYBACK_STATE)
    private val _repeatMode = ConflatedBroadcastChannel<RepeatMode>()
    private val _shuffleMode = ConflatedBroadcastChannel<Boolean>()

    @Volatile private var controller = CompletableDeferred<MediaControllerCompat>()

    override val nowPlaying: Flow<MediaMetadataCompat?>
        get() = _nowPlaying.asFlow()

    override val playbackState: Flow<PlaybackStateCompat>
        get() = _playbackState.asFlow()

    override val repeatMode: Flow<RepeatMode>
        get() = _repeatMode.asFlow()

    override val shuffleModeEnabled: Flow<Boolean>
        get() = _shuffleMode.asFlow()

    override fun connect() {
        if (!browser.isConnected) {
            browser.connect()
        }
    }

    override fun disconnect() {
        if (browser.isConnected) {
            browser.disconnect()

            controller.cancel()
            controller = CompletableDeferred()
        }
    }

    override fun getChildren(parentId: String): Flow<List<MediaItem>> = callbackFlow<List<MediaItem>> {
        val subscription = ChannelSubscription(channel, permissions)

        // It seems that the (un)subscription does not work properly when MediaBrowser is disconnected.
        // Wait for the media browser to be connected before registering subscription.
        controller.await()

        browser.subscribe(parentId, subscription)
        awaitClose { browser.unsubscribe(parentId, subscription) }
    }.conflate()

    override suspend fun getItem(itemId: String): MediaItem {
        // MediaBrowser needs to be connected in order to retrieve the item.
        controller.await()

        return suspendCoroutine { continuation ->
            browser.getItem(itemId, object : MediaBrowserCompat.ItemCallback() {

                override fun onItemLoaded(item: MediaItem) {
                    continuation.resume(item)
                }

                override fun onError(itemId: String) {
                    val itemLoadFailure = if (!permissions.canReadExternalStorage) {
                        // Retrieving an item can legitimately return null when permission are denied.
                        // Throw a specific exception to prompt to grant that permission.
                        PermissionDeniedException(Manifest.permission.READ_EXTERNAL_STORAGE)
                    } else {
                        // Returning null means that no item has been found, which is unexpected from this app.
                        NoSuchElementException("No item with the provided id: $itemId")
                    }

                    continuation.resumeWithException(itemLoadFailure)
                }
            })
        }
    }

    override suspend fun search(query: String): List<MediaItem> {
        // MediaBrowser needs to be connected in order to search items.
        controller.await()

        return suspendCoroutine { continuation ->
            browser.search(query, null, object : MediaBrowserCompat.SearchCallback() {

                override fun onSearchResult(query: String, extras: Bundle?, items: List<MediaItem>) {
                    continuation.resume(items)
                }

                override fun onError(query: String, extras: Bundle?) {
                    val searchFailure = if (!permissions.canReadExternalStorage) {
                        PermissionDeniedException(Manifest.permission.READ_EXTERNAL_STORAGE)
                    } else {
                        IllegalStateException("Unexpected failure when searching \"$query\".")
                    }

                    continuation.resumeWithException(searchFailure)
                }
            })
        }
    }

    override suspend fun play() {
        val controller = controller.await()
        controller.transportControls.play()
    }

    override suspend fun playFromMediaId(mediaId: String) {
        val controller = controller.await()
        controller.transportControls.playFromMediaId(mediaId, null)
    }

    override suspend fun pause() {
        val controller = controller.await()
        controller.transportControls.pause()
    }

    override suspend fun skipToNext() {
        val controller = controller.await()
        controller.transportControls.skipToNext()
    }

    override suspend fun skipToPrevious() {
        val controller = controller.await()
        controller.transportControls.skipToPrevious()
    }

    override suspend fun seekTo(positionMs: Long) {
        val controller = controller.await()
        controller.transportControls.seekTo(positionMs)
    }

    override suspend fun setShuffleMode(enabled: Boolean) {
        val controller = controller.await()
        controller.transportControls.setShuffleMode(when {
            enabled -> PlaybackStateCompat.SHUFFLE_MODE_ALL
            else -> PlaybackStateCompat.SHUFFLE_MODE_NONE
        })
    }

    override suspend fun setRepeatMode(mode: RepeatMode) {
        val controller = controller.await()
        controller.transportControls.setRepeatMode(when (mode) {
            RepeatMode.ALL -> PlaybackStateCompat.REPEAT_MODE_ALL
            RepeatMode.ONE -> PlaybackStateCompat.REPEAT_MODE_ONE
            else -> PlaybackStateCompat.REPEAT_MODE_NONE
        })
    }

    override suspend fun executeAction(name: String, params: Bundle?): Bundle? {
        // Custom actions can only be executed if the media browser is connected.
        controller.await()

        return suspendCoroutine { continuation ->
            browser.sendCustomAction(name, params, object : MediaBrowserCompat.CustomActionCallback() {

                override fun onResult(action: String?, extras: Bundle?, resultData: Bundle?) {
                    continuation.resume(resultData)
                }

                override fun onError(action: String?, extras: Bundle?, data: Bundle?) {
                    checkNotNull(action) { "Failing custom action should have a name" }
                    checkNotNull(data) { "Service should have sent a Bundle explaining the error" }

                    val errorMessage = data.getString(CustomActions.EXTRA_ERROR_MESSAGE)
                    continuation.resumeWithException(
                        MediaBrowserConnection.CustomActionException(action, errorMessage)
                    )
                }
            })
        }
    }

    private class ChannelSubscription(
        private val channel: SendChannel<List<MediaItem>>,
        private val permissions: RuntimePermissions
    ) : MediaBrowserCompat.SubscriptionCallback() {

        override fun onChildrenLoaded(parentId: String, children: List<MediaItem>) {
            channel.offer(children)
        }

        override fun onChildrenLoaded(
            parentId: String,
            children: List<MediaItem>,
            options: Bundle
        ) = onChildrenLoaded(parentId, children)

        override fun onError(parentId: String) = failForParent(parentId)

        override fun onError(parentId: String, options: Bundle) = failForParent(parentId)

        private fun failForParent(parentId: String) {
            val subscriptionFailure = if (!permissions.canReadExternalStorage) {
                // Loading children can legitimately fail when permission is denied.
                // Throw a specific exception to prompt user to grant that permission.
                PermissionDeniedException(Manifest.permission.READ_EXTERNAL_STORAGE)
            } else {
                // Returning null means that the parent item does not exists or is not browsable.
                IllegalArgumentException("Item \"$parentId\" does not exists or is not browsable.")
            }

            channel.close(subscriptionFailure)
        }
    }

    private inner class ConnectionCallback : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {
            Timber.i("MediaBrowser is connected.")

            val mediaController = MediaControllerCompat(context, browser.sessionToken).also {
                it.registerCallback(controllerCallback)
                _playbackState.offer(it.playbackState ?: EMPTY_PLAYBACK_STATE)
                _nowPlaying.offer(it.metadata)
                _repeatMode.offer(it.repeatMode.toRepeatModeEnum())
                _shuffleMode.offer(it.shuffleMode.isShuffleModeEnabled)
            }

            // Trigger all operations waiting for the browser to be connected.
            controller.complete(mediaController)

            // Prepare last played playlist if nothing to play.
            if (mediaController.playbackState?.isPrepared != true) {
                mediaController.transportControls.prepare()
            }
        }

        override fun onConnectionSuspended() {
            Timber.i("Connection to the MediaBrowserService has been suspended.")
            if (controller.isCompleted) {
                val controller = controller.getCompleted()
                controller.unregisterCallback(controllerCallback)
            } else {
                controller.cancel()
            }

            controller = CompletableDeferred()
        }

        override fun onConnectionFailed() {
            error("Unable to connect to MusicService via MediaBrowserCompat.")
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
            _repeatMode.offer(repeatMode.toRepeatModeEnum())
        }

        override fun onShuffleModeChanged(shuffleMode: Int) {
            _shuffleMode.offer(shuffleMode.isShuffleModeEnabled)
        }

        override fun onSessionDestroyed() {
            Timber.i("MediaSession has been destroyed.")
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

private val Int.isShuffleModeEnabled
    get() = this == PlaybackStateCompat.SHUFFLE_MODE_ALL || this == PlaybackStateCompat.SHUFFLE_MODE_GROUP

private fun Int.toRepeatModeEnum() = when (this) {
    PlaybackStateCompat.REPEAT_MODE_ALL,
    PlaybackStateCompat.REPEAT_MODE_GROUP -> RepeatMode.ALL
    PlaybackStateCompat.REPEAT_MODE_ONE -> RepeatMode.ONE
    else -> RepeatMode.DISABLED
}