package fr.nihilus.music.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaControllerCompat.Callback;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.content.res.AppCompatResources;
import android.transition.Transition;
import android.transition.TransitionInflater;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import fr.nihilus.music.R;
import fr.nihilus.music.glide.GlideApp;
import fr.nihilus.music.utils.ViewUtils;
import fr.nihilus.music.view.AutoUpdateSeekBar.OnUpdateListener;

@CoordinatorLayout.DefaultBehavior(BottomSheetBehavior.class)
public class PlayerView extends ConstraintLayout implements View.OnClickListener, OnUpdateListener {

    private static final String TAG = "PlayerView";

    private static final int LEVEL_PLAYING = 1;
    private static final int LEVEL_PAUSED = 0;
    private RequestBuilder<Bitmap> mGlideRequest;
    private TextView mTitle;
    private TextView mArtist;
    private ImageView mAlbumArt;
    private AutoUpdateSeekBar mProgress;
    private ImageView mPlayPauseButton;
    private ImageView mPreviousButton;
    private ImageView mNextButton;
    private ImageView mMasterPlayPause;
    private ImageView mBigArt;
    private ImageView mRandomButton;
    private MediaControllerCompat mController;

    private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar view, int progress, boolean fromUser) {
            if (fromUser) seekTo(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar view) {
            // Do nothing
        }

        @Override
        public void onStopTrackingTouch(SeekBar view) {
            // Do nothing
        }
    };

    private PlaybackStateCompat mLastPlaybackState;
    private boolean mIsPlaying;

    private final Callback mControllerCallback = new MediaControllerCompat.Callback() {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat newState) {
            updatePlaybackState(newState);
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata) {
            updateMetadata(metadata);
        }
    };

    private Transition mOpenTransition;

    private BottomSheetBehavior<PlayerView> mBehavior;

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
        ViewCompat.setElevation(this, ViewUtils.dipToPixels(context, 4));
        // Prevent from dispatching touches to the view behind
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

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        /* Starting from this point, PlayerView is attached to its parent.
         * Initialize BottomSheetBehavior only if the parent is a CoordinatorLayout.
         */
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params instanceof CoordinatorLayout.LayoutParams) {
            mBehavior = BottomSheetBehavior.from(this);
            mBehavior.setBottomSheetCallback(new BottomSheetCallback());
            if (mBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                onClose(false);
            } else if (mBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                onOpen(false);
            }
        }
    }

    public void setMediaController(@Nullable MediaControllerCompat controller) {
        Log.d(TAG, "setMediaController() called with: controller = [" + controller + "]");
        if (controller != null) {
            controller.registerCallback(mControllerCallback);
            updateMetadata(controller.getMetadata());
            updatePlaybackState(controller.getPlaybackState());
        } else if (mController != null) {
            mController.unregisterCallback(mControllerCallback);
        }
        mController = controller;
    }

    /**
     * Expand or collapse the PlayerView with an animation.
     * When expanded, it is drawn above the main content view.
     * When collapsed, only the top is visible.
     * If the playerView is not a direct child of CoordinatorLayout, this method will do nothing.
     * @param expanded true to expand the PlayerView, false to collapse
     */
    public void setExpanded(boolean expanded) {
        if (mBehavior != null) {
            mBehavior.setState(expanded
                    ? BottomSheetBehavior.STATE_EXPANDED
                    : BottomSheetBehavior.STATE_COLLAPSED);
        }
    }

    private void updateMetadata(MediaMetadataCompat metadata) {
        if (metadata != null) {
            MediaDescriptionCompat media = metadata.getDescription();
            mTitle.setText(media.getTitle());
            mArtist.setText(media.getSubtitle());
            mGlideRequest.load(media.getIconUri()).into(mAlbumArt);
            mGlideRequest.load(media.getIconUri()).into(mBigArt);
            int max = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
            mProgress.setMax(Math.max(max, 0));
            onUpdate(mProgress);
        }
    }

    private void updatePlaybackState(@NonNull PlaybackStateCompat newState) {
        boolean hasChanged = (mLastPlaybackState == null)
                || (mLastPlaybackState.getState() != newState.getState());
        Log.d(TAG, "updatePlaybackState: hasChanged=[" + hasChanged + "]");

        toggleControls(newState.getActions());
        mIsPlaying = newState.getState() == PlaybackStateCompat.STATE_PLAYING;
        mLastPlaybackState = newState;
        onUpdate(mProgress);
        if (hasChanged) {
            togglePlayPauseButton(mPlayPauseButton, mIsPlaying);
            togglePlayPauseButton(mMasterPlayPause, mIsPlaying);
            if (mIsPlaying) mProgress.startUpdate();
            else mProgress.stopUpdate();
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

    private void seekTo(int position) {
        if (mController != null) {
            mController.getTransportControls().seekTo(position);
        }
    }

    private void togglePlayPauseButton(ImageView button, boolean isPlaying) {
        button.setImageLevel(isPlaying ? LEVEL_PLAYING : LEVEL_PAUSED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Animatable avd = (Animatable) button.getDrawable().getCurrent();
            avd.start();
        }
    }

    @Override
    public void onUpdate(AutoUpdateSeekBar view) {
        if (mLastPlaybackState != null) {
            long currentPosition = mLastPlaybackState.getPosition();
            if (mLastPlaybackState.getState() != PlaybackStateCompat.STATE_PAUSED) {
                long timeDelda = SystemClock.elapsedRealtime() - mLastPlaybackState.getLastPositionUpdateTime();
                currentPosition += (int) timeDelda * mLastPlaybackState.getPlaybackSpeed();
            }
            view.setProgress((int) currentPosition);
        }
    }

    private void toggleControls(long actions) {
        mMasterPlayPause.setEnabled(hasFlag(actions, PlaybackStateCompat.ACTION_PLAY_PAUSE));
        mPlayPauseButton.setEnabled(hasFlag(actions, PlaybackStateCompat.ACTION_PLAY_PAUSE));
        mPreviousButton.setEnabled(hasFlag(actions, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));
        mNextButton.setEnabled(hasFlag(actions, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));

        if (mController != null) {
            mRandomButton.setActivated(
                    mController.getShuffleMode() != PlaybackStateCompat.SHUFFLE_MODE_NONE);
        }
    }

    @Override
    public void onClick(View view) {
        /*if (view.getId() == R.id.textContainer) {
            setExpanded(mBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED);
            //return;
        }*/

        if (mController != null) {
            MediaControllerCompat.TransportControls controls = mController.getTransportControls();
            switch (view.getId()) {
                case R.id.main_play_pause:
                case R.id.btn_play_pause:
                    if (mIsPlaying) {
                        controls.pause();
                    } else controls.play();
                    break;
                case R.id.btn_previous:
                    controls.skipToPrevious();
                    break;
                case R.id.btn_next:
                    controls.skipToNext();
                    break;
                case R.id.btn_random:
                    int shuffleMode = mRandomButton.isActivated()
                            ? PlaybackStateCompat.SHUFFLE_MODE_NONE
                            : PlaybackStateCompat.SHUFFLE_MODE_ALL;
                    controls.setShuffleMode(shuffleMode);
                    break;
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        mAlbumArt = findViewById(R.id.cover);
        mTitle = findViewById(R.id.title);
        mArtist = findViewById(R.id.subtitle);
        mProgress = findViewById(R.id.progress);
        mProgress.setOnUpdateListener(this);
        mProgress.setOnSeekBarChangeListener(mSeekListener);

        //findViewById(R.id.textContainer).setOnClickListener(this);

        mBigArt = findViewById(R.id.bigArt);

        mPlayPauseButton = findViewById(R.id.btn_play_pause);
        mPlayPauseButton.setOnClickListener(this);
        mPreviousButton = findViewById(R.id.btn_previous);
        mPreviousButton.setOnClickListener(this);
        mNextButton = findViewById(R.id.btn_next);
        mNextButton.setOnClickListener(this);
        mMasterPlayPause = findViewById(R.id.main_play_pause);
        mMasterPlayPause.setOnClickListener(this);

        mRandomButton = findViewById(R.id.btn_random);
        ColorStateList colorStateList = AppCompatResources.getColorStateList(getContext(),
                R.color.activation_state_list);
        Drawable wrapDrawable = DrawableCompat.wrap(mRandomButton.getDrawable());
        DrawableCompat.setTintList(wrapDrawable, colorStateList);
        mRandomButton.setImageDrawable(wrapDrawable);
        mRandomButton.setOnClickListener(this);

        // Transitions of the top of the PlayerView
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mOpenTransition = TransitionInflater.from(getContext())
                    .inflateTransition(R.transition.playerview_header_transition);
        }
    }

    /**
     * Class that contains callback to execute when this view's BottomSheet state changes.
     */
    private class BottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                onClose(true);
            } else if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                onOpen(true);
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            // Do nothing
        }
    }
}
