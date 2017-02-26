package fr.nihilus.mymusic.ui.playlist;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v7.app.AppCompatDialogFragment;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
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
    private SparseBooleanArray mSelectedSongs;
    private int mSelectedCount;

    private final SubscriptionCallback mSubscriptionCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaItem> children) {
            mSongs.addAll(children);

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
                        mSelectedSongs.put(i, true);
                        mSelectedCount++;
                        mListView.getChildAt(i).setActivated(true);
                    }
                }
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
        mSelectedSongs = new SparseBooleanArray();
    }

    @Override
    public void onStart() {
        super.onStart();
        MediaBrowserFragment.getInstance(getActivity().getSupportFragmentManager())
                .subscribe(MediaID.ID_MUSIC, mSubscriptionCallback);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(KEY_SONGS, mSongs);
    }

    @Override
    public void onStop() {
        MediaBrowserFragment.getInstance(getActivity().getSupportFragmentManager())
                .unsubscribe(MediaID.ID_MUSIC);
        super.onStop();
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
        mTitleInput = (TextInputEditText) mTitleLayout.getChildAt(0);
        mListView = (ListView) view.findViewById(android.R.id.list);
        mMessage = (TextView) view.findViewById(R.id.selected_songs);
        mValidateButton = (Button) view.findViewById(R.id.validate);

        mValidateButton.setOnClickListener(this);
        mListView.setOnItemClickListener(this);
    }

    @Override
    public void onClick(View v) {
        CharSequence playlistTitle = mTitleInput.getText();
        if (playlistTitle.length() == 0) {
            mTitleLayout.setError(getString(R.string.playlist_title_error));
            mTitleInput.requestFocus();
            return;
        }

        mMessage.setText(R.string.saving_playlist);
        MediaItem[] playlistSongs = new MediaItem[mSelectedCount];
        int position = 0;

        for (int index = 0; index < mSelectedSongs.size(); index++) {
            if (mSelectedSongs.valueAt(index)) {
                playlistSongs[position++] = mAdapter.getItem(mSelectedSongs.keyAt(index));
            }
        }

        // TODO AsyncTask in another retained fragment
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        boolean isSelected = mSelectedSongs.get(position, false);
        if (!isSelected) {
            mSelectedSongs.put(position, true);
            view.setActivated(true);
            mSelectedCount++;
        } else {
            mSelectedSongs.put(position, false);
            view.setActivated(false);
            mSelectedCount--;
        }

        CharSequence selectedMessage = getResources()
                .getQuantityString(R.plurals.selected_song_count, mSelectedCount, mSelectedCount);
        mMessage.setText(selectedMessage);
    }
}
