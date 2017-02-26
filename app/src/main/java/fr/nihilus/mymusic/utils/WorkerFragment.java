package fr.nihilus.mymusic.utils;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

/**
 * This class ensures that an AsyncTask will complete even if the activity is destroyed due to configuration change.
 * Please refer to the sample
 * <a href="https://github.com/android/platform_frameworks_support/blob/master/samples/Support4Demos/src/com/example/android/supportv4/app/FragmentRetainInstanceSupport.java">here</a>
 * for more informations.
 * @param <Param>
 * @param <Progress>
 * @param <Result>
 */
public class WorkerFragment<Param, Progress, Result> extends Fragment {

    private AsyncTask<Param, Progress, Result> mTask;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void setTask(AsyncTask<Param, Progress, Result> task) {
        mTask = task;
    }

    @SafeVarargs
    public final void execute(Param... params) {
        mTask.execute(params);
    }
}
