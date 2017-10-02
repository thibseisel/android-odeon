package fr.nihilus.music.ui.songs;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
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

import java.util.List;

import dagger.android.support.AndroidSupportInjection;
import fr.nihilus.music.R;
import fr.nihilus.music.library.BrowserViewModel;
import fr.nihilus.music.utils.MediaID;

import static android.support.v4.media.session.MediaControllerCompat.getMediaController;
import static fr.nihilus.music.utils.MediaID.ID_MUSIC;

public class SongListFragment extends Fragment implements AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {

    private static final String TAG = "SongListFragment";
    private static final String KEY_SCROLL = "ScrollY";

    private ListView mListView;
    private View mListContainer;
    private SongAdapter mAdapter;
    private ContentLoadingProgressBar mProgressBar;

    private BrowserViewModel mViewModel;

    private final SubscriptionCallback mCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, @NonNull List<MediaItem> items) {
            Log.d(TAG, "onChildrenLoaded: loaded " + items.size() + " from " + parentId);
            mAdapter.updateItems(items);
            mProgressBar.hide();
            mListContainer.setVisibility(View.VISIBLE);
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
        mAdapter = new SongAdapter(this);
        mViewModel = ViewModelProviders.of(getActivity()).get(BrowserViewModel.class);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.menu_songlist, menu);
        final SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        // TODO Search and filtering features
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

        mListView = view.findViewById(R.id.list);
        setupListHeader();
        mListView.setAdapter(mAdapter);
        mListView.setOnItemClickListener(this);
        mListView.setEmptyView(view.findViewById(android.R.id.empty));
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setMultiChoiceModeListener(new SongListActionMode());
        ViewCompat.setNestedScrollingEnabled(mListView, true);

        mProgressBar = view.findViewById(android.R.id.progress);

        mListContainer.setVisibility(View.GONE);
        if (savedInstanceState == null) {
            mProgressBar.show();
        } else {
            mListView.setSelectionFromTop(savedInstanceState.getInt(KEY_SCROLL), 0);
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().setTitle(R.string.all_music);
        mViewModel.subscribe(MediaID.ID_MUSIC, mCallback);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (isVisible()) {
            outState.putInt(KEY_SCROLL, mListView.getFirstVisiblePosition());
        }
    }

    @Override
    public void onStop() {
        mViewModel.unsubscribe(MediaID.ID_MUSIC);
        super.onStop();
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
		if (position == 0) {
			mViewModel.setShuffleMode(PlaybackStateCompat.SHUFFLE_MODE_ALL);
			mViewModel.playFromMediaId(MediaID.createMediaID(null, ID_MUSIC));
		} else {
			// Offset the position as the header is considered at position 0
			MediaItem clickedItem = mAdapter.getItem(position - 1);
			mViewModel.playFromMediaId(clickedItem.getMediaId());
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
                toDelete[index++] = mAdapter.getItem(pos - 1);
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
