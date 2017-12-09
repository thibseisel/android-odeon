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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.content.res.AppCompatResources;
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

import fr.nihilus.music.R;
import fr.nihilus.music.glide.GlideApp;
import fr.nihilus.music.utils.ViewUtils;
import kotlin.Unit;

public class PlayerView extends ConstraintLayout {

    private static final String TAG = "PlayerView";

    private RequestBuilder<Bitmap> mGlideRequest;
    private TextView mTitle;
    private TextView mSubtitle;
    private ImageView mAlbumArt;
    private SeekBar mProgress;
    private PlayPauseButton mPlayPauseButton;
    private ImageView mPreviousButton;
    private ImageView mNextButton;
    private PlayPauseButton mMasterPlayPause;
    private ImageView mBigArt;
    private ImageView mShuffleModeButton;
    private ImageView mRepeatModeButton;

    private EventListener mListener;

    private boolean mExpanded = false;
    private int mRepeatMode = PlaybackStateCompat.REPEAT_MODE_NONE;
    private PlaybackStateCompat mLastPlaybackState;
    private MediaMetadataCompat mMetadata;
    private Transition mOpenTransition;

    private ProgressAutoUpdater mAutoUpdater;

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

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        // Retrieve reference to views from top
        mAlbumArt = findViewById(R.id.cover);
        mTitle = findViewById(R.id.title);
        mSubtitle = findViewById(R.id.subtitle);
        mProgress = findViewById(R.id.seekbar);

        // Configure playback position auto-updater
        final TextView seekPosition = findViewById(R.id.seek_position);
        final TextView seekDuration = findViewById(R.id.seek_duration);

        mAutoUpdater = new ProgressAutoUpdater(mProgress, seekPosition, seekDuration, (position) -> {
            if (mListener != null) {
                mListener.onSeek(position);
            }
            return Unit.INSTANCE;
        });

        mProgress.setOnSeekBarChangeListener(mAutoUpdater);

        mBigArt = findViewById(R.id.bigArt);

        mPlayPauseButton = findViewById(R.id.btn_play_pause);
        mPreviousButton = findViewById(R.id.btn_previous);
        mNextButton = findViewById(R.id.btn_next);
        mMasterPlayPause = findViewById(R.id.main_play_pause);
        mShuffleModeButton = findViewById(R.id.btn_shuffle);
        mRepeatModeButton = findViewById(R.id.btn_repeat);

        // Change color when shuffle mode and repeat mode buttons are activated
        ColorStateList activationStateList = AppCompatResources.getColorStateList(getContext(),
                R.color.activation_state_list);

        Drawable shuffleDrawable = DrawableCompat.wrap(mShuffleModeButton.getDrawable());
        DrawableCompat.setTintList(shuffleDrawable, activationStateList);
        mShuffleModeButton.setImageDrawable(shuffleDrawable);

        Drawable repeatDrawable = DrawableCompat.wrap(mRepeatModeButton.getDrawable());
        DrawableCompat.setTintList(repeatDrawable, activationStateList);
        mRepeatModeButton.setImageDrawable(repeatDrawable);

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
            if (expanded) onOpen();
            else onClose();
            mExpanded = expanded;
        }
    }

    /**
     * Updates the track's metadata currently represented by this PlayerView.
     * @param metadata the currently focused track metadata.
     */
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
            mAutoUpdater.setMetadata(metadata);
        }
    }

    /**
     * Updates the playback state currently represented by this PlayerView.
     * Playback state describes what actions are available.
     *
     * @param newState The last playback state.
     */
    public void updatePlaybackState(@Nullable PlaybackStateCompat newState) {
        if (newState != null) {
            mLastPlaybackState = newState;
            toggleControls(newState.getActions());
            mAutoUpdater.setPlaybackState(newState);

            boolean isPlaying = newState.getState() == PlaybackStateCompat.STATE_PLAYING;
            mPlayPauseButton.setPlaying(isPlaying);
            mMasterPlayPause.setPlaying(isPlaying);

        } else {

            mAlbumArt.setImageDrawable(null);
            mBigArt.setImageDrawable(null);
            mTitle.setText(null);
            mSubtitle.setText(null);

            mProgress.setProgress(0);
            mProgress.setMax(0);
        }
    }

    /**
     * Enables or disables actions depending on parameters provided by the current playback state.
     * If an action is disabled, its associated view is also disabled and does not react to clicks.
     *
     * @param actions A set of flags describing what actions are available on this media session.
     */
    private void toggleControls(long actions) {
        mMasterPlayPause.setEnabled((actions & PlaybackStateCompat.ACTION_PLAY_PAUSE) != 0);
        mPlayPauseButton.setEnabled((actions & PlaybackStateCompat.ACTION_PLAY_PAUSE) != 0);
        mPreviousButton.setEnabled((actions & PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0);
        mNextButton.setEnabled((actions & PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0);
        mRepeatModeButton.setEnabled((actions & PlaybackStateCompat.ACTION_SET_REPEAT_MODE) != 0);
        mShuffleModeButton.setEnabled((actions & PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE) != 0);
    }

    private void onOpen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TransitionManager.beginDelayedTransition(this, mOpenTransition);
        }

        int eightDps = ViewUtils.dipToPixels(getContext(), 8);
        mAlbumArt.setPadding(eightDps, eightDps, eightDps, eightDps);
        mPlayPauseButton.setVisibility(View.GONE);
    }

    private void onClose() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            TransitionManager.beginDelayedTransition(this, mOpenTransition);
        }

        mAlbumArt.setPadding(0, 0, 0, 0);
        mPlayPauseButton.setVisibility(View.VISIBLE);
    }

    /**
     * Updates display of the shuffle mode of this PlayerView.
     * For requests to change the shuffle mode to be accurate, this value should always
     * be in sync with the media session's.
     *
     * @param mode The current shuffle mode for this media session.
     */
    public void setRepeatMode(@PlaybackStateCompat.RepeatMode int mode) {
        mRepeatModeButton.setImageLevel(mode);
        mRepeatModeButton.setActivated(mode != PlaybackStateCompat.REPEAT_MODE_NONE);
    }

    /**
     * Updates display of the repeat mode of this PlayerView.
     * For requests to change the repeat mode to be accurate, this value should always
     * be in sync with the media session's.
     *
     * @param mode The current repeat mode for this media session.
     */
    public void setShuffleMode(@PlaybackStateCompat.ShuffleMode int mode) {
        mShuffleModeButton.setActivated(mode != PlaybackStateCompat.SHUFFLE_MODE_NONE);
    }

    /**
     * Listens for events triggered by interactions with this PlayerView.
     */
    public void setEventListener(EventListener listener) {
        mListener = listener;
    }

    /**
     * Listens for events triggered by user interactions with the PlayerView.
     */
    public interface EventListener {

        /**
         * Called to handle a request to start or resume playback of the currently selected track.
         */
        void onActionPlay();

        /**
         * Called to handle a request to pause playback of the currently selected track.
         */
        void onActionPause();

        /**
         * Called when user moved the progress cursor to a new position.
         * @param position The new position of the progress cursor
         */
        void onSeek(long position);

        /**
         * Called to handle a request to move to the previous track in playlist.
         */
        void onSkipToPrevious();

        /**
         * Called to handle a request to move to the next track in playlist.
         */
        void onSkipToNext();

        /**
         * Called to handle a request to change the current repeat mode.
         * @param newMode The new repeat mode.
         */
        void onRepeatModeChanged(@PlaybackStateCompat.RepeatMode int newMode);
        /**
         * Called to handle a request to change the current shuffle mode.
         * @param newMode The new shuffle mode.
         */
        void onShuffleModeChanged(@PlaybackStateCompat.ShuffleMode int newMode);

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

    /**
     * A parcelable object that saves the internal state of a PlayerView.
     */
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
