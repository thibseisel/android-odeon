package fr.nihilus.mymusic.view;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.drawable.Animatable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaControllerCompat.Callback;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.content.res.AppCompatResources;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.service.MusicService;
import fr.nihilus.mymusic.utils.ViewUtils;
import fr.nihilus.mymusic.view.AutoUpdateSeekBar.OnUpdateListener;

@CoordinatorLayout.DefaultBehavior(BottomSheetBehavior.class)
public class PlayerView extends PercentRelativeLayout implements View.OnClickListener, OnUpdateListener {

    private static final String TAG = "PlayerView";

    private static final int LEVEL_PLAYING = 1;
    private static final int LEVEL_PAUSED = 0;

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

    private MediaControllerCompat mController;
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
    private ImageView mBigArt;
    private ImageView mRandomButton;

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

        Drawable dummyAlbumArt = AppCompatResources.getDrawable(context, R.drawable.ic_audiotrack_24dp);
        mGlideRequest = Glide.with(context).fromUri()
                .asBitmap()
                .fitCenter()
                .error(dummyAlbumArt)
                .diskCacheStrategy(DiskCacheStrategy.NONE);
    }

    private static boolean hasFlag(long actions, long flag) {
        return (actions & flag) == flag;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mAlbumArt = (ImageView) findViewById(R.id.cover);
        mTitle = (TextView) findViewById(R.id.title);
        mArtist = (TextView) findViewById(R.id.subtitle);
        mProgress = (AutoUpdateSeekBar) findViewById(R.id.progress);
        mProgress.setOnUpdateListener(this);
        mProgress.setOnSeekBarChangeListener(mSeekListener);

        findViewById(R.id.textContainer).setOnClickListener(this);

        mBigArt = (ImageView) findViewById(R.id.bigArt);

        mPlayPauseButton = (ImageView) findViewById(R.id.btn_play_pause);
        mPlayPauseButton.setOnClickListener(this);
        mPreviousButton = (ImageView) findViewById(R.id.btn_previous);
        mPreviousButton.setOnClickListener(this);
        mNextButton = (ImageView) findViewById(R.id.btn_next);
        mNextButton.setOnClickListener(this);
        mMasterPlayPause = (ImageView) findViewById(R.id.main_play_pause);
        mMasterPlayPause.setOnClickListener(this);

        // Random button
        mRandomButton = (ImageView) findViewById(R.id.btn_random);
        ColorStateList colorStateList = AppCompatResources.getColorStateList(getContext(),
                R.color.activation_state_list);
        Drawable wrapDrawable = DrawableCompat.wrap(mRandomButton.getDrawable());
        DrawableCompat.setTintList(wrapDrawable, colorStateList);
        mRandomButton.setImageDrawable(wrapDrawable);
        mRandomButton.setOnClickListener(this);
    }

    @Override
    protected void dispatchSetPressed(boolean pressed) {
        // Do not dispatch pressed event to View children
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

    private void updateMetadata(MediaMetadataCompat metadata) {
        if (metadata != null) {
            MediaDescriptionCompat media = metadata.getDescription();
            mTitle.setText(media.getTitle());
            mArtist.setText(media.getSubtitle());
            mGlideRequest.load(media.getIconUri()).into(mAlbumArt);
            mGlideRequest.load(media.getIconUri()).into(mBigArt);
            int max = (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION);
            mProgress.setMax(max > 0 ? max : 0);
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

    public void setHeaderOpacity(float opacity) {
        mPlayPauseButton.setAlpha(opacity);
        mAlbumArt.setAlpha(opacity);
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
        mRandomButton.setActivated(hasFlag(actions, MusicService.ACTION_RANDOM));
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.textContainer) {
            BottomSheetBehavior.from(this).setState(BottomSheetBehavior.STATE_EXPANDED);
            return;
        }

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
                    Bundle args = new Bundle(1);
                    args.putBoolean(MusicService.EXTRA_RANDOM_ENABLED, !mRandomButton.isActivated());
                    controls.sendCustomAction(MusicService.CUSTOM_ACTION_RANDOM, args);
                    break;
            }
        }
    }
}
