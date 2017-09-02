package fr.nihilus.mymusic.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.text.TextUtils
import android.util.Log
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import fr.nihilus.mymusic.R
import fr.nihilus.mymusic.di.ServiceScoped
import fr.nihilus.mymusic.service.MusicService
import javax.inject.Inject

private const val TAG = "ExoMusicPlayer"

/**
 * The volume level to use when we lose audio focus,
 * but are allowed to reduce the volume instead of stopping playback.
 */
private const val VOLUME_DUCK = 0.2f
/** The volume level to use when we have audio focus. */
private const val VOLUME_NORMAL = 1.0f

/** We don't have audio focus and can't duck (play at a low volume). */
private const val AUDIO_NO_FOCUS_NO_DUCK = 0
/** We don't have focus, but we can duck (play at a low volume). */
private const val AUDIO_NO_FOCUS_CAN_DUCK = 1
/** We have full audio focus. We are allowed to play loudly. */
private const val AUDIO_FOCUSED = 2

/**
 * Perform local media playback using [ExoPlayer].
 */
@ServiceScoped
internal class ExoMusicPlayer
@Inject constructor(private val context: Context) : ExoPlayer.EventListener, MusicPlayer {

    private val mAudioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val mAudioNoisyIntentFilter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)

    private var mExoPlayer: SimpleExoPlayer? = null
    private var mPlayOnFocusGain = false
    private var mPlayerNullIfStopped = false
    private var mAudioNoisyReceiverRegistered = false
    private var mCurrentAudioFocus = AUDIO_NO_FOCUS_NO_DUCK

    private val mExtractorFactory: ExtractorsFactory = DefaultExtractorsFactory()
    private val mDataSourceFactory: DataSource.Factory

    init {
        val userAgent = Util.getUserAgent(context, context.getString(R.string.app_name))
        mDataSourceFactory = DefaultDataSourceFactory(context, userAgent)
    }

    override var callback: MusicPlayer.Callback? = null

    /**
     * The ID of the currently playing media.
     */
    override var currentMediaId: String? = null

    /**
     * The current [android.media.session.PlaybackState.getState]
     */
    @PlaybackStateCompat.State
    override val state: Int
        get() {
            if (mExoPlayer == null) {
                return if (mPlayerNullIfStopped) PlaybackStateCompat.STATE_STOPPED
                else PlaybackStateCompat.STATE_NONE
            }

            return when (mExoPlayer!!.playbackState) {
                ExoPlayer.STATE_IDLE -> PlaybackStateCompat.STATE_PAUSED
                ExoPlayer.STATE_BUFFERING -> PlaybackStateCompat.STATE_BUFFERING
                ExoPlayer.STATE_READY -> if (mExoPlayer!!.playWhenReady)
                    PlaybackStateCompat.STATE_PLAYING else PlaybackStateCompat.STATE_PAUSED
                else -> PlaybackStateCompat.STATE_NONE
            }
        }

    /**
     * Indicate whether the mExoPlayer is playing or is supposed to be
     * playing when we gain audio focus.
     */
    override val isPlaying get() = mPlayOnFocusGain || mExoPlayer?.playWhenReady ?: false

    /**
     * The current position in the audio stream in milliseconds.
     */
    override val currentPosition get() = mExoPlayer?.currentPosition ?: 0L

    /**
     * Start playback for an [item] in the queue.
     * If the media id of this item is the same as [currentMediaId], then the playback is resumed.
     * Otherwise, the specified [item] will be played instead.
     *
     * MusicPlayer will begin only if the application has audio focus.
     */
    override fun play(item: MediaSessionCompat.QueueItem) {
        Log.d(TAG, "play called with item: $item")
        mPlayOnFocusGain = true
        tryToGetAudioFocus()
        registerAudioNoisyReceiver()

        val mediaId = item.description.mediaId
                ?: throw IllegalStateException("Queue item has no media id")
        val mediaHasChanged = !TextUtils.equals(mediaId, currentMediaId)
        if (mediaHasChanged) currentMediaId = mediaId

        if (mediaHasChanged || mExoPlayer == null) {
            releaseResources(false)
            val sourceUri = item.description.mediaUri
                    ?: throw IllegalStateException("Queue item has no source URI")

            if (mExoPlayer == null) {
                mExoPlayer = ExoPlayerFactory.newSimpleInstance(
                        DefaultRenderersFactory(context),
                        DefaultTrackSelector(),
                        DefaultLoadControl())
                mExoPlayer!!.addListener(this)
            }

            val mediaSource = ExtractorMediaSource(sourceUri, mDataSourceFactory,
                    mExtractorFactory, null, null)

            // Prepares media to play (happen on background thread) an triggers
            // onPlayerStateChanged callback when the stream is ready to play.
            mExoPlayer!!.prepare(mediaSource)
        }

        configurePlayerState()
    }

    /**
     * Pause the media playback.
     * If you call this method during media buffering,
     * playback will not resume when the loading is complete.
     *
     * Listeners are notified when playback state is updated.
     */
    override fun pause() {
        // Pause player and cancel the foreground service state.
        Log.d(TAG, "pause called")
        mExoPlayer?.playWhenReady = false

        releaseResources(false)
        unregisterAudioNoisyReceiver()
    }

    /**
     * Seek to the specified position in the currently playing media.
     * When playing, playback will resume at this position.
     * @param position in the currently playing media in milliseconds
     */
    override fun seekTo(position: Long) {
        Log.d(TAG, "seekTo called with $position")
        if (mExoPlayer != null) {
            registerAudioNoisyReceiver()
            mExoPlayer!!.seekTo(position)
        }
    }

    /**
     * Stop the player. All resources are de-allocated.
     */
    override fun stop() {
        giveUpAudioFocus()
        unregisterAudioNoisyReceiver()
        releaseResources(true)
    }

    private fun tryToGetAudioFocus() {
        Log.d(TAG, "TryToGetAudioFocus")
        val result = mAudioManager.requestAudioFocus(mAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

        mCurrentAudioFocus =
                if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) AUDIO_FOCUSED
                else AUDIO_NO_FOCUS_NO_DUCK
    }

    private fun giveUpAudioFocus() {
        Log.d(TAG, "GiveUpAudioFocus")
        if (mAudioManager.abandonAudioFocus(mAudioFocusChangeListener)
                == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mCurrentAudioFocus = AUDIO_NO_FOCUS_NO_DUCK
        }
    }

    private fun configurePlayerState() {
        Log.d(TAG, "configurePlayerState. mCurrentAudioFocus=$mCurrentAudioFocus")
        if (mCurrentAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // We don't have audio focus and can't duck, so we have to pause
            pause()
        } else {
            registerAudioNoisyReceiver()

            if (mCurrentAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                // We're permitted to play, but only if we "duck" (play quietly)
                mExoPlayer!!.volume = VOLUME_DUCK
            } else {
                mExoPlayer!!.volume = VOLUME_NORMAL
            }

            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                mExoPlayer!!.playWhenReady = true
                mPlayOnFocusGain = false
            }
        }
    }

    private fun registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            context.registerReceiver(audioNoisyReceiver, mAudioNoisyIntentFilter)
            mAudioNoisyReceiverRegistered = true
        }
    }

    private fun unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            context.unregisterReceiver(audioNoisyReceiver)
            mAudioNoisyReceiverRegistered = false
        }
    }

    /**
     * Releases resources used by the service for playback.
     * @param releasePlayer whether the mExoPlayer should also be released
     */
    private fun releaseResources(releasePlayer: Boolean) {
        Log.d(TAG, "releaseResources: releasePlayer = $releasePlayer")

        if (releasePlayer && mExoPlayer != null) {
            mExoPlayer!!.release()
            mExoPlayer!!.removeListener(this)
            mExoPlayer = null
            mPlayerNullIfStopped = true
            mPlayOnFocusGain = false
        }
    }

    private val mAudioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focus ->
        when (focus) {
            AudioManager.AUDIOFOCUS_GAIN -> mCurrentAudioFocus = AUDIO_FOCUSED
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                // Audio focus was lost, but it's possible to duck
                mCurrentAudioFocus = AUDIO_NO_FOCUS_CAN_DUCK
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Lost focus, but will gain it back so note whether playback should resume
                mCurrentAudioFocus = AUDIO_NO_FOCUS_NO_DUCK
                mPlayOnFocusGain = true
            }
            AudioManager.AUDIOFOCUS_LOSS ->
                // Lost audio focus, probably permanently
                mCurrentAudioFocus = AUDIO_NO_FOCUS_NO_DUCK
            else -> Log.w(TAG, "Unhandled AudioFocus state: $focus")
        }

        if (mExoPlayer != null) {
            // Update the player state based on the change
            configurePlayerState()
        }
    }

    private val audioNoisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                Log.d(TAG, "Headphones disconnected.")
                if (isPlaying) {
                    val pauseIntent = Intent(context, MusicService::class.java)
                    pauseIntent.action = MusicService.ACTION_CMD
                    pauseIntent.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE)
                    context.startService(pauseIntent)
                }
            }
        }
    }

    override fun onTimelineChanged(timeline: Timeline, manifest: Any?) {
        // Nothing to do.
    }

    override fun onTracksChanged(trackGroups: TrackGroupArray, trackSelections: TrackSelectionArray) {
        // Nothing to do.
    }

    override fun onLoadingChanged(isLoading: Boolean) {
        // Nothing to do.
    }

    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        Log.d(TAG, "ExoPlayer new state: $playbackState, playWhenReady: $playWhenReady")
        when (playbackState) {
            ExoPlayer.STATE_IDLE, ExoPlayer.STATE_READY ->
                callback?.onPlaybackStatusChanged(state)
            ExoPlayer.STATE_ENDED -> callback?.onCompletion()
        }
    }

    override fun onPlayerError(error: ExoPlaybackException) {
        val what = when (error.type) {
            ExoPlaybackException.TYPE_SOURCE -> error.sourceException.message
            ExoPlaybackException.TYPE_RENDERER -> error.rendererException.message
            ExoPlaybackException.TYPE_UNEXPECTED -> error.unexpectedException.message
            else -> "Unknown $error"
        }

        Log.e(TAG, "Exoplayer error: what=$what")
        callback?.onError("ExoPlayer error $what")
    }

    override fun onPositionDiscontinuity() {
        // Nothing to do.
    }

    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        // Nothing to do.
    }

}