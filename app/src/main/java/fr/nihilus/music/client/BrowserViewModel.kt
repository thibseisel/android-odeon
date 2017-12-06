/*
 * Copyright 2017 Thibault Seisel
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

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.ComponentName
import android.content.Context
import android.os.RemoteException
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import fr.nihilus.music.MediaControllerRequest
import fr.nihilus.music.doIfPresent
import fr.nihilus.music.service.MusicService
import java.lang.ref.WeakReference
import java.util.*
import javax.inject.Inject

private const val TAG = "BrowserViewModel"

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

    private val requestQueue: Queue<MediaControllerRequest> = LinkedList()
    private lateinit var mController: MediaControllerCompat

    init {
        val componentName = ComponentName(context, MusicService::class.java)
        val connectionCallback = ConnectionCallback(context)
        mBrowser = MediaBrowserCompat(context, componentName, connectionCallback, null)
    }

    fun connect() {
        if (!mBrowser.isConnected) {
            Log.d(TAG, "Connecting...")
            mBrowser.connect()
        }
    }

    /**
     * Post a request to execute instructions on a media controller.
     * If the media browser is not actually connected to its service, requests are delayed
     * then processed in order whenever the media browser (re)connects.
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
            request.invoke(mController)
            return
        }

        // Otherwise, enqueue the request until media browser is connected
        requestQueue.offer(request)
    }

    /**
     * @see MediaBrowserCompat.disconnect
     */
    fun disconnect() {
        if (mBrowser.isConnected) {
            Log.d(TAG, "Disconnecting from service")
            mBrowser.disconnect()
        }
    }

    /**
     * @see MediaBrowserCompat.subscribe
     */
    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mBrowser.subscribe(parentId, callback)
    }

    /**
     * @see MediaBrowserCompat.unsubscribe
     */
    fun unsubscribe(parentId: String) {
        mBrowser.unsubscribe(parentId)
    }

    override fun onCleared() {
        disconnect()
    }

    private inner class ConnectionCallback(context: Context) : MediaBrowserCompat.ConnectionCallback() {
        private val contextRef = WeakReference<Context>(context)

        override fun onConnected() {
            try {
                contextRef.doIfPresent {
                    Log.v(TAG, "onConnected: browser is now connected to MediaBrowserService.")
                    val controller = MediaControllerCompat(it, mBrowser.sessionToken)
                    controller.registerCallback(mControllerCallback)
                    mController = controller
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
                Log.e(TAG, "onConnected: cannot create MediaController", re)
            }
        }

        override fun onConnectionSuspended() {
            Log.w(TAG, "onConnectionSuspended: disconnected from MediaBrowserService.")
            mController.unregisterCallback(mControllerCallback)
        }

        override fun onConnectionFailed() {
            Log.e(TAG, "onConnectionFailed: can't connect to MediaBrowserService.")
        }
    }

    private inner class ControllerCallback : MediaControllerCompat.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            playbackState.value = state
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
            Log.w(TAG, "onSessionDestroyed")
        }
    }
}