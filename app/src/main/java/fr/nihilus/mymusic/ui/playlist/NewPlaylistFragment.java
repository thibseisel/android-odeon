package fr.nihilus.mymusic.ui.playlist;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v7.app.AppCompatDialogFragment;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fr.nihilus.mymusic.MediaBrowserFragment;
import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.ui.songs.SongAdapter;
import fr.nihilus.mymusic.utils.MediaID;

public class NewPlaylistFragment extends AppCompatDialogFragment
        implements AdapterView.OnItemClickListener, View.OnClickListener {

    private static final String TAG = "NewPlaylistFragment";
    private static final String KEY_SONGS = "songs";
    private static final String ARG_SONGS_IDS = "song_ids";

    private TextInputLayout mTitleLayout;
    private TextInputEditText mTitleInput;
    private ListView mListView;
    private TextView mMessage;
    private Button mValidateButton;

    private SongAdapter mAdapter;
    private ArrayList<MediaItem> mSongs;

    private final SubscriptionCallback mCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaItem> children) {
            mSongs.addAll(children);
            mAdapter.notifyDataSetChanged();

            // In case we have provided song ids as arguments
            Bundle args = getArguments();
            long[] songIds;
            if (args != null && (songIds = args.getLongArray(ARG_SONGS_IDS)) != null) {
                Arrays.sort(songIds);

                // Each song having the id of the ones specified as an argument is marked as selected
                int size = children.size();
                for (int i = 0; i < size; i++) {
                    String mediaId = children.get(i).getMediaId();
                    long musicId = Long.parseLong(MediaID.extractMusicID(mediaId));
                    int index = Arrays.binarySearch(songIds, musicId);
                    if (index >= 0) {
                        mListView.setItemChecked(i, true);
                    }
                }

                int selectedCount = mListView.getCheckedItemCount();
                CharSequence selectedMessage = getResources()
                        .getQuantityString(R.plurals.selected_song_count, selectedCount, selectedCount);
                mMessage.setText(selectedMessage);
            }
        }
    };

    /**
     * Create a new instance of a dialog that allow to choose songs to add to a new playlist.
     *
     * @param songIds optionnal ids of songs to select for addition in the new playlist.
     * @return new fragment instance
     */
    public static NewPlaylistFragment newInstance(@Nullable long... songIds) {
        Bundle args = new Bundle();
        args.putLongArray(ARG_SONGS_IDS, songIds);
        NewPlaylistFragment fragment = new NewPlaylistFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mSongs = savedInstanceState.getParcelableArrayList(KEY_SONGS);
        } else mSongs = new ArrayList<>();

        mAdapter = new SongAdapter(getContext(), mSongs);
        setStyle(AppCompatDialogFragment.STYLE_NO_TITLE, R.style.AppTheme_DialogWhenLarge);
    }

    @Override
    public void onStart() {
        super.onStart();
        MediaBrowserFragment.getInstance(getFragmentManager()).subscribe(MediaID.ID_MUSIC, mCallback);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(KEY_SONGS, mSongs);
    }

    @Override
    public void onDestroyView() {
        mTitleLayout = null;
        mTitleInput = null;
        mListView = null;
        mMessage = null;
        mValidateButton = null;
        super.onDestroyView();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_new_playlist, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTitleLayout = (TextInputLayout) view.findViewById(R.id.titleLayout);
        mTitleInput = (TextInputEditText) mTitleLayout.findViewById(R.id.title);

        mValidateButton = (Button) view.findViewById(R.id.validate);
        mValidateButton.setOnClickListener(this);

        mMessage = (TextView) view.findViewById(R.id.selected_songs);
        mMessage.setText(R.string.new_playlist_help_message);

        mListView = (ListView) view.findViewById(android.R.id.list);
        mListView.setAdapter(mAdapter);
        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        mListView.setOnItemClickListener(this);

        // Add a dismiss button to the toolbar
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.menu_dialog);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_close) {
                    dismiss();
                }
                return false;
            }
        });
    }

    @Override
    public void onClick(View v) {
        final CharSequence playlistTitle = mTitleInput.getText();
        if (playlistTitle.length() == 0) {
            mTitleLayout.setError(getString(R.string.playlist_title_error));
            mTitleInput.requestFocus();
            return;
        }

        mTitleLayout.setError(null);

        int checkedItemCount = mListView.getCheckedItemCount();
        if (checkedItemCount == 0) {
            mMessage.setText(R.string.new_playlist_help_message);
            return;
        }

        SparseBooleanArray selectedSongs = mListView.getCheckedItemPositions();
        Log.d(TAG, "onClick: selected items = " + selectedSongs.toString());

        mMessage.setText(R.string.saving_playlist);
        MediaItem[] playlistSongs = new MediaItem[checkedItemCount];
        int position = 0;

        for (int index = 0; index < selectedSongs.size(); index++) {
            if (selectedSongs.valueAt(index)) {
                playlistSongs[position++] = mAdapter.getItem(selectedSongs.keyAt(index));
            }
        }

        // Starts a new async task in a retained fragment, and dismiss this fragment when finished.
        Fragment task = CreatePlaylistTaskFragment.newInstance(playlistTitle, playlistSongs);
        task.setTargetFragment(this, 0);
        getFragmentManager().beginTransaction().add(task, CreatePlaylistTaskFragment.TAG).commit();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        int selectedCount = mListView.getCheckedItemCount();
        CharSequence selectedMessage = getResources()
                .getQuantityString(R.plurals.selected_song_count, selectedCount, selectedCount);
        mMessage.setText(selectedMessage);
    }
}
