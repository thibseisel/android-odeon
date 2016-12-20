package fr.nihilus.mymusic.ui.songs;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import fr.nihilus.mymusic.MediaBrowserFragment;
import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.utils.MediaID;

public class SongListFragment extends Fragment implements AdapterView.OnItemClickListener {

    // FIXME Affiche parfois la ProgressBar indéfininiment

    private static final String TAG = "SongListFragment";
    private static final String KEY_SONGS = "MediaItems";
    private static final String KEY_SCROLL = "ScrollY";

    private ArrayList<MediaItem> mSongs;
    private ListView mListView;
    private View mListContainer;
    private SongAdapter mAdapter;
    private ContentLoadingProgressBar mProgressBar;

    private final SubscriptionCallback mCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaItem> items) {
            Log.d(TAG, "onChildrenLoaded: loaded " + items.size() + " from " + parentId);
            mSongs.clear();
            mSongs.addAll(items);
            mAdapter.notifyDataSetChanged();

            mProgressBar.hide();
            mListContainer.setVisibility(View.VISIBLE);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (savedInstanceState != null) {
            mSongs = savedInstanceState.getParcelableArrayList(KEY_SONGS);
        } else mSongs = new ArrayList<>();
        mAdapter = new SongAdapter(getContext(), mSongs);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_songs, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mListContainer = view.findViewById(R.id.list_container);

        mListView = (ListView) view.findViewById(R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));
        ViewCompat.setNestedScrollingEnabled(mListView, true);

        mProgressBar = (ContentLoadingProgressBar) view.findViewById(android.R.id.progress);

        if (savedInstanceState == null) {
            mListContainer.setVisibility(View.GONE);
            mProgressBar.show();
        } else {
            mListView.setSelectionFromTop(savedInstanceState.getInt(KEY_SCROLL), 0);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        MediaBrowserFragment.getInstance(getActivity().getSupportFragmentManager())
                .subscribe(MediaID.ID_MUSIC, mCallback);
        getActivity().setTitle(R.string.all_music);
    }

    @Override
    public void onStop() {
        super.onStop();
        MediaBrowserFragment.getInstance(getActivity().getSupportFragmentManager())
                .unsubscribe(MediaID.ID_MUSIC);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(KEY_SONGS, mSongs);
        outState.putInt(KEY_SCROLL, mListView.getFirstVisiblePosition());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroyView() {
        mListView = null;
        mProgressBar = null;
        mListContainer = null;
        super.onDestroyView();
    }

    @Override
    public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
        MediaItem clickedItem = mAdapter.getItem(position);
        MediaControllerCompat controller = MediaControllerCompat.getMediaController(getActivity());
        if (controller != null && clickedItem.isPlayable()) {
            controller.getTransportControls().playFromMediaId(clickedItem.getMediaId(), null);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_songlist, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.action_random == item.getItemId()) {
            // TODO Play randomly
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
