package fr.nihilus.music.ui.artists;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v7.widget.GridLayoutManager;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.android.support.AndroidSupportInjection;
import fr.nihilus.music.R;
import fr.nihilus.music.di.ActivityScoped;
import fr.nihilus.music.library.MediaBrowserConnection;
import fr.nihilus.music.ui.albums.AlbumDetailActivity;
import fr.nihilus.recyclerfragment.RecyclerFragment;

@ActivityScoped
public class ArtistDetailFragment extends RecyclerFragment
        implements ArtistDetailAdapter.OnMediaItemSelectedListener {

    public static final String BACKSTACK_ENTRY = "artist_detail";

    private static final String TAG = "ArtistDetailFragment";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_ARTIST_DETAIL = "artist_detail";

    private MediaItem mPickedArtist;
    private ArtistDetailAdapter mAdapter;

    @Inject MediaBrowserConnection mBrowserConnection;

    private final SubscriptionCallback mCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaItem> children) {
            Log.d(TAG, "onChildrenLoaded: loaded " + children.size() + " items");
            mAdapter.updateItems(children);
            setRecyclerShown(true);
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
    public void onAttach(Context context) {
        AndroidSupportInjection.inject(this);
        super.onAttach(context);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null || (mPickedArtist = args.getParcelable(KEY_ARTIST)) == null) {
            throw new IllegalStateException("Caller must specify the artist to display.");
        }

        mAdapter = new ArtistDetailAdapter(getContext(), new ArrayList<MediaItem>());
        mAdapter.setOnMediaItemSelectedListener(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final int spanCount = getResources().getInteger(R.integer.artist_grid_span_count);
        GridLayoutManager manager = new GridLayoutManager(getContext(), spanCount);
        getRecyclerView().setLayoutManager(manager);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                Bundle extras = mAdapter.get(position).getDescription().getExtras();
                if (extras != null) {
                    boolean isAlbum = extras.getString(AlbumColumns.ALBUM_KEY, null) != null;
                    return isAlbum ?  1 : spanCount;
                }
                return 1;
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setAdapter(mAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(mPickedArtist.getDescription().getTitle());
        mBrowserConnection.subscribe(mPickedArtist.getMediaId(), mCallback);
    }

    @Override
    public void onStop() {
        mBrowserConnection.unsubscribe(mPickedArtist.getMediaId());
        super.onStop();
    }

    @Override
    public void onAlbumSelected(ArtistDetailAdapter.AlbumHolder holder, MediaItem album) {
        ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                getActivity(), holder.albumArt, AlbumDetailActivity.ALBUM_ART_TRANSITION_NAME);
        Intent albumDetailIntent = new Intent(getContext(), AlbumDetailActivity.class);
        albumDetailIntent.putExtra(AlbumDetailActivity.ARG_PICKED_ALBUM, album);
        albumDetailIntent.putExtra(AlbumDetailActivity.ARG_PALETTE, holder.colors);
        startActivity(albumDetailIntent, options.toBundle());
    }

    @Override
    public void onTrackSelected(ArtistDetailAdapter.TrackHolder holder, MediaItem track) {
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null) {
            controller.getTransportControls().playFromMediaId(track.getMediaId(), null);
        }
    }
}
