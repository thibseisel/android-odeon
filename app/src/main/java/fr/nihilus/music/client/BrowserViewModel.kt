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
import fr.nihilus.music.media.service.MusicService
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

/**
 * A ViewModel that abstracts the connection to a MediaBrowserService.
 */
class BrowserViewModel
@Inject constructor(context: Context) : ViewModel() {

    val playbackState = MutableLiveData<PlaybackStateCompat>()
    val currentMetadata = MutableLiveData<MediaMetadataCompat>()
    val repeatMode = MutableLiveData<@PlaybackStateCompat.RepeatMode Int>()
    val shuffleMode = MutableLiveData<@PlaybackStateCompat.ShuffleMode Int>()

    private val mBrowser: MediaBrowserCompat
    private val mControllerCallback = ControllerCallback()
    private val subscriptionCache = HashMap<String, LiveData<List<MediaBrowserCompat.MediaItem>>>()

    private val requestQueue: Queue<MediaControllerRequest> = LinkedList()
    private lateinit var controller: MediaControllerCompat

    init {
        val componentName = ComponentName(context, MusicService::class.java)
        val connectionCallback = ConnectionCallback(context)
        mBrowser = MediaBrowserCompat(context, componentName, connectionCallback, null)
    }

    /**
     * Initiates a connection to the media browser service.
     */
    fun connect() {
        if (!mBrowser.isConnected) {
            Timber.d("Attempt to connect to MediaSession with MediaBrowser...")
            mBrowser.connect()
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
        if (mBrowser.isConnected) {
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
     * the result code [R.id.error_unknown_command].
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
            SubscriptionLiveData(mBrowser, parentId)
        }
    }

    /**
     * @see MediaBrowserCompat.disconnect
     */
    fun disconnect() {
        if (mBrowser.isConnected) {
            Timber.d("Disconnecting from the MediaSession")
            mBrowser.disconnect()
        }
    }

    override fun onCleared() {
        disconnect()
    }

    private inner class ConnectionCallback(
        context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {

        private val contextRef = WeakReference<Context>(context)

        override fun onConnected() {
            try {
                contextRef.doIfPresent {
                    Timber.v("onConnected: browser is now connected to MediaBrowserService.")
                    val controller = MediaControllerCompat(it, mBrowser.sessionToken)
                    controller.registerCallback(mControllerCallback)
                    this@BrowserViewModel.controller = controller
                    currentMetadata.value = controller.metadata
                    playbackState.value = controller.playbackState
                    shuffleMode.value = controller.shuffleMode
                    repeatMode.value = controller.repeatMode

                    while (requestQueue.isNotEmpty()) {
                        val request = requestQueue.poll()
                        request.invoke(controller)
                    }
                }
            } catch (re: RemoteException) {
                Timber.e(re, "onConnected: failed to create a MediaController")
            }
        }

        override fun onConnectionSuspended() {
            Timber.w("onConnectionSuspended: disconnected from MediaBrowserService.")
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

        override fun onPlaybackStateChanged(state: PlaybackStateCompat) {
            // Only report for interesting statuses.
            // Transient statuses such as SKIPPING, BUFFERING are ignored for UI display
            if (state.state in meaningfulStatuses) {
                playbackState.value = state
            }
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            currentMetadata.value = metadata
        }

        override fun onRepeatModeChanged(@PlaybackStateCompat.RepeatMode mode: Int) {
            repeatMode.value = mode
        }

        override fun onShuffleModeChanged(@PlaybackStateCompat.ShuffleMode mode: Int) {
            shuffleMode.value = mode
        }

        override fun onSessionDestroyed() {
            Timber.i("onSessionDestroyed")
        }
    }
}