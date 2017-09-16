package fr.nihilus.music.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.AppCompatSeekBar;
import android.util.AttributeSet;

import fr.nihilus.music.R;

/**
 * A SeekBar updating its value every second.
 */
public class AutoUpdateSeekBar extends AppCompatSeekBar {

    private static final int DEFAULT_DELAY = 1000;

    private boolean mIsRunning;
    private OnUpdateListener mListener;
    private int mDelay;

    private final Runnable mPostUpdate = new Runnable() {
        @Override
        public void run() {
            if (mListener != null) {
                mListener.onUpdate(AutoUpdateSeekBar.this);
            }
            if (mIsRunning) {
                postDelayed(mPostUpdate, mDelay);
            }
        }
    };

    public AutoUpdateSeekBar(Context context) {
        this(context, null, 0);
    }

    public AutoUpdateSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoUpdateSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.AutoUpdateSeekBar, defStyleAttr, 0);
        try {
            mDelay = a.getInt(R.styleable.AutoUpdateSeekBar_updateDelay, DEFAULT_DELAY);
        } finally {
            a.recycle();
        }
    }

    public void startUpdate() {
        mIsRunning = true;
        postDelayed(mPostUpdate, mDelay);
    }

    public void stopUpdate() {
        mIsRunning = false;
        removeCallbacks(mPostUpdate);
    }

    public void setOnUpdateListener(OnUpdateListener listener) {
        mListener = listener;
    }

    public interface OnUpdateListener {
        void onUpdate(AutoUpdateSeekBar view);
    }
}
