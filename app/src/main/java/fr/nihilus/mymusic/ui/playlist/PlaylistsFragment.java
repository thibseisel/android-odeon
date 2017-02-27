package fr.nihilus.mymusic.ui.playlist;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import fr.nihilus.mymusic.MediaBrowserFragment;
import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.utils.MediaID;

public class PlaylistsFragment extends Fragment implements PlaylistsAdapter.OnPlaylistSelectedListener {

    private static final String TAG = "PlaylistsFragment";
    private static final String KEY_PLAYLISTS = "playlists";

    private ArrayList<MediaItem> mPlaylists;
    private PlaylistsAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private ContentLoadingProgressBar mProgressBar;
    private View mEmptyView;
    private SubscriptionCallback mCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaItem> children) {
            Log.d(TAG, "onChildrenLoaded: loaded " + children.size() + " items from " + parentId);
            mPlaylists.clear();
            mPlaylists.addAll(children);
            mAdapter.notifyDataSetChanged();
            showLoading(false);
            if (children.isEmpty()) {
                showEmptyView(true);
            }
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            mPlaylists = savedInstanceState.getParcelableArrayList(KEY_PLAYLISTS);
        } else mPlaylists = new ArrayList<>();

        mAdapter = new PlaylistsAdapter(getContext(), mPlaylists);
        mAdapter.setOnPlaylistSelectedListener(this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_playlists, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.action_new_playlist == item.getItemId()) {
            NewPlaylistFragment.newInstance().show(getFragmentManager(), null);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        mProgressBar = (ContentLoadingProgressBar) view.findViewById(android.R.id.progress);
        mEmptyView = view.findViewById(android.R.id.empty);

        mRecyclerView.setAdapter(mAdapter);

        if (savedInstanceState == null) {
            showLoading(true);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(KEY_PLAYLISTS, mPlaylists);
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

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(R.string.action_playlists);
        MediaBrowserFragment.getInstance(getFragmentManager()).subscribe(MediaID.ID_PLAYLISTS, mCallback);
    }

    @Override
    public void onStop() {
        MediaBrowserFragment.getInstance(getFragmentManager()).unsubscribe(MediaID.ID_PLAYLISTS);
        showLoading(false);
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        mRecyclerView = null;
        mProgressBar = null;
        mEmptyView = null;
        super.onDestroyView();
    }

    @Override
    public void onPlaylistSelected(PlaylistsAdapter.PlaylistHolder holder, MediaItem playlist) {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            controller.getTransportControls().playFromMediaId(playlist.getMediaId(), null);
        }
    }
}
