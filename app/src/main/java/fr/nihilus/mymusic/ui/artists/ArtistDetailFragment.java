package fr.nihilus.mymusic.ui.artists;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

import fr.nihilus.mymusic.MediaBrowserFragment;
import fr.nihilus.mymusic.R;

public class ArtistDetailFragment extends Fragment {

    private static final String TAG = "ArtistDetailFragment";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_ARTIST_DETAIL = "artist_detail";

    private RecyclerView mRecyclerView;
    private ContentLoadingProgressBar mProgressBar;
    private MediaItem mPickedArtist;
    private ArrayList<MediaItem> mItems;

    private final SubscriptionCallback mSubscriptionCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaItem> children) {
            Log.d(TAG, "onChildrenLoaded: loaded " + children.size() + " items");
            showLoading(false);
        }
    };

    public static ArtistDetailFragment newInstance(MediaItem artist) {
        Bundle args = new Bundle();
        args.putParcelable(KEY_ARTIST, artist);
        ArtistDetailFragment fragment = new ArtistDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null || (mPickedArtist = args.getParcelable(KEY_ARTIST)) == null) {
            throw new IllegalStateException("Caller must specify the artist to display.");
        }

        if (savedInstanceState != null) {
            mItems = savedInstanceState.getParcelableArrayList(KEY_ARTIST_DETAIL);
        } else mItems = new ArrayList<>();
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_artist_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mProgressBar = (ContentLoadingProgressBar) view.findViewById(android.R.id.progress);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);

        if (savedInstanceState == null) {
            showLoading(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        MediaBrowserFragment.getInstance(getActivity().getSupportFragmentManager())
                .subscribe(mPickedArtist.getMediaId(), mSubscriptionCallback);
    }

    @Override
    public void onStop() {
        MediaBrowserFragment.getInstance(getActivity().getSupportFragmentManager())
                .unsubscribe(mPickedArtist.getMediaId());
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(KEY_ARTIST_DETAIL, mItems);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        mRecyclerView = null;
        mProgressBar = null;
        super.onDestroyView();
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
}
