package fr.nihilus.mymusic.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
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
import fr.nihilus.mymusic.utils.MediaIDHelper;
import fr.nihilus.mymusic.widget.GridSpacerDecoration;

public class AlbumGridFragment extends Fragment implements AlbumsAdapter.OnAlbumSelectedListener {

    private static final String TAG = "AlbumGridFragment";
    private static final String KEY_ITEMS = "Albums";

    private RecyclerView mRecyclerView;
    private AlbumsAdapter mAdapter;
    private ContentLoadingProgressBar mProgressBar;
    private View mEmptyView;
    private ArrayList<MediaItem> mAlbums;

    private final SubscriptionCallback mSubscriptionCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaItem> albums) {
            Log.d(TAG, "onChildrenLoaded: loaded children count: " + albums.size());
            mAlbums.clear();
            mAlbums.addAll(albums);
            mAdapter.updateAlbums(albums);
            showLoading(false);
            showEmptyView(albums.isEmpty());
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mAlbums = savedInstanceState.getParcelableArrayList(KEY_ITEMS);
        } else {
            mAlbums = new ArrayList<>();
            MediaBrowserFragment.getInstance(getActivity().getSupportFragmentManager())
                    .subscribe(MediaIDHelper.MEDIA_ID_ALBUMS, mSubscriptionCallback);
        }

        mAdapter = new AlbumsAdapter(getContext(), mAlbums);
        mAdapter.setOnAlbumSelectedListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_albums, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        mRecyclerView.addItemDecoration(new GridSpacerDecoration(getContext()));
        mProgressBar = (ContentLoadingProgressBar) view.findViewById(android.R.id.progress);
        mEmptyView = view.findViewById(android.R.id.empty);

        mRecyclerView.setAdapter(mAdapter);

        if (mAlbums.isEmpty()) {
            Log.d(TAG, "onViewCreated: showing ProgressBar");
            showLoading(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(R.string.action_albums);
    }

    @Override
    public void onStop() {
        super.onStop();
        MediaBrowserFragment.getInstance(getActivity().getSupportFragmentManager())
                .unsubscribe(MediaIDHelper.MEDIA_ID_ALBUMS);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(KEY_ITEMS, mAlbums);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        mRecyclerView = null;
        mProgressBar = null;
        mEmptyView = null;
        super.onDestroyView();
    }

    @Override
    public void onAlbumSelected(MediaItem album, AlbumsAdapter.AlbumHolder holder) {
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                getActivity(), holder.albumArt, AlbumDetailActivity.ALBUM_ART_TRANSITION_NAME);
        Intent albumDetailIntent = new Intent(getContext(), AlbumDetailActivity.class);
        albumDetailIntent.putExtra(AlbumDetailActivity.ARG_PICKED_ALBUM, album);
        albumDetailIntent.putExtra(AlbumDetailActivity.ARG_PALETTE, holder.colors);
        startActivity(albumDetailIntent, options.toBundle());
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
