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
import fr.nihilus.music.service.MusicService
import javax.inject.Inject

private const val TAG = "BrowserViewModel"

class BrowserViewModel
@Inject constructor(private val context: Context) : ViewModel() {

    private val mBrowser: MediaBrowserCompat
    private var mController: MediaControllerCompat? = null
    private val mControllerCallback = ControllerCallback()

    val playbackState = MutableLiveData<PlaybackStateCompat>()
    val currentMetadata = MutableLiveData<MediaMetadataCompat>()
    val repeatMode = MutableLiveData<@PlaybackStateCompat.RepeatMode Int>()
    val shuffleMode = MutableLiveData<@PlaybackStateCompat.ShuffleMode Int>()

    init {
        val componentName = ComponentName(context, MusicService::class.java)
        mBrowser = MediaBrowserCompat(context, componentName, ConnectionCallback(), null)
    }

    fun connect() {
        if (!mBrowser.isConnected) {
            Log.d(TAG, "Connecting...")
            mBrowser.connect()
        }
    }

    fun playFromMediaId(mediaId: String) {
        if (mBrowser.isConnected) {
            mController!!.transportControls.playFromMediaId(mediaId, null)
        }
    }

    fun disconnect() {
        if (mBrowser.isConnected) {
            Log.d(TAG, "Disconnecting from service")
            mBrowser.disconnect()
        }
    }

    fun dispatchMediaButtonEvent(keyEvent: KeyEvent) {
        if (mController != null) {
            mController!!.dispatchMediaButtonEvent(keyEvent)
        }
    }

    /**
     *
     */
    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mBrowser.subscribe(parentId, callback)
    }

    /**
     *
     */
    fun unsubscribe(parentId: String) {
        mBrowser.unsubscribe(parentId)
    }

    fun setShuffleMode(@PlaybackStateCompat.ShuffleMode mode: Int) {
        mController?.transportControls?.setShuffleMode(mode)
    }

    fun setRepeatMode(@PlaybackStateCompat.RepeatMode mode: Int) {
        mController?.transportControls?.setRepeatMode(mode)
    }

    fun play() {
        mController?.transportControls?.play()
    }

    fun pause() {
        mController?.transportControls?.pause()
    }

    fun seekTo(position: Long) {
        mController?.transportControls?.seekTo(position)
    }

    fun skipToPrevious() {
        mController?.transportControls?.skipToPrevious()
    }

    fun skipToNext() {
        mController?.transportControls?.skipToNext()
    }

    fun sendCommand(command: String, params: Bundle?, cb: ResultReceiver) {
        mController?.sendCommand(command, params, cb)
    }

    override fun onCleared() {
        disconnect()
    }

    private inner class ConnectionCallback : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            try {
                Log.v(TAG, "onConnected: browser is now connected to MediaBrowserService.")
                val controller = MediaControllerCompat(context, mBrowser.sessionToken)
                controller.registerCallback(mControllerCallback)
                mController = controller
                currentMetadata.value = controller.metadata
                playbackState.value = controller.playbackState
                shuffleMode.value = controller.shuffleMode
                repeatMode.value = controller.repeatMode
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