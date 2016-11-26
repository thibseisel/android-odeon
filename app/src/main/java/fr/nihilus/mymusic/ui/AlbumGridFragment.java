package fr.nihilus.mymusic.ui;

import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.RecyclerView;
import android.transition.Fade;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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

        Log.d(TAG, "onCreate() called with: savedInstanceState = [" + savedInstanceState + "]");

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
        Log.d(TAG, "onCreateView: album count=" + mAlbums.size());
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
        Log.d(TAG, "onStop: unsubscribing from MediaBrowser.");
        MediaBrowserFragment.getInstance(getActivity().getSupportFragmentManager())
                .unsubscribe(MediaIDHelper.MEDIA_ID_ALBUMS);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.d(TAG, "onSaveInstanceState: saving state.");
        outState.putParcelableArrayList(KEY_ITEMS, mAlbums);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView: destroying View hierarchy.");
        mRecyclerView = null;
        mProgressBar = null;
        mEmptyView = null;
        super.onDestroyView();
    }

    @Override
    public void onAlbumSelected(MediaItem album, ImageView artView) {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        AlbumDetailFragment details = AlbumDetailFragment.newInstance(album);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            details.setSharedElementEnterTransition(TransitionInflater.from(getContext())
                    .inflateTransition(android.R.transition.move));
            details.setEnterTransition(new Fade());
            setExitTransition(new Fade());
            details.setSharedElementReturnTransition(TransitionInflater.from(getContext())
                    .inflateTransition(android.R.transition.move));
        }

        Log.d(TAG, "onAlbumSelected: transitionName=" + ViewCompat.getTransitionName(artView));

        fm.beginTransaction()
                .addSharedElement(artView, "albumArt")
                .replace(R.id.container, details)
                .addToBackStack(null)
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
