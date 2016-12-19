package fr.nihilus.mymusic;

import android.content.ComponentName;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.ConnectionCallback;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.util.Pair;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.LinkedList;
import java.util.Queue;

import fr.nihilus.mymusic.playback.MusicService;

/**
 * Un Fragment qui établit une connexion distante à {@link MusicService} pour accéder aux médias.
 * La connexion peut être partagée avec tous les {@link Fragment} associés avec la même Activité
 * que celui-ci.
 * Pour cela, il suffit de récupérer l'instance de ce Fragment avec {@link #getInstance(FragmentManager)}.
 */
public class MediaBrowserFragment extends Fragment {

    private static final String TAG = "MediaBrowserFragment";

    private MediaBrowserCompat mMediaBrowser;
    private Queue<Pair<String, WeakReference<SubscriptionCallback>>> mQueue = new LinkedList<>();
    private Queue<WeakReference<ConnectedCallback>> mConnectionQueue = new LinkedList<>();

    private final ConnectionCallback mConnectionCallback = new ConnectionCallback() {

        @Override
        public void onConnected() {
            try {
                Log.d(TAG, "MediaBrowser is now connected.");
                MediaSessionCompat.Token token = mMediaBrowser.getSessionToken();
                MediaControllerCompat controller = new MediaControllerCompat(getContext(), token);
                MediaControllerCompat.setMediaController(getActivity(), controller);
                notifyConnectedListeners();
                subscribeAfterConnection();
            } catch (RemoteException e) {
                Log.e(TAG, "subscribeAfterConnection: Failed to create MediaController.", e);
            }
        }

        @Override
        public void onConnectionSuspended() {
            Log.w(TAG, "MediaBrowser connection has been suspended.");
            mMediaBrowser = null;
        }

        @Override
        public void onConnectionFailed() {
            Log.e(TAG, "Connection to MediaBrowser has failed.");
            super.onConnectionFailed();
        }
    };

    public static MediaBrowserFragment getInstance(FragmentManager manager) {
        MediaBrowserFragment f = (MediaBrowserFragment) manager.findFragmentByTag(TAG);
        if (f == null) {
            Log.v(TAG, "New instance of MediaBrowserFragment.");
            f = new MediaBrowserFragment();
            manager.beginTransaction().add(f, TAG).commit();
        }
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMediaBrowser = new MediaBrowserCompat(getContext(), new ComponentName(getContext(),
                MusicService.class), mConnectionCallback, null);
        mMediaBrowser.connect();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Disconnecting from MediaBrowser.");
        if (mMediaBrowser != null) {
            mMediaBrowser.disconnect();
        }
        super.onDestroy();
    }

    boolean isConnected() {
        return mMediaBrowser != null && mMediaBrowser.isConnected();
    }

    public void subscribe(@NonNull String mediaId, @NonNull SubscriptionCallback callback) {
        Log.d(TAG, "subscribe: subscribing for " + mediaId);
        if (!isConnected()) {
            // Met la requête en file d'attente, pour quand le MediaBrowser sera connecté
            Log.w(TAG, "subscribe: waiting for the MediaBrowser to connect. Delaying request...");
            mQueue.add(new Pair<>(mediaId, new WeakReference<>(callback)));
            return;
        }

        mMediaBrowser.subscribe(mediaId, callback);
    }

    public void unsubscribe(@NonNull String mediaId) {
        if (mMediaBrowser != null) {
            mMediaBrowser.unsubscribe(mediaId);
        }
    }

    public void doWhenConnected(ConnectedCallback callback) {
        if (isConnected()) {
            callback.onConnected();
            return;
        }
        mConnectionQueue.add(new WeakReference<>(callback));
    }

    private void subscribeAfterConnection() {
        Pair<String, WeakReference<SubscriptionCallback>> pair;
        while ((pair = mQueue.poll()) != null) {
            final SubscriptionCallback callback = pair.second.get();
            if (callback != null) {
                Log.d(TAG, "subscribeAfterConnection: subscribing for " + pair.first);
                mMediaBrowser.subscribe(pair.first, callback);
            }
        }
    }

    private void notifyConnectedListeners() {
        WeakReference<ConnectedCallback> callbackRef;
        while ((callbackRef = mConnectionQueue.poll()) != null) {
            final ConnectedCallback callback = callbackRef.get();
            if (callback != null) {
                callback.onConnected();
            }
        }
    }

    public interface ConnectedCallback {
        void onConnected();
    }
}
