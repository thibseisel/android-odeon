package fr.nihilus.mymusic.library;

import android.app.Application;
import android.content.ComponentName;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.ConnectionCallback;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import javax.inject.Inject;

import fr.nihilus.mymusic.di.ActivityScope;
import fr.nihilus.mymusic.service.MusicService;
import io.reactivex.Observable;
import io.reactivex.subjects.BehaviorSubject;
import io.reactivex.subjects.Subject;

import static android.content.ContentValues.TAG;

@ActivityScope
public class MediaBrowserConnection {
    private static final boolean LOG = true;
    private MediaBrowserCompat mBrowser;
    private Subject<MediaControllerCompat> mControllerSubject;

    @Inject
    public MediaBrowserConnection(@NonNull final Application app) {
        mControllerSubject = BehaviorSubject.create();

        ComponentName musicServiceComponent = new ComponentName(app, MusicService.class);
        mBrowser = new MediaBrowserCompat(app, musicServiceComponent, new ConnectionCallback() {
            @Override
            public void onConnected() {
                try {
                    if (LOG) Log.d(TAG, "onConnected: mediaBrowser is now connected.");
                    MediaSessionCompat.Token token = mBrowser.getSessionToken();
                    MediaControllerCompat controller = new MediaControllerCompat(app, token);
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
