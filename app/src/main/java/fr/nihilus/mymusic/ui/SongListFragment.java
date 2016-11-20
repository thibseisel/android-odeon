package fr.nihilus.mymusic.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import fr.nihilus.mymusic.MediaBrowserFragment;
import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.utils.MediaIDHelper;

public class SongListFragment extends ListFragment {

    private static final String TAG = "SongListFragment";
    private static final String KEY_SONGS = "MediaItems";
    private static final String KEY_SCROLL = "ScrollY";

    private ArrayList<MediaItem> mItems = new ArrayList<>();
    private TextView mFooterText;
    private TextView mEmptyTextView;

    private final SubscriptionCallback mCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaItem> items) {
            Log.d(TAG, "onChildrenLoaded: loaded " + items.size() + " from " + parentId);
            mItems.clear();
            mItems.addAll(items);
            updateSongCount();
            getListAdapter().notifyDataSetChanged();
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_songs, container, false);
        mEmptyTextView = (TextView) root.findViewById(R.id.empty_text);
        return root;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LayoutInflater inflater = LayoutInflater.from(getContext());

        setupListHeader(inflater);
        setupListFooter(inflater);

        ViewCompat.setNestedScrollingEnabled(getListView(), true);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mItems = savedInstanceState.getParcelableArrayList(KEY_SONGS);
            //getListView().setScrollY(savedInstanceState.getInt(KEY_SCROLL));
            Log.d(TAG, "onActivityCreated: state restored.");
        }

        setListAdapter(new SongAdapter(getContext(), mItems));
    }

    @Override
    public SongAdapter getListAdapter() {
        return ((SongAdapter) super.getListAdapter());
    }

    @Override
    public void onStart() {
        super.onStart();
        MediaBrowserFragment.getInstance(getActivity().getSupportFragmentManager())
                .subscribe(MediaIDHelper.MEDIA_ID_ALL_MUSIC, mCallback);
        getActivity().setTitle(R.string.all_music);
    }

    @Override
    public void onStop() {
        super.onStop();
        MediaBrowserFragment.getInstance(getActivity().getSupportFragmentManager())
                .unsubscribe(MediaIDHelper.MEDIA_ID_ALL_MUSIC);
    }

    private void setupListHeader(final LayoutInflater inflater) {
        View listHeader = inflater.inflate(R.layout.list_header_button, getListView(), false);
        getListView().addHeaderView(listHeader);
        getListView().setHeaderDividersEnabled(true);
        listHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO Tout jouer en aléatoire
                Toast.makeText(getContext(), R.string.play_all_shuffled, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListFooter(final LayoutInflater inflater) {
        mFooterText = (TextView) inflater.inflate(R.layout.list_footer, getListView(), false);
        getListView().addFooterView(mFooterText);
        updateSongCount();
    }

    void updateSongCount() {
        mFooterText.setText(getString(R.string.song_count, mItems.size()));
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(KEY_SONGS, mItems);
        outState.putInt(KEY_SCROLL, getListView().getScrollY());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        MediaItem clickedItem = getListAdapter().getItem(position);
        MediaControllerCompat controller = getActivity().getSupportMediaController();
        if (controller != null && clickedItem.isPlayable()) {
            Log.d(TAG, "onItemClick: playing song at position " + position);
            controller.getTransportControls().playFromMediaId(clickedItem.getMediaId(), null);
            setSelection(position);
        }
    }

    @Override
    public void setEmptyText(CharSequence text) {
        mEmptyTextView.setText(text);
    }
}
