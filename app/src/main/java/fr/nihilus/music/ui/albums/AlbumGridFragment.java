package fr.nihilus.music.ui.albums;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import fr.nihilus.music.R;
import fr.nihilus.music.di.ActivityScoped;
import fr.nihilus.music.library.MediaBrowserConnection;
import fr.nihilus.music.utils.MediaID;
import fr.nihilus.recyclerfragment.RecyclerFragment;

@ActivityScoped
public class AlbumGridFragment extends RecyclerFragment implements AlbumsAdapter.OnAlbumSelectedListener {

    private static final String TAG = "AlbumGridFragment";
    private static final String KEY_ITEMS = "Albums";

    private AlbumsAdapter mAdapter;

    @Inject MediaBrowserConnection mBrowserConnection;

    private final SubscriptionCallback mCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaItem> albums) {
            Log.d(TAG, "onChildrenLoaded: loaded children count: " + albums.size());
            mAdapter.updateAlbums(albums);
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

        mAdapter = new AlbumsAdapter(getContext());
        mAdapter.setOnAlbumSelectedListener(this);
    }

    @NonNull
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_albums, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getRecyclerView().setHasFixedSize(true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setAdapter(mAdapter);
        if (savedInstanceState == null) {
            // Show progress indicator while loading album items
            setRecyclerShown(false);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(R.string.action_albums);
        mBrowserConnection.subscribe(MediaID.ID_ALBUMS, mCallback);
        Log.d(TAG, "Subscribed, waiting for items...");
    }

    @Override
    public void onStop() {
        mBrowserConnection.unsubscribe(MediaID.ID_ALBUMS);
        super.onStop();
    }

    @Override
    public void onAlbumSelected(AlbumsAdapter.AlbumHolder holder, MediaItem album) {
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                getActivity(), holder.albumArt, AlbumDetailActivity.ALBUM_ART_TRANSITION_NAME);
        Intent albumDetailIntent = new Intent(getContext(), AlbumDetailActivity.class);
        albumDetailIntent.putExtra(AlbumDetailActivity.ARG_PICKED_ALBUM, album);
        albumDetailIntent.putExtra(AlbumDetailActivity.ARG_PALETTE, holder.colors);
        startActivity(albumDetailIntent, options.toBundle());
    }
}
