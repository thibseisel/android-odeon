package fr.nihilus.music.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.SeekBar;

/**
 * SeekBar that can be used with a {@link MediaSessionCompat} to track and seek in playing
 * media.
 */
public class MediaSeekBar extends AppCompatSeekBar implements ValueAnimator.AnimatorUpdateListener {

    private OnSeekListener mSeekListener;
    private boolean mIsTracking = false;
    private OnSeekBarChangeListener mOnSeekBarChangeListener = new OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mIsTracking = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (mSeekListener != null) {
                mSeekListener.onSeek(getProgress());
            }

            mIsTracking = false;
        }
    };

    private ValueAnimator mProgressAnimator;

    public MediaSeekBar(Context context) {
        super(context);
        super.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
    }

    public MediaSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        super.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
    }

    public MediaSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setOnSeekBarChangeListener(mOnSeekBarChangeListener);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        // Prohibit adding seek listeners to this subclass.
        throw new UnsupportedOperationException();
    }

    public void setOnSeekListener(@Nullable OnSeekListener listener) {
        mSeekListener = listener;
    }

    public void setMetadata(@Nullable MediaMetadataCompat metadata) {
        final int max = metadata != null
                ? (int) metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION)
                : 0;
        setProgress(0);
        setMax(max);
    }

    public void setPlaybackState(@Nullable PlaybackStateCompat state) {
        // If there's an ongoing animation, stop it now.
        if (mProgressAnimator != null) {
            mProgressAnimator.cancel();
            mProgressAnimator = null;
        }

        final int progress = state != null
                ? (int) state.getPosition()
                : 0;
        setProgress(progress);

        // If the media is playing then the seekbar should follow it, and the easiest
        // way to do that is to create a ValueAnimator to update it so the bar reaches
        // the end of the media the same time as playback gets there (or close enough).
        if (state != null && state.getState() == PlaybackStateCompat.STATE_PLAYING) {
            final int timeToEnd = (int) ((getMax() - progress) / state.getPlaybackSpeed());

            mProgressAnimator = ValueAnimator.ofInt(progress, getMax())
                    .setDuration(timeToEnd);
            mProgressAnimator.setInterpolator(new LinearInterpolator());
            mProgressAnimator.addUpdateListener(this);
            mProgressAnimator.start();
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator valueAnimator) {
        // If the user is changing the slider, cancel the animation.
        if (mIsTracking) {
            valueAnimator.cancel();
            return;
        }

        final int animatedIntValue = (int) valueAnimator.getAnimatedValue();
        setProgress(animatedIntValue);
    }

    public interface OnSeekListener {
        void onSeek(int newPosition);
    }
}
