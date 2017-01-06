package fr.nihilus.mymusic.provider;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;

import static fr.nihilus.mymusic.provider.MusicStatsProvider.CONTENT_URI;
import static fr.nihilus.mymusic.provider.MusicStatsProvider.KEY_FIELD;
import static fr.nihilus.mymusic.provider.MusicStatsProvider.KEY_MUSIC_ID;
import static fr.nihilus.mymusic.provider.MusicStatsProvider.METHOD_INCREMENT;

public class StatsRecorder {

    private static final int MESSAGE_COMPLETION = 1;
    private static final int MESSAGE_SKIP = 2;

    private final Handler.Callback mHandlerCallback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_COMPLETION:
                    Bundle completionArgs = new Bundle(2);
                    completionArgs.putLong(KEY_MUSIC_ID, (long) msg.arg1);
                    completionArgs.putString(KEY_FIELD, (String) msg.obj);
                    mResolver.call(CONTENT_URI, METHOD_INCREMENT, null, completionArgs);
                    return true;
                case MESSAGE_SKIP:
                    Bundle skipArgs = new Bundle(2);
                    skipArgs.putLong(KEY_MUSIC_ID, (long) msg.arg1);
                    skipArgs.putString(KEY_FIELD, (String) msg.obj);
                    mResolver.call(CONTENT_URI, METHOD_INCREMENT, null, skipArgs);
                    return true;
                default:
                    return false;
            }
        }
    };

    private final ContentResolver mResolver;
    private HandlerThread mThread;
    private Handler mHandler;

    public StatsRecorder(@NonNull Context context) {
        mResolver = context.getContentResolver();
        mThread = new HandlerThread(StatsRecorder.class.getSimpleName());
    }

    public void start() {
        mThread.start();
        mHandler = new Handler(mThread.getLooper(), mHandlerCallback);
    }

    public void stop() {
        mHandler.removeCallbacksAndMessages(null);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            mThread.quitSafely();
        } else mThread.quit();
    }

    /**
     *
     * @param musicId
     */
    public void recordCompletion(long musicId) {
        mHandler.obtainMessage(MESSAGE_COMPLETION, (int) musicId,
                1, MusicStats.READ_COUNT).sendToTarget();
    }

    public void recordSkip(long musicId) {
        mHandler.obtainMessage(MESSAGE_SKIP, (int) musicId,
                1, MusicStats.SKIP_COUNT).sendToTarget();
    }
}
