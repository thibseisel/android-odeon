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

import android.arch.lifecycle.DefaultLifecycleObserver
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
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
import fr.nihilus.music.di.ActivityScoped
import fr.nihilus.music.media.service.MusicService
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * The playback state used as an alternative to `null`.
 */
private val EMPTY_PLAYBACK_STATE = PlaybackStateCompat.Builder()
    .setState(PlaybackStateCompat.STATE_NONE, PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 0f, 0L)
    .build()

@ActivityScoped
class MediaBrowserConnection
@Inject constructor(
    context: Context,
    owner: LifecycleOwner
) : DefaultLifecycleObserver {

    /** Requests sent while the MediaBrowser was disconnected. */
    private val pendingRequests = LinkedList<MediaControllerRequest>()
    private val controllerCallback = ClientControllerCallback()
    private lateinit var controller: MediaControllerCompat

    private val mediaBrowser = MediaBrowserCompat(
        context.applicationContext,
        ComponentName(context, MusicService::class.java),
        ConnectionCallback(context),
        null
    )

    private val _playbackState = MutableLiveData<PlaybackStateCompat>()
    private val _nowPlaying = MutableLiveData<MediaMetadataCompat?>()
    private val _shuffleMode = MutableLiveData<@PlaybackStateCompat.ShuffleMode Int>()
    private val _repeatMode = MutableLiveData<@PlaybackStateCompat.RepeatMode Int>()

    val playbackState: MutableLiveData<PlaybackStateCompat> get() = _playbackState
    val nowPlaying: MutableLiveData<MediaMetadataCompat?> get() = _nowPlaying
    val shuffleMode: LiveData<Int> get() = _shuffleMode
    val repeatMode: LiveData<Int> get() = _repeatMode

    init {
        // Automatically connect/disconnect following the Activity's lifecycle.
        owner.lifecycle.addObserver(this)

        _playbackState.postValue(EMPTY_PLAYBACK_STATE)
        _shuffleMode.postValue(PlaybackStateCompat.SHUFFLE_MODE_INVALID)
        _repeatMode.postValue(PlaybackStateCompat.REPEAT_MODE_INVALID)
    }

    override fun onCreate(owner: LifecycleOwner) {
        mediaBrowser.connect()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        mediaBrowser.disconnect()
    }

    fun subscribe(mediaId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(mediaId, callback)
    }

    fun unsubscribe(mediaId: String) {
        mediaBrowser.unsubscribe(mediaId)
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
            // if connected, satisfy incoming requests immediately.
            request.invoke(controller)
            return
        }

        // Otherwise, enqueue the request until media browser is connected
        pendingRequests.offer(request)
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

    private inner class ConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() = try {
            controller = MediaControllerCompat(context, mediaBrowser.sessionToken).also {
                it.registerCallback(controllerCallback)
                _playbackState.postValue(it.playbackState ?: EMPTY_PLAYBACK_STATE)
                _nowPlaying.postValue(it.metadata)
                _repeatMode.value = it.repeatMode
                _shuffleMode.value = it.shuffleMode
            }

        } catch (re: RemoteException) {
            Timber.e(re, "Error while connecting through remote service binder.")
        }

        override fun onConnectionSuspended() {
            Timber.i("Connection to the MediaBrowserService has been suspended.")
            controller.unregisterCallback(controllerCallback)
        }

        override fun onConnectionFailed() {
            Timber.wtf("Failed to connect to the MediaBrowserService.")
        }
    }

    private inner class ClientControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            _playbackState.postValue(state ?: EMPTY_PLAYBACK_STATE)
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
            controller.unregisterCallback(controllerCallback)
        }
    }
}