package fr.nihilus.mymusic;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Global executor pools for the whole application.
 * Grouping tasks like this avoid the effects of task starvation.
 */
@Singleton
public class AppExecutors {
    private final Executor mDiskIo;
    private final Executor mNetworkIo;

    @Inject
    public AppExecutors() {
        mDiskIo = Executors.newSingleThreadExecutor();
        mNetworkIo = Executors.newFixedThreadPool(3);
    }

    public Executor diskIo() {
        return mDiskIo;
    }

    public Executor networkIo() {
        return mNetworkIo;
    }
}
