package fr.nihilus.music.ui.playlist;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import fr.nihilus.music.R;
import fr.nihilus.music.di.ActivityScoped;
import fr.nihilus.music.library.BrowserViewModel;
import fr.nihilus.music.library.NavigationController;
import fr.nihilus.music.utils.MediaID;
import fr.nihilus.recyclerfragment.RecyclerFragment;

@ActivityScoped
public class PlaylistsFragment extends RecyclerFragment implements PlaylistsAdapter.OnPlaylistSelectedListener {

    private static final String TAG = "PlaylistsFragment";

    @Inject NavigationController mRouter;
    private PlaylistsAdapter mAdapter;
    private final SubscriptionCallback mCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaItem> children) {
            Log.d(TAG, "onChildrenLoaded: loaded " + children.size() + " items from " + parentId);
            mAdapter.update(children);
            setRecyclerShown(true);
        }
    };
    private BrowserViewModel mViewModel;

    @Override
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mAdapter = new PlaylistsAdapter(this);
        mAdapter.setOnPlaylistSelectedListener(this);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = ViewModelProviders.of(getActivity()).get(BrowserViewModel.class);

        setAdapter(mAdapter);
        if (savedInstanceState == null) {
            setRecyclerShown(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(R.string.action_playlists);
        mViewModel.subscribe(MediaID.ID_PLAYLISTS, mCallback);
    }

    @Override
    public void onStop() {
        mViewModel.unsubscribe(MediaID.ID_PLAYLISTS);
        super.onStop();
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

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist, container, false);
    }

    @Override
    public void onPlaylistSelected(PlaylistsAdapter.PlaylistHolder holder, MediaItem playlist) {
        mRouter.navigateToPlaylistDetails(playlist);
    }

    @Override
    public void onPlay(final MediaItem playlist) {
        String mediaId = playlist.getMediaId();
        if (mediaId == null) {
            throw new AssertionError("Playlists should have a MediaId");
        }

        mViewModel.playFromMediaId(playlist.getMediaId());
    }
}
