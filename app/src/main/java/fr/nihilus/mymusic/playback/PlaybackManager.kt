package fr.nihilus.mymusic.playback

import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import fr.nihilus.mymusic.MusicService
import fr.nihilus.mymusic.di.MusicServiceScope
import javax.inject.Inject

private const val TAG = "PlaybackManager"

@MusicServiceScope
open internal class PlaybackManager
@Inject constructor(
        service: MusicService,
        queueManager: QueueManager,
        playback: LocalPlayback
) : LocalPlayback.Callback {

    private val mServiceCallback: ServiceCallback = service
    private val mResources = service.resources
    private val mQueueManager = queueManager
    private val mPlayback = playback

    fun handlePlayRequest() {
        Log.v(TAG, "handlePlayRequest: mState=${mPlayback.state}")
        mQueueManager.currentMusic?.let {
            mServiceCallback.onPlaybackStart()
            mPlayback.play(it)
        }
    }

    fun handlePauseRequest() {
        Log.v(TAG, "handlePauseRequest: mState=${mPlayback.state}")
        if (mPlayback.isPlaying) {
            mPlayback.pause()
            mServiceCallback.onPlaybackStop()
        }
    }

    fun handleStopRequest(error: String?) {
        Log.v(TAG, "handleStopRequest: mState=${mPlayback.state}, error=$error")
        mPlayback.stop()
        mServiceCallback.onPlaybackStop()
        updatePlaybackState(error)
    }

    fun updatePlaybackState(error: String?) {
        Log.v(TAG, "updatePlaybackState: mState=${mPlayback.state}")
        val position = mPlayback.currentPosition

        val stateBuilder = PlaybackStateCompat.Builder()
        setAvailableActions(stateBuilder)
        setCustomActions(stateBuilder)

        var state = mPlayback.state
        if (error != null) {
            stateBuilder.setErrorMessage(PlaybackStateCompat.ERROR_CODE_UNKNOWN_ERROR, error)
            state = PlaybackStateCompat.STATE_ERROR
        }

        stateBuilder.setState(state, position, 1.0f, SystemClock.elapsedRealtime())
        mQueueManager.currentMusic?.let {
            stateBuilder.setActiveQueueItemId(it.queueId)
        }

        mServiceCallback.onPlaybackStateUpdated(stateBuilder.build())

        if (state == PlaybackStateCompat.STATE_PLAYING
                || state == PlaybackStateCompat.STATE_PAUSED) {
            mServiceCallback.onNotificationRequired()
        }
    }

    private fun setAvailableActions(stateBuilder: PlaybackStateCompat.Builder) {
        // Actions that are available at any time
        var actions = PlaybackStateCompat.ACTION_PLAY_PAUSE or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE_ENABLED

        // Give opportunity to pause only when playing
        actions = actions or if (mPlayback.isPlaying)
            PlaybackStateCompat.ACTION_PAUSE
        else PlaybackStateCompat.ACTION_PLAY

        stateBuilder.setActions(actions)
    }

    private fun setCustomActions(stateBuilder: PlaybackStateCompat.Builder) {
        // No custom action at the time.
    }

    override fun onCompletion() {
        if (mQueueManager.skipPosition(+1)) {
            handlePlayRequest()
            mQueueManager.updateMetadata()
        } else {
            handleStopRequest(null)
        }
    }

    override fun onPlaybackStatusChanged(newState: Int) {
        updatePlaybackState(null)
    }

    override fun onError(message: String) {
        updatePlaybackState(message)
    }

    /**
     *
     */
    internal val mediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onPlay() {
            Log.v(TAG, "onPlay")
            if (mQueueManager.currentMusic == null) {
                mQueueManager.loadRandomQueue()
            }

            handlePlayRequest()
        }

        override fun onSkipToQueueItem(queueId: Long) {
            Log.v(TAG, "onSkipToQueueItem: queueId=$queueId")
            mQueueManager.setCurrentQueueItem(queueId)
            mQueueManager.updateMetadata()
        }

        override fun onSeekTo(position: Long) {
            Log.v(TAG, "onSeekTo: position=$position")
            mPlayback.seekTo(position)
        }

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
            Log.v(TAG, "onPlayFromMediaId: mediaId=$mediaId, extras=$extras")
            mQueueManager.loadQueueFromMusic(mediaId)
        }

        override fun onPause() {
            Log.v(TAG, "onPause")
            handlePauseRequest()
        }

        override fun onStop() {
            Log.v(TAG, "onStop")
            handleStopRequest(null)
        }

        override fun onSkipToPrevious() {
            Log.v(TAG, "onSkipToPrevious")
            if (mQueueManager.skipPosition(-1))
                handlePlayRequest()
            else handleStopRequest("Cannot skip")
            mQueueManager.updateMetadata()
        }

        override fun onSkipToNext() {
            Log.v(TAG, "onSkipToNext")
            if (mQueueManager.skipPosition(1))
                handlePlayRequest()
            else handleStopRequest("Cannot skip")
            mQueueManager.updateMetadata()
        }

        override fun onCustomAction(action: String, extras: Bundle?) {
            Log.v(TAG, "onCustomAction: action=$action, extras=$extras")
            Log.w(TAG, "Unhandled custom action: $action")
        }

        override fun onPlayFromSearch(query: String, extras: Bundle?) {
            Log.v(TAG, "onPlayFromSearch: query=$query, extras=$extras")
            val searchSuccessful = mQueueManager.loadQueueFromSearch(query, extras)
            if (searchSuccessful) {
                handlePlayRequest()
                mQueueManager.updateMetadata()
            } else {
                updatePlaybackState("No queue could be build from the query: $query")
            }
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            // TODO Modify MediaSession shuffle mode and queue order accordingly
            when(shuffleMode) {
                PlaybackStateCompat.SHUFFLE_MODE_NONE -> TODO("Reorder queue")
                PlaybackStateCompat.SHUFFLE_MODE_ALL -> TODO("Shuffle queue")
            }
        }
    }

    interface ServiceCallback {
        fun onPlaybackStart()
        fun onNotificationRequired()
        fun onPlaybackStop()
        fun onPlaybackStateUpdated(newState: PlaybackStateCompat)
    }
}