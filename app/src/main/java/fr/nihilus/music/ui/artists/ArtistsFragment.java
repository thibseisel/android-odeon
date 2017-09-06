package fr.nihilus.music.ui.artists;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import fr.nihilus.music.R;
import fr.nihilus.music.di.ActivityScoped;
import fr.nihilus.music.library.MediaBrowserConnection;
import fr.nihilus.music.utils.MediaID;
import fr.nihilus.recyclerfragment.RecyclerFragment;

@ActivityScoped
public class ArtistsFragment extends RecyclerFragment implements ArtistAdapter.OnArtistSelectedListener {

    private static final String TAG = "ArtistsFragment";

    private ArtistAdapter mAdapter;

    @Inject MediaBrowserConnection mBrowserConnection;

    private final SubscriptionCallback mCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaItem> artists) {
            Log.d(TAG, "onChildrenLoaded: loaded " + artists.size() + " artists.");
            mAdapter.updateArtists(artists);
            setRecyclerShown(true);
        }
    };

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new ArtistAdapter(getContext(), new ArrayList<MediaItem>());
        mAdapter.setOnArtistSelectedListener(this);
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_artists, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(R.string.action_artists);
        mBrowserConnection.subscribe(MediaID.ID_ARTISTS, mCallback);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setAdapter(mAdapter);
        if (savedInstanceState == null) {
            setRecyclerShown(false);
        }
    }

    @Override
    public void onStop() {
        mBrowserConnection.unsubscribe(MediaID.ID_ARTISTS);
        super.onStop();
    }

    @Override
    public void onArtistSelected(ArtistAdapter.ArtistHolder holder, MediaItem artist) {
        Fragment detail = ArtistDetailFragment.newInstance(artist);
        getFragmentManager().beginTransaction()
                .replace(R.id.container, detail)
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .addToBackStack(ArtistDetailFragment.BACKSTACK_ENTRY)
                .commit();
    }
}
