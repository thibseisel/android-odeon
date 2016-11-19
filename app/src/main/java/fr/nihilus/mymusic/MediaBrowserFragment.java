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
import android.util.Log;

import fr.nihilus.mymusic.playback.MusicService;

/**
 * Un Fragment qui établit une connexion distante à {@link MusicService} pour accéder aux médias.
 * La connexion peut être partagée avec tous les {@link Fragment} associés avec la même Activité
 * que celui-ci.
 * Pour cela, il suffit de récupérer l'instance de ce Fragment avec {@link #getInstance(FragmentManager)}.
 */
public class MediaBrowserFragment extends Fragment {

    private static final String TAG = "RetainedConnFragment";

    private MediaBrowserCompat mMediaBrowser;

    private final ConnectionCallback mConnectionCallback = new ConnectionCallback() {

        @Override
        public void onConnected() {
            try {
                Log.d(TAG, "onConnected() called");
                MediaSessionCompat.Token token = mMediaBrowser.getSessionToken();
                MediaControllerCompat controller = new MediaControllerCompat(getContext(), token);
                getActivity().setSupportMediaController(controller);
            } catch (RemoteException e) {
                Log.e(TAG, "onMediaBrowserConnected: Failed to create MediaController.", e);
            }
        }

        @Override
        public void onConnectionSuspended() {
            Log.d(TAG, "onConnectionSuspended() called");
            mMediaBrowser = null;
        }

        @Override
        public void onConnectionFailed() {
            Log.e(TAG, "onConnectionFailed() called");
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

        Log.d(TAG, "onCreate: creating and starting MediaBrowser...");
        mMediaBrowser = new MediaBrowserCompat(getContext(), new ComponentName(getContext(),
                MusicService.class), mConnectionCallback, null);
        mMediaBrowser.connect();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy() called");
        mMediaBrowser.disconnect();
        super.onDestroy();
    }

    public boolean isConnected() {
        return mMediaBrowser != null && mMediaBrowser.isConnected();
    }

    public void subscribe(@NonNull String mediaId, @NonNull SubscriptionCallback callback) {
        Log.d(TAG, "subscribe() called");
        mMediaBrowser.subscribe(mediaId, callback);
    }

    public void unsubscribe(@NonNull String mediaId) {
        if (mMediaBrowser != null) {
            mMediaBrowser.unsubscribe(mediaId);
        }
    }
}
