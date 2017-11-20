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

package fr.nihilus.music.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.content.res.AppCompatResources;
import android.text.format.DateUtils;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import fr.nihilus.music.R;
import fr.nihilus.music.glide.GlideApp;
import fr.nihilus.music.utils.ViewUtils;

public class PlayerView extends ConstraintLayout {

    private static final String TAG = "PlayerView";
    private static final int LEVEL_PLAYING = 1;
    private static final int LEVEL_PAUSED = 0;
    private static final int PROGRESS_UPDATE_INITIAL_DELAY = 100;
    private static final int PROGRESS_UPDATE_PERIOD = 1000;

    private RequestBuilder<Bitmap> mGlideRequest;
    private TextView mTitle;
    private TextView mSubtitle;
    private ImageView mAlbumArt;
    private MediaSeekBar mProgress;
    private ImageView mPlayPauseButton;
    private ImageView mPreviousButton;
    private ImageView mNextButton;
    private ImageView mMasterPlayPause;
    private ImageView mBigArt;
    private ImageView mShuffleModeButton;
    private ImageView mRepeatModeButton;

    private TextView mSeekPosition;
    private TextView mSeekDuration;

    private EventListener mListener;

    private boolean mExpanded = false;
    private int mRepeatMode = PlaybackStateCompat.REPEAT_MODE_NONE;
    private PlaybackStateCompat mLastPlaybackState;
    private MediaMetadataCompat mMetadata;
    private Transition mOpenTransition;
    private final StringBuilder mDurationBuilder = new StringBuilder();

    private final Handler mHandler = new Handler();
    private final ScheduledExecutorService mExecutorService =
            Executors.newSingleThreadScheduledExecutor();

    private final Runnable mUpdateProgressTask = new Runnable() {
        @Override
        public void run() {
            updateProgress();
        }
    };

    private ScheduledFuture<?> mScheduleFuture;

    public PlayerView(Context context) {
        this(context, null, 0);
    }

    public PlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(context, R.layout.view_player, this);

        // Make this view appear above AppbarLayout
        ViewCompat.setElevation(this, getResources().getDimensionPixelSize(R.dimen.playerview_elevation));
        // Prevent from dispatching touches to views behind
        setClickable(true);

        if (isInEditMode()) {
            return;
        }

        Drawable dummyAlbumArt = AppCompatResources.getDrawable(context, R.drawable.ic_audiotrack_24dp);
        mGlideRequest = GlideApp.with(context).asBitmap()
                .fitCenter()
                .error(dummyAlbumArt)
                .diskCacheStrategy(DiskCacheStrategy.NONE);
    }

    private static boolean hasFlag(long actions, long flag) {
        return (actions & flag) == flag;
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        // Do not dispatch pressed event to View children
    }

    /**
     * Expand or collapse the PlayerView with an animation.
     * When expanded, it is drawn above the main content view.
     * When collapsed, only the top is visible.
     * If the playerView is not a direct child of CoordinatorLayout, this method will do nothing.
     *
     * @param expanded true to expand the PlayerView, false to collapse
     */
    public void setExpanded(boolean expanded) {
        if (mExpanded != expanded) {
            if (expanded) onOpen(true);
            else onClose(true);
            mExpanded = expanded;
        }
    }

    public void updateMetadata(@Nullable MediaMetadataCompat metadata) {
        if (metadata != null) {
            mMetadata = metadata;
            MediaDescriptionCompat media = metadata.getDescription();
            mTitle.setText(media.getTitle());
            mSubtitle.setText(media.getSubtitle());

            Bitmap bitmap = media.getIconBitmap();
            if (bitmap != null) {
                mAlbumArt.setImageBitmap(media.getIconBitmap());
            } else {
                mAlbumArt.setImageResource(R.drawable.dummy_album_art);
            }

            mGlideRequest.load(media.getIconUri()).into(mBigArt);
            mProgress.setMetadata(metadata);

            String durationText = DateUtils.formatElapsedTime(mDurationBuilder,
                    metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) / 1000L);
            mSeekDuration.setText(durationText);
        }
    }

    public void updatePlaybackState(@Nullable PlaybackStateCompat newState) {
        if (newState == null) {
            reset();
            return;
        }

        boolean hasChanged = (mLastPlaybackState == null)
                || (mLastPlaybackState.getState() != newState.getState());
        Log.d(TAG, "updatePlaybackState: hasChanged=[" + hasChanged + "]");

        mProgress.setPlaybackState(newState);
        toggleControls(newState.getActions());
        boolean isPlaying = newState.getState() == PlaybackStateCompat.STATE_PLAYING;
        mLastPlaybackState = newState;

        String position = DateUtils.formatElapsedTime(mDurationBuilder, newState.getPosition() / 1000L);
        mSeekPosition.setText(position);

        if (hasChanged) {
            togglePlayPauseButton(mPlayPauseButton, isPlaying);
            togglePlayPauseButton(mMasterPlayPause, isPlaying);
        }

        if (isPlaying) {
            scheduleProgressUpdate();
        } else {
            stopProgressUpdate();
        }
    }

    private void onOpen(boolean animate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && animate) {
            TransitionManager.beginDelayedTransition(this, mOpenTransition);
        }

        int eightDps = ViewUtils.dipToPixels(getContext(), 8);
        mAlbumArt.setPadding(eightDps, eightDps, eightDps, eightDps);
        mPlayPauseButton.setVisibility(View.GONE);
    }

    private void onClose(boolean animate) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && animate) {
            TransitionManager.beginDelayedTransition(this, mOpenTransition);
        }

        mAlbumArt.setPadding(0, 0, 0, 0);
        mPlayPauseButton.setVisibility(View.VISIBLE);
    }

    private void togglePlayPauseButton(ImageView button, boolean isPlaying) {
        button.setImageLevel(isPlaying ? LEVEL_PLAYING : LEVEL_PAUSED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Animatable avd = (Animatable) button.getDrawable().getCurrent();
            avd.start();
        }
    }

    private void toggleControls(long actions) {
        mMasterPlayPause.setEnabled(hasFlag(actions, PlaybackStateCompat.ACTION_PLAY_PAUSE));
        mPlayPauseButton.setEnabled(hasFlag(actions, PlaybackStateCompat.ACTION_PLAY_PAUSE));
        mPreviousButton.setEnabled(hasFlag(actions, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));
        mNextButton.setEnabled(hasFlag(actions, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));
    }


    private void updateProgress() {
        if (mLastPlaybackState != null) {
            long currentPosition = mLastPlaybackState.getPosition();
            if (mLastPlaybackState.getState() == PlaybackStateCompat.STATE_PLAYING) {

                // Calculate the elapsed time between the last position update and now and unless
                // paused, we can assume (delta * speed) + current position is approximately the
                // latest position. This ensure that we do not repeatedly call the getPlaybackState()
                // on MediaControllerCompat.

                long timeDelta = SystemClock.elapsedRealtime() -
                        mLastPlaybackState.getLastPositionUpdateTime();
                currentPosition += (int) timeDelta * mLastPlaybackState.getPlaybackSpeed();
            }

            mProgress.setProgress((int) currentPosition);
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mAlbumArt = findViewById(R.id.cover);
        mTitle = findViewById(R.id.title);
        mSubtitle = findViewById(R.id.subtitle);
        mProgress = findViewById(R.id.seekbar);
        mProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mSeekPosition.setText(DateUtils.formatElapsedTime(mDurationBuilder, progress / 1000));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopProgressUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                mListener.onSeek(seekBar.getProgress());
                scheduleProgressUpdate();
            }
        });

        mSeekPosition = findViewById(R.id.seek_position);
        mSeekDuration = findViewById(R.id.seek_duration);

        mBigArt = findViewById(R.id.bigArt);

        mPlayPauseButton = findViewById(R.id.btn_play_pause);
        mPreviousButton = findViewById(R.id.btn_previous);
        mNextButton = findViewById(R.id.btn_next);
        mMasterPlayPause = findViewById(R.id.main_play_pause);

        mShuffleModeButton = findViewById(R.id.btn_shuffle);
        ColorStateList colorStateList = AppCompatResources.getColorStateList(getContext(),
                R.color.activation_state_list);
        Drawable wrapDrawable = DrawableCompat.wrap(mShuffleModeButton.getDrawable());
        DrawableCompat.setTintList(wrapDrawable, colorStateList);
        mShuffleModeButton.setImageDrawable(wrapDrawable);
        mRepeatModeButton = findViewById(R.id.btn_repeat);

        OnClickListener clickListener = new WidgetClickListener();
        mPlayPauseButton.setOnClickListener(clickListener);
        mRepeatModeButton.setOnClickListener(clickListener);
        mPreviousButton.setOnClickListener(clickListener);
        mMasterPlayPause.setOnClickListener(clickListener);
        mNextButton.setOnClickListener(clickListener);
        mShuffleModeButton.setOnClickListener(clickListener);

        ImageView miniPrevious = findViewById(R.id.btn_mini_previous);
        ImageView miniNext = findViewById(R.id.btn_mini_next);
        if (miniPrevious != null && miniNext != null) {
            miniPrevious.setOnClickListener(clickListener);
            miniNext.setOnClickListener(clickListener);
        }

        // Transitions of the top of the PlayerView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mOpenTransition = TransitionInflater.from(getContext())
                    .inflateTransition(R.transition.playerview_header_transition);
        }
    }

    private void scheduleProgressUpdate() {
        stopProgressUpdate();
        if (!mExecutorService.isShutdown()) {
            mScheduleFuture = mExecutorService.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    mHandler.post(mUpdateProgressTask);
                }
            }, PROGRESS_UPDATE_INITIAL_DELAY,
                    PROGRESS_UPDATE_PERIOD, TimeUnit.MILLISECONDS);
        }
    }

    private void stopProgressUpdate() {
        if (mScheduleFuture != null) {
            mScheduleFuture.cancel(false);
        }
    }

    public void setRepeatMode(@PlaybackStateCompat.RepeatMode int mode) {
        mRepeatModeButton.setImageLevel(mode);
        mRepeatModeButton.setActivated(mode != PlaybackStateCompat.REPEAT_MODE_ONE);
    }

    public void setShuffleMode(@PlaybackStateCompat.ShuffleMode int mode) {
        mShuffleModeButton.setActivated(mode != PlaybackStateCompat.SHUFFLE_MODE_NONE);
    }

    public void setEventListener(EventListener listener) {
        mListener = listener;
    }

    public interface EventListener {
        void onActionPlay();

        void onActionPause();

        void onSeek(long position);

        void onSkipToPrevious();

        void onSkipToNext();

        void onRepeatModeChanged(int newMode);

        void onShuffleModeChanged(int newMode);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.lastPlaybackState = mLastPlaybackState;
        savedState.metadata = mMetadata;
        savedState.repeatMode = mRepeatMode;
        savedState.expanded = mExpanded;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mLastPlaybackState = savedState.lastPlaybackState;
        mMetadata = savedState.metadata;
        mRepeatMode = savedState.repeatMode;
        mExpanded = savedState.expanded;
    }

    private class WidgetClickListener implements OnClickListener {
        private void handlePlayPauseClick() {
            int currentState = mLastPlaybackState.getState();
            if (currentState == PlaybackStateCompat.STATE_PLAYING) {
                mListener.onActionPause();
            } else {
                mListener.onActionPlay();
            }
        }

        @Override
        public void onClick(View view) {
            if (mListener != null) {
                switch (view.getId()) {
                    case R.id.main_play_pause:
                    case R.id.btn_play_pause:
                        handlePlayPauseClick();
                        break;
                    case R.id.btn_previous:
                    case R.id.btn_mini_previous:
                        mListener.onSkipToPrevious();
                        break;
                    case R.id.btn_next:
                    case R.id.btn_mini_next:
                        mListener.onSkipToNext();
                        break;
                    case R.id.btn_shuffle:
                        mListener.onShuffleModeChanged(mShuffleModeButton.isActivated()
                                ? PlaybackStateCompat.SHUFFLE_MODE_NONE
                                : PlaybackStateCompat.SHUFFLE_MODE_ALL);
                        break;
                    case R.id.btn_repeat:
                        mRepeatMode = (mRepeatMode + 1) % 3;
                        mListener.onRepeatModeChanged(mRepeatMode);
                        break;
                }

            }
        }
    }

    private void reset() {
        mAlbumArt.setImageDrawable(null);
        mBigArt.setImageDrawable(null);
        mTitle.setText(null);
        mSubtitle.setText(null);

        mProgress.setProgress(0);
        mProgress.setMax(0);
    }

    private static class SavedState extends BaseSavedState {
        PlaybackStateCompat lastPlaybackState;
        MediaMetadataCompat metadata;
        int repeatMode;
        boolean expanded;

        SavedState(Parcelable superState) {
            super(superState);
        }

        SavedState(Parcel in) {
            super(in);
            Log.d(TAG, "Restoring from parcel");
            lastPlaybackState = in.readParcelable(PlaybackStateCompat.class.getClassLoader());
            metadata = in.readParcelable(MediaMetadataCompat.class.getClassLoader());
            repeatMode = in.readInt();
            expanded = in.readInt() != 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeParcelable(lastPlaybackState, flags);
            out.writeParcelable(metadata, flags);
            out.writeInt(repeatMode);
            out.writeInt(expanded ? 1 : 0);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
