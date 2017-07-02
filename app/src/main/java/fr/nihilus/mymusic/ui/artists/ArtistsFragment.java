package fr.nihilus.mymusic.ui.artists;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.library.MediaBrowserConnection;
import fr.nihilus.mymusic.utils.MediaID;

public class ArtistsFragment extends Fragment implements ArtistAdapter.OnArtistSelectedListener {

    private static final String TAG = "ArtistsFragment";
    private static final String KEY_ARTISTS = "Artists";

    private RecyclerView mRecyclerView;
    private ContentLoadingProgressBar mProgressBar;
    private View mEmptyView;
    private ArrayList<MediaItem> mArtists;
    private ArtistAdapter mAdapter;

    @Inject MediaBrowserConnection mBrowserConnection;

    private final SubscriptionCallback mCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaItem> artists) {
            Log.d(TAG, "onChildrenLoaded: loaded " + artists.size() + " artists.");
            mArtists.addAll(artists);
            mAdapter.updateArtists(artists);
            showLoading(false);
            showEmptyView(artists.isEmpty());
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

        if (savedInstanceState != null) {
            mArtists = savedInstanceState.getParcelableArrayList(KEY_ARTISTS);
        } else mArtists = new ArrayList<>();
        mAdapter = new ArtistAdapter(getContext(), mArtists);
        mAdapter.setOnArtistSelectedListener(this);
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_artists, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mProgressBar = (ContentLoadingProgressBar) view.findViewById(android.R.id.progress);
        mEmptyView = view.findViewById(android.R.id.empty);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        mRecyclerView.setAdapter(mAdapter);

        if (savedInstanceState == null) {
            showLoading(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(R.string.action_artists);
        mBrowserConnection.subscribe(MediaID.ID_ARTISTS, mCallback);
    }

    @Override
    public void onStop() {
        mBrowserConnection.unsubscribe(MediaID.ID_ARTISTS);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(KEY_ARTISTS, mArtists);
    }

    @Override
    public void onDestroyView() {
        mRecyclerView = null;
        mEmptyView = null;
        mProgressBar = null;
        super.onDestroyView();
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

    private void showLoading(boolean shown) {
        if (shown) {
            mRecyclerView.setVisibility(View.GONE);
            mProgressBar.show();
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mProgressBar.hide();
        }
    }

    private void showEmptyView(boolean shown) {
        mEmptyView.setVisibility(shown ? View.VISIBLE : View.GONE);
    }
}
