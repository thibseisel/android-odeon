package fr.nihilus.mymusic.ui.playlist;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.di.ActivityScoped;
import fr.nihilus.mymusic.library.MediaBrowserConnection;
import fr.nihilus.mymusic.utils.MediaID;
import io.reactivex.functions.Consumer;

@ActivityScoped
public class PlaylistsFragment extends Fragment implements PlaylistsAdapter.OnPlaylistSelectedListener {

    private static final String TAG = "PlaylistsFragment";
    private static final String KEY_PLAYLISTS = "playlists";

    private ArrayList<MediaItem> mPlaylists;
    private PlaylistsAdapter mAdapter;
    private RecyclerView mRecyclerView;
    private ContentLoadingProgressBar mProgressBar;
    private View mEmptyView;

    @Inject MediaBrowserConnection mBrowserConnection;

    private final SubscriptionCallback mCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaItem> children) {
            Log.d(TAG, "onChildrenLoaded: loaded " + children.size() + " items from " + parentId);
            mPlaylists.clear();
            mPlaylists.addAll(children);
            mAdapter.notifyDataSetChanged();
            showLoading(false);
            showEmptyView(children.isEmpty());
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
        mRecyclerView = view.findViewById(R.id.recyclerView);
        mProgressBar = view.findViewById(android.R.id.progress);
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
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.action_playlists);
        mBrowserConnection.subscribe(MediaID.ID_PLAYLISTS, mCallback);
    }

    @Override
    public void onPause() {
        mBrowserConnection.unsubscribe(MediaID.ID_PLAYLISTS);
        showLoading(false);
        super.onPause();
    }

    @Override
    public void onDestroyView() {
        mRecyclerView = null;
        mProgressBar = null;
        mEmptyView = null;
        super.onDestroyView();
    }

    @Override
    public void onPlaylistSelected(PlaylistsAdapter.PlaylistHolder holder, final MediaItem playlist) {
        mBrowserConnection.getMediaController().take(1).subscribe(new Consumer<MediaControllerCompat>() {
            @Override
            public void accept(@Nullable MediaControllerCompat controller) throws Exception {
                if (controller != null) {
                    controller.getTransportControls().playFromMediaId(playlist.getMediaId(), null);
                }
            }
        });
    }
}
