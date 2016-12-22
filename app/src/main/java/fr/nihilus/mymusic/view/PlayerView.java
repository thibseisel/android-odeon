package fr.nihilus.mymusic.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaControllerCompat.Callback;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;

import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.utils.ViewUtils;
import fr.nihilus.mymusic.view.AutoUpdateSeekBar.OnUpdateListener;

@CoordinatorLayout.DefaultBehavior(BottomSheetBehavior.class)
public class PlayerView extends PercentRelativeLayout implements View.OnClickListener, OnUpdateListener {

    private static final String TAG = "PlayerView";

    private static final int LEVEL_PLAYING = 1;
    private static final int LEVEL_PAUSED = 0;

    @Nullable
    private MediaControllerCompat mController;
    private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {

        @Override
        public void onProgressChanged(SeekBar view, int progress, boolean fromUser) {
            if (fromUser) seekTo(progress);
        }

        @Override
        public void onStartTrackingTouch(SeekBar view) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar view) {
        }
    };
    private BitmapRequestBuilder<Uri, Bitmap> mGlideRequest;
    private PlaybackStateCompat mLastPlaybackState;
    private TextView mTitle;
    private TextView mArtist;
    private ImageView mAlbumArt;
    private AutoUpdateSeekBar mProgress;
    private boolean mIsPlaying;
    private ImageView mPlayPauseButton;
    private ImageView mPreviousButton;
    private ImageView mNextButton;
    private ImageView mMasterPlayPause;

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

    public PlayerView(Context context) {
        this(context, null, 0);
    }

    public PlayerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PlayerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        View.inflate(context, R.layout.view_player, this);

        // Make this view apear above AppbarLayout
        ViewCompat.setElevation(this, ViewUtils.dipToPixels(context, 4));
        // Prevent from dispatching touches to the view behind
        setClickable(true);

        if (isInEditMode()) {
            return;
        }

        Drawable dummyAlbumArt = ContextCompat.getDrawable(context, R.drawable.dummy_album_art);
        mGlideRequest = Glide.with(context).fromUri()
                .asBitmap()
                .fitCenter()
                .error(dummyAlbumArt);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAlbumArt = (ImageView) findViewById(R.id.albumArt);
        mTitle = (TextView) findViewById(R.id.title);
        mArtist = (TextView) findViewById(R.id.subtitle);
        mProgress = (AutoUpdateSeekBar) findViewById(R.id.progress);
        mProgress.setOnUpdateListener(this);
        mProgress.setOnSeekBarChangeListener(mSeekListener);

        mPlayPauseButton = (ImageView) findViewById(R.id.btn_play_pause);
        mPlayPauseButton.setOnClickListener(this);
        mPreviousButton = (ImageView) findViewById(R.id.btn_previous);
        mPreviousButton.setOnClickListener(this);
        mNextButton = (ImageView) findViewById(R.id.btn_next);
        mNextButton.setOnClickListener(this);
        mMasterPlayPause = (ImageView) findViewById(R.id.main_play_pause);
        mMasterPlayPause.setOnClickListener(this);
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        // Do not dispatch pressed event to View children
    }

    public void attachMediaController(@Nullable MediaControllerCompat controller) {
        Log.d(TAG, "attachMediaController() called with: controller = [" + controller + "]");
        if (controller != null) {
            controller.registerCallback(mControllerCallback);
            updateMetadata(controller.getMetadata());
            updatePlaybackState(controller.getPlaybackState());
        } else if (mController != null) {
            mController.unregisterCallback(mControllerCallback);
        }
        mController = controller;
    }

    private void updateMetadata(MediaMetadataCompat metadata) {
        if (metadata != null) {
            MediaDescriptionCompat media = metadata.getDescription();
            mTitle.setText(media.getTitle());
            mArtist.setText(media.getSubtitle());
            mGlideRequest.load(media.getIconUri()).into(mAlbumArt);
            mProgress.setMax((int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION));
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
        mPreviousButton.setEnabled(hasFlag(actions, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));
        mNextButton.setEnabled(hasFlag(actions, PlaybackStateCompat.ACTION_SKIP_TO_NEXT));
    }

    @Override
    public void onClick(View view) {
        if (mController != null) {
            switch (view.getId()) {
                case R.id.main_play_pause:
                case R.id.btn_play_pause:
                    if (mIsPlaying) {
                        mController.getTransportControls().pause();
                    } else mController.getTransportControls().play();
                    break;
                case R.id.btn_previous:
                    mController.getTransportControls().skipToPrevious();
                    break;
                case R.id.btn_next:
                    mController.getTransportControls().skipToNext();
                    break;
            }
        }
    }

    private static boolean hasFlag(long actions, long flag) {
        return (actions & flag) == flag;
    }
}
