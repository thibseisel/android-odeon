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
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaControllerCompat.Callback;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;

import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.view.AutoUpdateSeekBar.OnUpdateListener;

@CoordinatorLayout.DefaultBehavior(BottomSheetBehavior.class)
public class PlayerView extends RelativeLayout implements View.OnClickListener, OnUpdateListener {

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
        mPlayPauseButton = (ImageView) findViewById(R.id.btn_play_pause);
        mPlayPauseButton.setOnClickListener(this);
        mProgress = (AutoUpdateSeekBar) findViewById(R.id.progress);
        mProgress.setOnUpdateListener(this);
        mProgress.setOnSeekBarChangeListener(mSeekListener);
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
        }
    }

    private void updatePlaybackState(@NonNull PlaybackStateCompat newState) {
        boolean hasChanged = (mLastPlaybackState == null)
                || (mLastPlaybackState.getState() != newState.getState());
        Log.d(TAG, "updatePlaybackState: hasChanged=[" + hasChanged + "]");

        mIsPlaying = newState.getState() == PlaybackStateCompat.STATE_PLAYING;
        mLastPlaybackState = newState;
        onUpdate(mProgress);
        if (hasChanged) {
            togglePlayPauseButton(mIsPlaying);
            if (mIsPlaying) mProgress.startUpdate();
            else mProgress.stopUpdate();
        }
    }

    private void seekTo(int position) {
        if (mController != null) {
            mController.getTransportControls().seekTo(position);
        }
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_play_pause) {
            if (mController != null) {
                if (mIsPlaying) {
                    mController.getTransportControls().pause();
                } else mController.getTransportControls().play();
            }
        }
    }

    private void togglePlayPauseButton(boolean isPlaying) {
        mPlayPauseButton.setImageLevel(isPlaying ? LEVEL_PLAYING : LEVEL_PAUSED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Animatable avd = (Animatable) mPlayPauseButton.getDrawable().getCurrent();
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
}
