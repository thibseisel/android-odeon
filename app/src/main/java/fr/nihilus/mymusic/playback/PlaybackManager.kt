package fr.nihilus.mymusic.playback

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import fr.nihilus.mymusic.di.ServiceScoped
import fr.nihilus.mymusic.service.MusicService
import fr.nihilus.mymusic.settings.PreferenceDao
import fr.nihilus.mymusic.utils.MediaID
import javax.inject.Inject

private const val TAG = "PlaybackManager"
private const val HEADSET_CLICK_DELAY = 250L

@ServiceScoped
class PlaybackManager
@Inject constructor(
        service: MusicService,
        queueManager: QueueManager,
        val musicPlayer: MusicPlayer,
        val prefs: PreferenceDao
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

        mQueueManager.currentMusic?.let {
            prefs.lastPlayedMediaId = it.description.mediaId
        }

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
                PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE_ENABLED

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
        private val mHeadsetButtonHandler = Handler()
        @Volatile private var mHeadsetClickCount = 0

        private val mHeadsetButtonRunnable = Runnable {
            if (mHeadsetClickCount == 1) {
                // Single click: play if paused, or pause if playing.
                if (!musicPlayer.isPlaying) onPlay()
                else onPause()
                mHeadsetClickCount = 0
            }
        }

        override fun onPlay() {
            Log.d(TAG, "onPlay")
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

        override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
            Log.d(TAG, "onPlayFromMediaId: mediaId=$mediaId, extras=$extras")
            mQueueManager.loadQueueFromMusic(mediaId)
            handlePlayRequest()
        }

        override fun onPause() {
            Log.d(TAG, "onPause")
            handlePauseRequest()
        }

        override fun onStop() {
            Log.d(TAG, "onStop")
            handleStopRequest(null)
        }

        override fun onSkipToPrevious() {
            Log.d(TAG, "onSkipToPrevious")
            if (mQueueManager.skipPosition(-1))
                if (musicPlayer.isPlaying) handlePlayRequest()
            else handleStopRequest("Cannot skip to previous")
            mQueueManager.updateMetadata()
        }

        override fun onSkipToNext() {
            Log.d(TAG, "onSkipToNext")
            if (mQueueManager.skipPosition(1))
                if (musicPlayer.isPlaying) handlePlayRequest()
            else handleStopRequest("Cannot skip to next")
            mQueueManager.updateMetadata()
        }

        override fun onCustomAction(action: String, extras: Bundle?) {
            Log.d(TAG, "onCustomAction: action=$action, extras=$extras")
            Log.w(TAG, "Unhandled custom action: $action")
        }

        override fun onPlayFromSearch(query: String, extras: Bundle?) {
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

        override fun onMediaButtonEvent(mediaButtonEvent: Intent?): Boolean {
            if (mediaButtonEvent != null) {
                val event = mediaButtonEvent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                if (event.keyCode == KeyEvent.KEYCODE_HEADSETHOOK && event.action == KeyEvent.ACTION_DOWN) {
                    // Event is produced by a click on the headset button.
                    mHeadsetClickCount++
                    if (mHeadsetClickCount > 1) {
                        // Double click: skip to next song.
                        onSkipToNext()
                        mHeadsetButtonHandler.removeCallbacks(mHeadsetButtonRunnable)
                        mHeadsetClickCount = 0
                    } else {
                        // Single click: delay action to wait for a potential second click.
                        mHeadsetButtonHandler.postDelayed(mHeadsetButtonRunnable, HEADSET_CLICK_DELAY)
                    }

                    // Prevent the default behavior (play/pause immediately)
                    return true
                }
            }

            return super.onMediaButtonEvent(mediaButtonEvent)
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
