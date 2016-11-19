package fr.nihilus.mymusic.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

import fr.nihilus.mymusic.R;

public class AlbumGridFragment extends Fragment {

    public static final int SPAN_COUNT = 2;
    private static final String TAG = "AlbumGridFragment";
    private static final String KEY_ITEMS = "Albums";

    private RecyclerView mRecyclerView;
    private AlbumsAdapter mAdapter;
    private ArrayList<MediaBrowserCompat.MediaItem> mItems = new ArrayList<>();

    private final AlbumsAdapter.OnAlbumSelectedListener mAlbumSelectedListener =
            new AlbumsAdapter.OnAlbumSelectedListener() {
                @Override
                public void onAlbumSelected(String mediaId) {
                    // TODO Open that fucking album with a shared-element animation
                }
            };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_albums, container, false);
        mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerview);
        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mItems = savedInstanceState.getParcelableArrayList(KEY_ITEMS);
        }

        mAdapter = new AlbumsAdapter(getContext(), mItems);
        mAdapter.setOnAlbumSelectedListener(mAlbumSelectedListener);

        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), SPAN_COUNT));
        mRecyclerView.setAdapter(mAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(KEY_ITEMS, mItems);
        super.onSaveInstanceState(outState);
    }
}
