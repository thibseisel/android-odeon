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

package fr.nihilus.music.library

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.os.RemoteException
import android.os.ResultReceiver
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import fr.nihilus.music.doIfPresent
import fr.nihilus.music.service.MusicService
import java.lang.ref.WeakReference
import javax.inject.Inject

private const val TAG = "BrowserViewModel"

/**
 * A ViewModel that abstracts the connection to a MediaBrowserService.
 */
class BrowserViewModel
@Inject constructor(context: Context) : ViewModel() {

    private val mBrowser: MediaBrowserCompat
    private var mController: MediaControllerCompat? = null
    private val mControllerCallback = ControllerCallback()

    val playbackState = MutableLiveData<PlaybackStateCompat>()
    val currentMetadata = MutableLiveData<MediaMetadataCompat>()
    val repeatMode = MutableLiveData<@PlaybackStateCompat.RepeatMode Int>()
    val shuffleMode = MutableLiveData<@PlaybackStateCompat.ShuffleMode Int>()

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
     * @see MediaControllerCompat.TransportControls.playFromMediaId
     */
    fun playFromMediaId(mediaId: String) {
        if (mBrowser.isConnected) {
            mController!!.transportControls.playFromMediaId(mediaId, null)
        }
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
     * @see MediaControllerCompat.dispatchMediaButtonEvent
     */
    fun dispatchMediaButtonEvent(keyEvent: KeyEvent) {
        mController?.run { dispatchMediaButtonEvent(keyEvent) }
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

    /**
     * @see MediaControllerCompat.TransportControls.setShuffleMode
     */
    fun setShuffleMode(@PlaybackStateCompat.ShuffleMode mode: Int) {
        mController?.transportControls?.setShuffleMode(mode)
    }

    /**
     * @see MediaControllerCompat.TransportControls.setRepeatMode
     */
    fun setRepeatMode(@PlaybackStateCompat.RepeatMode mode: Int) {
        mController?.transportControls?.setRepeatMode(mode)
    }

    /**
     * @see MediaControllerCompat.TransportControls.play
     */
    fun play() {
        mController?.transportControls?.play()
    }

    /**
     * @see MediaControllerCompat.TransportControls.pause
     */
    fun pause() {
        mController?.transportControls?.pause()
    }

    /**
     * @see MediaControllerCompat.TransportControls.seekTo
     */
    fun seekTo(position: Long) {
        mController?.transportControls?.seekTo(position)
    }

    /**
     * @see MediaControllerCompat.TransportControls.skipToPrevious
     */
    fun skipToPrevious() {
        mController?.transportControls?.skipToPrevious()
    }

    /**
     * @see MediaControllerCompat.TransportControls.skipToNext
     */
    fun skipToNext() {
        mController?.transportControls?.skipToNext()
    }

    /**
     * @see MediaControllerCompat.sendCommand
     */
    fun sendCommand(command: String, params: Bundle?, cb: ResultReceiver) {
        mController?.sendCommand(command, params, cb)
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

                    // Prepare playback. The service chooses what to prepare.
                    val state = controller.playbackState.state
                    if (state == PlaybackStateCompat.STATE_NONE ||
                            state == PlaybackStateCompat.STATE_STOPPED) {
                        controller.transportControls.prepare()
                    }
                }
            } catch (re: RemoteException) {
                Log.e(TAG, "onConnected: cannot create MediaController", re)
            }
        }

        override fun onConnectionSuspended() {
            Log.w(TAG, "onConnectionSuspended: disconnected from MediaBrowserService.")
            mController?.unregisterCallback(mControllerCallback)
            mController = null
        }

        override fun onConnectionFailed() {
            Log.wtf(TAG, "onConnectionFailed: cannot connect to MediaBrowserService.")
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

        override fun onShuffleModeChanged(mode: Int) {
            shuffleMode.value = mode
        }

        override fun onSessionDestroyed() {
            Log.w(TAG, "onSessionDestroyed")
        }
    }
}