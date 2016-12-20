/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fr.nihilus.mymusic.playback;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.IntDef;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import fr.nihilus.mymusic.utils.MediaID;

/**
 * A class that implements local media playback using {@link MediaPlayer}.
 */
class Playback implements AudioManager.OnAudioFocusChangeListener,
        MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener,
        MediaPlayer.OnPreparedListener, MediaPlayer.OnSeekCompleteListener {

    private static final float VOLUME_DUCK = 0.2f, VOLUME_NORMAL = 1.0f;
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0, AUDIO_NO_FOCUS_CAN_DUCK = 1, AUDIO_FOCUSED = 2;
    private static final String TAG = "Playback";
    private final MusicService mService;

    /**
     * Filtre l'évènement "Le casque est débranché pendant la lecture audio"
     */
    private final IntentFilter mAudioNoisyIntentFilter =
            new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);

    /**
     * Reçoit l'évènement "le casque a été débranché pendant la lecture audio
     */
    private final BroadcastReceiver mAudioNoisyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                Log.d(TAG, "Headphones disconnected.");
                // Mettre la lecture en pause si active
                Intent pauseIntent = new Intent(context, MusicService.class);
                pauseIntent.setAction(MusicService.ACTION_CMD);
                pauseIntent.putExtra(MusicService.CMD_NAME, MusicService.CMD_PAUSE);
                mService.startService(pauseIntent);
            }
        }
    };
    private final AudioManager mAudioManager;
    private int mState;
    private boolean mPlayOnFocusGain;
    private MediaPlayer mMediaPlayer;
    private volatile int mCurrentPosition;
    private volatile String mCurrentMediaId;
    @AudioFocus
    private int mAudioFocus;
    private volatile boolean mAudioNoisyReceiverRegistered;
    private Callback mCallback;

    Playback(MusicService service) {
        mService = service;
        mAudioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
    }

    void stop(boolean notifyListeners) {
        mState = PlaybackStateCompat.STATE_STOPPED;
        if (notifyListeners && mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
        mCurrentPosition = getCurrentStreamPosition();
        // Give up Audio Focus
        giveUpAudioFocus();
        unregisterAudioNoisyReceiver();
        // Relax all resources
        relaxResources(true);
    }

    @PlaybackStateCompat.State
    int getState() {
        return mState;
    }

    void setState(@PlaybackStateCompat.State int state) {
        mState = state;
    }

    boolean isConnected() {
        return true;
    }

    boolean isPlaying() {
        return mPlayOnFocusGain || (mMediaPlayer != null
                && mMediaPlayer.isPlaying());
    }

    int getCurrentStreamPosition() {
        return mMediaPlayer != null ?
                mMediaPlayer.getCurrentPosition() :
                mCurrentPosition;
    }

    void play(MediaSessionCompat.QueueItem item) {
        mPlayOnFocusGain = true;
        tryToGetAudioFocus();
        registerAudioNoisyReceiver();

        // Récupère l'identifiant de la musique à jouer maintenant
        String mediaId = item.getDescription().getMediaId();
        boolean mediaHasChanged = !TextUtils.equals(mediaId, mCurrentMediaId);
        if (mediaHasChanged) {
            // Si ce n'est plus la même piste, mettre à jour position et ID
            mCurrentPosition = 0;
            mCurrentMediaId = mediaId;
        }

        if (mState == PlaybackStateCompat.STATE_PAUSED && !mediaHasChanged
                && mMediaPlayer != null) {
            configMediaPlayerState();
        } else {
            mState = PlaybackStateCompat.STATE_STOPPED;
            relaxResources(false); // Release everything exept MediaPlayer

            Uri source = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendEncodedPath(MediaID.extractMusicIDFromMediaID(mediaId))
                    .build();

            try {
                createMediaPlayerIfNeeded();
                mState = PlaybackStateCompat.STATE_BUFFERING;
                mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mMediaPlayer.setDataSource(mService, source);

                mMediaPlayer.prepareAsync();

                if (mCallback != null) {
                    mCallback.onPlaybackStatusChanged(mState);
                }

            } catch (IOException e) {
                Log.e(TAG, "play: can't load from datasource.", e);
                if (mCallback != null) {
                    mCallback.onError(e.getMessage());
                }
            }
        }
    }

    void pause() {
        if (mState == PlaybackStateCompat.STATE_PLAYING) {
            // Pause media and cancel the foreground service state.
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
                mCurrentPosition = mMediaPlayer.getCurrentPosition();
            }

            // While paused, retain the MediaPlayer but give up audio focus
            relaxResources(false);
            giveUpAudioFocus();
        }
        mState = PlaybackStateCompat.STATE_PAUSED;
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
        unregisterAudioNoisyReceiver();
    }

    void seekTo(int position) {
        Log.d(TAG, "seekTo: called with position " + position);

        if (mMediaPlayer == null) {
            // If we do not have a current media player, simply update the current position.
            mCurrentPosition = position;
        } else {
            if (mMediaPlayer.isPlaying()) {
                mState = PlaybackStateCompat.STATE_BUFFERING;
            }
            mMediaPlayer.seekTo(position);
            if (mCallback != null) {
                mCallback.onPlaybackStatusChanged(mState);
            }
        }
    }

    void setCallback(Callback callback) {
        mCallback = callback;
    }

    /**
     * Try to get the system audio focus.
     */
    private void tryToGetAudioFocus() {
        Log.d(TAG, "tryToGetAudioFocus");
        if (mAudioFocus != AUDIO_FOCUSED) {
            int result = mAudioManager.requestAudioFocus(this,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_FOCUSED;
            }
        }
    }

    private void giveUpAudioFocus() {
        Log.d(TAG, "giveUpAudioFocus");
        if (mAudioFocus == AUDIO_FOCUSED) {
            if (mAudioManager.abandonAudioFocus(this)
                    == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                mAudioFocus = AUDIO_NO_FOCUS_NO_DUCK;
            }
        }
    }

    /**
     * Reconfigures MediaPlayer according to audio focus settings and
     * starts/restarts it. This method starts/restarts the MediaPlayer
     * respecting the current audio focus state. So if we have focus, it will
     * play normally; if we don't have focus, it will either leave the
     * MediaPlayer paused or set it to a low volume, depending on what is
     * allowed by the current focus settings. This method assumes mMediaPlayer
     * != null, so if you are calling it, you have to do so from a context
     * where you are sure this is the case.
     */
    private void configMediaPlayerState() {
        Log.d(TAG, "configMediaPlayerState. mAudioFocus=" + mAudioFocus);
        if (mAudioFocus == AUDIO_NO_FOCUS_NO_DUCK) {
            // If we don't have audio focus and can't duck, we have to pause
            if (mState == PlaybackStateCompat.STATE_PLAYING) {
                pause();
            }
        } else { // We have audio focus
            if (mAudioFocus == AUDIO_NO_FOCUS_CAN_DUCK) {
                // We'll be relatively quiet
                mMediaPlayer.setVolume(VOLUME_DUCK, VOLUME_DUCK);
            } else {
                if (mMediaPlayer != null) {
                    // We can be loud again
                    mMediaPlayer.setVolume(VOLUME_NORMAL, VOLUME_NORMAL);
                }
            }
            // If we were playing when we lost focus, we need to resume playing.
            if (mPlayOnFocusGain) {
                if (mMediaPlayer != null && !mMediaPlayer.isPlaying()) {
                    Log.d(TAG, "configMediaPlayerState startMediaPlayer. Seeking to "
                            + mCurrentPosition);
                    if (mCurrentPosition == mMediaPlayer.getCurrentPosition()) {
                        mMediaPlayer.start();
                        mState = PlaybackStateCompat.STATE_PLAYING;
                    } else {
                        mMediaPlayer.seekTo(mCurrentPosition);
                        mState = PlaybackStateCompat.STATE_BUFFERING;
                    }
                }
                mPlayOnFocusGain = false;
            }
        }
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    /**
     * Called by AudioManager on audio focus changes.
     * Implementation of
     * {@link AudioManager.OnAudioFocusChangeListener}
     */
    @Override
    public void onAudioFocusChange(int focusChange) {
        Log.d(TAG, "onAudioFocusChange. focusChange=" + focusChange);
        if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            // We have gained focus:
            mAudioFocus = AUDIO_FOCUSED;

        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
                focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            // We have lost focus. If we can duck (low playback volume), we can keep playing.
            // Otherwise, we need to pause the playback.
            boolean canDuck = focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK;
            mAudioFocus = canDuck ? AUDIO_NO_FOCUS_CAN_DUCK : AUDIO_NO_FOCUS_NO_DUCK;

            // If we are playing, we need to reset media player
            // by calling configuMediaPlayerState with mAudioFocus properly set.
            if (mState == PlaybackStateCompat.STATE_PLAYING && !canDuck) {
                // If we don't have audio focus and can't duck, we save the information
                // we were playing, so that we can resume playback once we get the focus back.
                mPlayOnFocusGain = true;
            }
        } else {
            Log.d(TAG, "onAudioFocusChange: Ignoring unsupported focusChange: " + focusChange);
        }
        configMediaPlayerState();
    }

    /**
     * Called when MediaPlayer has completed a seek
     *
     * @see MediaPlayer.OnSeekCompleteListener
     */
    @Override
    public void onSeekComplete(MediaPlayer mp) {
        Log.d(TAG, "onSeekComplete from MediaPlayer: " + mp.getCurrentPosition());
        mCurrentPosition = mp.getCurrentPosition();
        if (mState == PlaybackStateCompat.STATE_BUFFERING) {
            mMediaPlayer.start();
            mState = PlaybackStateCompat.STATE_PLAYING;
        }
        if (mCallback != null) {
            mCallback.onPlaybackStatusChanged(mState);
        }
    }

    /**
     * Called when MediaPlayer is done playing current song.
     *
     * @see MediaPlayer.OnCompletionListener
     */
    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onCompletion from MediaPlayer");
        // The media player finished plaing the current song, so we go ahead and start the next.

        if (mCallback != null) {
            mCallback.onCompletion();
        }
    }

    /**
     * Called when media player is done preparing.
     *
     * @see MediaPlayer.OnPreparedListener
     */
    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onPrepared from MediaPlayer");
        // The media player is done preparing.
        // That means we can start playing if we have audio focus.
        configMediaPlayerState();
    }

    /**
     * Called when there's an error playing media.
     * When this happens, the media player goes to the Error state.
     * We warn the user about the error and reset te media player.
     *
     * @see MediaPlayer.OnErrorListener
     */
    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        Log.e(TAG, "Media player error: what=" + what + ", extra=" + extra);

        if (mCallback != null) {
            mCallback.onError("MediaPlayer error " + what + " (" +
                    extra + ")");
        }
        return true; // true indicated we handled the error
    }

    /**
     * Makes sure the media player exists and has been reset. This will create
     * the media player if needed, or reset the existing media player if one
     * already exists.
     */
    private void createMediaPlayerIfNeeded() {
        Log.d(TAG, "createMediaPlayerIfNeeded. needed ? "
                + (mMediaPlayer == null));
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();

            // Make sure the media player will acquire a wake-lock while playing.
            // If we don't do that, the CPU might go to sleep while the song is playing,
            // causing playback to stop.
            mMediaPlayer.setWakeMode(mService.getApplicationContext(),
                    PowerManager.PARTIAL_WAKE_LOCK);

            // We want the media player to notify us when it's ready preparing,
            // and when it's done playing.
            mMediaPlayer.setOnPreparedListener(this);
            mMediaPlayer.setOnCompletionListener(this);
            mMediaPlayer.setOnErrorListener(this);
            mMediaPlayer.setOnSeekCompleteListener(this);
        } else {
            mMediaPlayer.reset();
        }
    }

    /**
     * Releases resources used by the service for playback.
     * This includes the "foreground service" status, the wake locks an possibly
     * the MediaPlayer.
     *
     * @param releaseMediaPlayer Indicates whether the Media Player should also be released or not
     */
    private void relaxResources(boolean releaseMediaPlayer) {
        Log.d(TAG, "relaxResources. releaseMediaPlayer=" + releaseMediaPlayer);

        mService.stopForeground(true);

        // Stop and release the Media Player, if it's available
        if (releaseMediaPlayer && mMediaPlayer != null) {
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void registerAudioNoisyReceiver() {
        if (!mAudioNoisyReceiverRegistered) {
            mService.registerReceiver(mAudioNoisyReceiver,
                    mAudioNoisyIntentFilter);
            mAudioNoisyReceiverRegistered = true;
        }
    }

    private void unregisterAudioNoisyReceiver() {
        if (mAudioNoisyReceiverRegistered) {
            mService.unregisterReceiver(mAudioNoisyReceiver);
            mAudioNoisyReceiverRegistered = false;
        }
    }

    interface Callback {
        /**
         * On current music completed.
         */
        void onCompletion();

        /**
         * On Playback status changed
         * Implementations can use this callback to update playback state
         * on the media sessions.
         */
        void onPlaybackStatusChanged(int state);

        /**
         * @param error to be added to the PlaybackState
         */
        void onError(String error);
    }

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({AUDIO_NO_FOCUS_NO_DUCK, AUDIO_NO_FOCUS_CAN_DUCK, AUDIO_FOCUSED})
    @interface AudioFocus {
    }
}
