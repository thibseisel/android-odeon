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

package fr.nihilus.music.playback

import android.os.Bundle
import android.os.ResultReceiver
import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import fr.nihilus.music.command.MediaSessionCommand
import fr.nihilus.music.di.ServiceScoped
import fr.nihilus.music.service.MusicService
import fr.nihilus.music.settings.PreferenceDao
import fr.nihilus.music.utils.MediaID
import javax.inject.Inject

private const val TAG = "PlaybackManager"

@ServiceScoped
class PlaybackManager
@Inject constructor(
        service: MusicService,
        queueManager: QueueManager,
        val musicPlayer: MusicPlayer,
        val prefs: PreferenceDao,
        val commands: Map<String, @JvmSuppressWildcards MediaSessionCommand>
) : MusicPlayer.Callback {

    private val mServiceCallback: ServiceCallback = service
    private val mResources = service.resources
    private val mQueueManager = queueManager

    /**
     * Initialize the musicPlayer manager.
     */
    fun init() {
        musicPlayer.callback = this

        mediaSessionCallback.onSetShuffleMode(prefs.shuffleMode)
        prefs.lastPlayedMediaId?.let {
            // Recover the queue from the last time it has played
            mQueueManager.loadQueueFromMusic(it)
        }
    }

    fun handlePlayRequest() {
        Log.d(TAG, "handlePlayRequest: mState=${musicPlayer.state}")
        mQueueManager.currentMusic?.let {
            mServiceCallback.onPlaybackStart()
            musicPlayer.play(it)
            prefs.lastPlayedMediaId = it.description.mediaId
        }
    }

    fun handlePauseRequest() {
        Log.d(TAG, "handlePauseRequest: mState=${musicPlayer.state}")
        if (musicPlayer.isPlaying) {
            musicPlayer.pause()
            mServiceCallback.onPlaybackStop()
        }
    }

    fun handleStopRequest(withError: String?) {
        Log.d(TAG, "handleStopRequest: mState=${musicPlayer.state}, withError=$withError")

        musicPlayer.stop()
        mServiceCallback.onPlaybackStop()
        updatePlaybackState(withError)
    }

    fun updatePlaybackState(error: String?) {
        Log.d(TAG, "updatePlaybackState: mState=${musicPlayer.state}")
        val position = musicPlayer.currentPosition

        val stateBuilder = PlaybackStateCompat.Builder()
        setAvailableActions(stateBuilder)
        setCustomActions(stateBuilder)

        var state = musicPlayer.state
        if (error != null) {
            stateBuilder.setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR, error)
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
                PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE_ENABLED or
                PlaybackStateCompat.ACTION_STOP

        if (mQueueManager.canSkip(-1))
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
        if (mQueueManager.canSkip(+1))
            actions = actions or PlaybackStateCompat.ACTION_SKIP_TO_NEXT

        // Give opportunity to pause only when playing
        actions = actions or if (musicPlayer.isPlaying)
            PlaybackStateCompat.ACTION_PAUSE
        else PlaybackStateCompat.ACTION_PLAY

        stateBuilder.setActions(actions)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun setCustomActions(stateBuilder: PlaybackStateCompat.Builder) {
        // No custom action at the time.
    }

    override fun onCompletion() {
        if (mQueueManager.skipPosition(1)) {
            handlePlayRequest()
            mQueueManager.updateMetadata()
        } else {
            // If skipping was not possible, we stop and release the resources
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
     * Handle commands received by the media session.
     * Available commands are configured by [setAvailableActions].
     */
    internal val mediaSessionCallback = object : MediaSessionCompat.Callback() {

        override fun onPlay() {
            Log.d(TAG, "onActionPlay")
            if (mQueueManager.currentMusic == null) {
                Log.d(TAG, "Playing without a queue. Play last played media id")
                val lastPlayedMediaId = prefs.lastPlayedMediaId ?: MediaID.ID_MUSIC
                mQueueManager.loadQueueFromMusic(lastPlayedMediaId)
            }

            handlePlayRequest()
        }

        override fun onSkipToQueueItem(queueId: Long) {
            Log.d(TAG, "onSkipToQueueItem: queueId=$queueId")
            mQueueManager.setCurrentQueueItem(queueId)
            mQueueManager.updateMetadata()
        }

        override fun onSeekTo(position: Long) {
            Log.d(TAG, "onSeekTo: position=$position")
            musicPlayer.seekTo(position)
        }

        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            Log.d(TAG, "onPlayFromMediaId: mediaId=$mediaId, extras=$extras")
            if (mediaId != null) {
                mQueueManager.loadQueueFromMusic(mediaId)
                handlePlayRequest()
            }
        }

        override fun onPause() {
            Log.d(TAG, "onActionPause")
            handlePauseRequest()
        }

        override fun onStop() {
            Log.d(TAG, "onStop")
            handleStopRequest(null)
        }

        override fun onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious")

            if (musicPlayer.currentPosition < 3000L) {
                // Skip to previous if not playing for more than 3 seconds
                if (mQueueManager.skipPosition(-1)) {
                    if (musicPlayer.isPlaying) handlePlayRequest()
                } else handleStopRequest("Cannot skip to previous")
                mQueueManager.updateMetadata()
            } else {
                // If more than 3 seconds, restarts this track at 0
                musicPlayer.seekTo(0L)
            }

        }

        override fun onSkipToNext() {
            Log.d(TAG, "onSkipToNext")
            if (mQueueManager.skipPosition(1)) {
                if (musicPlayer.isPlaying) handlePlayRequest()
            } else handleStopRequest("Cannot skip to next")
            mQueueManager.updateMetadata()
        }

        override fun onCustomAction(action: String?, extras: Bundle?) {
            Log.d(TAG, "onCustomAction: action=$action, extras=$extras")
            Log.w(TAG, "Unhandled custom action: $action")
        }

        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            Log.d(TAG, "onPlayFromSearch: query=$query, extras=$extras")
            val searchSuccessful = mQueueManager.loadQueueFromSearch(query, extras)
            if (searchSuccessful) {
                handlePlayRequest()
                mQueueManager.updateMetadata()
            } else {
                updatePlaybackState("No queue could be build from the query: $query")
            }
        }

        override fun onSetShuffleMode(shuffleMode: Int) {
            Log.d(TAG, "New shuffle mode: $shuffleMode")
            prefs.shuffleMode = shuffleMode
            mQueueManager.shuffleMode = shuffleMode
            mServiceCallback.onShuffleModeChanged(shuffleMode)
            updatePlaybackState(null)
        }

        override fun onCommand(commandName: String?, extras: Bundle?, cb: ResultReceiver?) {
            commands[commandName]?.handle(extras, cb)
                    ?: cb?.send(MediaSessionCommand.CODE_UNKNOWN_COMMAND, null)
        }
    }

    interface ServiceCallback {
        fun onPlaybackStart()
        fun onNotificationRequired()
        fun onPlaybackStop()
        fun onPlaybackStateUpdated(newState: PlaybackStateCompat)
        fun onShuffleModeChanged(@PlaybackStateCompat.ShuffleMode shuffleMode: Int)
    }
}
