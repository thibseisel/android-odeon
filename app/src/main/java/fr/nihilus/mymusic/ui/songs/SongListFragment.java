package fr.nihilus.mymusic.ui.songs;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ContentLoadingProgressBar;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import fr.nihilus.mymusic.MediaBrowserFragment;
import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.service.MusicService;
import fr.nihilus.mymusic.utils.MediaID;

import static android.support.v4.media.session.MediaControllerCompat.getMediaController;
import static fr.nihilus.mymusic.utils.MediaID.ID_MUSIC;

public class SongListFragment extends Fragment implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_songlist, menu);
        final SearchView searchView = (SearchView) MenuItemCompat
                .getActionView(menu.findItem(R.id.action_search));
        // TODO Search and filtering functionalities
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
        setupListHeader();
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(new SongListActionMode());
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
        MediaBrowserFragment.getInstance(getFragmentManager()).subscribe(ID_MUSIC, mCallback);
        getActivity().setTitle(R.string.all_music);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(KEY_SONGS, mSongs);
        outState.putInt(KEY_SCROLL, mListView.getFirstVisiblePosition());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        super.onStop();
        MediaBrowserFragment.getInstance(getFragmentManager()).unsubscribe(ID_MUSIC);
    }

    @Override
    public void onDestroyView() {
        mListView = null;
        mProgressBar = null;
        mListContainer = null;
        super.onDestroyView();
    }

    private void setupListHeader() {
        final Context context = getContext();
        final LayoutInflater inflater = LayoutInflater.from(context);
        View headerView = inflater.inflate(R.layout.random_button, mListView, false);
        Drawable icRandom = AppCompatResources.getDrawable(context, R.drawable.ic_shuffle_primary);
        ((TextView) headerView.findViewById(R.id.text)).setCompoundDrawables(icRandom, null, null, null);
        mListView.addHeaderView(headerView);
    }

    @Override
    public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
        MediaControllerCompat controller = getMediaController(getActivity());
        if (controller != null) {
            if (position == 0) {
                Bundle extras = new Bundle();
                extras.putBoolean(MusicService.EXTRA_RANDOM_ENABLED, true);
                controller.getTransportControls().sendCustomAction(MusicService.CUSTOM_ACTION_RANDOM, extras);
                controller.getTransportControls().playFromMediaId(MediaID.createMediaID(null, ID_MUSIC), null);
            } else {
                // Offset the position as the header is considered at position 0
                MediaItem clickedItem = mAdapter.getItem(position - 1);
                controller.getTransportControls().playFromMediaId(clickedItem.getMediaId(), null);
            }
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        return false;
    }

    private void handleDeleteSongs() {
        int index = 0;
        SparseBooleanArray checked = mListView.getCheckedItemPositions();
        MediaItem[] toDelete = new MediaItem[mListView.getCheckedItemCount()];
        for (int i = 0; i < checked.size(); i++) {
            if (checked.valueAt(i)) {
                int pos = checked.keyAt(i);
                toDelete[index++] = mSongs.get(pos - 1);
            }
        }
        ConfirmDeleteDialog dialog = ConfirmDeleteDialog.newInstance(toDelete);
        dialog.show(getFragmentManager(), ConfirmDeleteDialog.TAG);
    }

    /**
     * An ActionMode that handles multiple item selection inside the song ListView.
     */
    private class SongListActionMode implements MultiChoiceModeListener {

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            mode.setTitle(String.valueOf(mListView.getCheckedItemCount()));
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.actionmode_songlist, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    handleDeleteSongs();
                    mode.finish();
                    return true;
                case R.id.action_playlist:
                    // TODO Ouvrir écran des playlists
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            // By default, deselect items
        }
    }
}
