package fr.nihilus.mymusic.ui.artists;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import fr.nihilus.mymusic.MediaBrowserFragment;
import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.ui.albums.AlbumDetailActivity;

public class ArtistDetailFragment extends Fragment
        implements ArtistDetailAdapter.OnMediaItemSelectedListener {

    public static final String BACKSTACK_ENTRY = "artist_detail";

    private static final String TAG = "ArtistDetailFragment";
    private static final String KEY_ARTIST = "artist";
    private static final String KEY_ARTIST_DETAIL = "artist_detail";

    private RecyclerView mRecyclerView;
    private ContentLoadingProgressBar mProgressBar;
    private MediaItem mPickedArtist;
    private ArrayList<MediaItem> mItems;
    private ArtistDetailAdapter mAdapter;

    private final SubscriptionCallback mCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaItem> children) {
            Log.d(TAG, "onChildrenLoaded: loaded " + children.size() + " items");
            mItems.clear();
            mItems.addAll(children);
            mAdapter.updateItems(children);
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

        mAdapter = new ArtistDetailAdapter(getContext(), mItems);
        mAdapter.setOnMediaItemSelectedListener(this);
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
        mRecyclerView.setAdapter(mAdapter);

        final int spanCount = getResources().getInteger(R.integer.artist_grid_span_count);
        GridLayoutManager manager = new GridLayoutManager(getContext(), spanCount);
        mRecyclerView.setLayoutManager(manager);
        manager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                Bundle extras = mItems.get(position).getDescription().getExtras();
                if (extras != null) {
                    boolean isAlbum = extras.getString(AlbumColumns.ALBUM_KEY, null) != null;
                    return isAlbum ?  1 : spanCount;
                }
                return 1;
            }
        });

        if (savedInstanceState == null) {
            showLoading(true);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(mPickedArtist.getDescription().getTitle());
        MediaBrowserFragment.getInstance(getFragmentManager())
                .subscribe(mPickedArtist.getMediaId(), mCallback);
    }

    @Override
    public void onStop() {
        MediaBrowserFragment.getInstance(getFragmentManager())
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
