package fr.nihilus.music.library;

import android.content.ComponentName;
import android.content.Context;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.ConnectionCallback;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import javax.inject.Inject;

import fr.nihilus.music.di.ActivityScoped;
import fr.nihilus.music.service.MusicService;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

import static android.content.ContentValues.TAG;

/**
 * An object that manages a connection between UI components and the {@link MusicService}.
 */
@ActivityScoped
public class MediaBrowserConnection {
    private static final boolean LOG = true;
    private MediaBrowserCompat mBrowser;
    private final Subject<MediaControllerCompat> mControllerSubject;

    @Inject
    public MediaBrowserConnection(final Context context) {
        mControllerSubject = BehaviorSubject.create();

        ComponentName musicServiceComponent = new ComponentName(context, MusicService.class);
        mBrowser = new MediaBrowserCompat(context, musicServiceComponent, new ConnectionCallback() {
            @Override
            public void onConnected() {
                try {
                    if (LOG) Log.d(TAG, "onConnected: mediaBrowser is now connected.");
                    MediaSessionCompat.Token token = mBrowser.getSessionToken();
                    MediaControllerCompat controller = new MediaControllerCompat(context, token);
                    mControllerSubject.onNext(controller);
                } catch (RemoteException rex) {
                    Log.e(TAG, "onConnected: failed to create MediaController", rex);
                }
            }

            @Override
            public void onConnectionSuspended() {
                if (LOG) Log.d(TAG, "onConnectionSuspended() called");
            }

            @Override
            public void onConnectionFailed() {
                Log.e(TAG, "onConnectionFailed: cannot connect to MediaBrowser");
            }
        }, null);
    }

    public Observable<MediaControllerCompat> getMediaController() {
        return mControllerSubject;
    }

    public void connect() {
        if (!mBrowser.isConnected()) {
            mBrowser.connect();
        }
    }

    public void release() {
        mBrowser.disconnect();
    }

    public void subscribe(@NonNull String mediaId, @NonNull SubscriptionCallback callback) {
        mBrowser.subscribe(mediaId, callback);
    }

    public void unsubscribe(@NonNull String mediaId) {
        mBrowser.unsubscribe(mediaId);
    }
}
