package fr.nihilus.mymusic.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import fr.nihilus.mymusic.MediaBrowserFragment;
import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.utils.MediaIDHelper;

public class SongListFragment extends Fragment implements AdapterView.OnItemClickListener {

    private static final String TAG = "SongListFragment";
    private static final String KEY_SONGS = "MediaItems";
    private static final String KEY_SCROLL = "ScrollY";

    private ListView mListView;
    private SongAdapter mAdapter;
    private ArrayList<MediaItem> mItems = new ArrayList<>();
    private TextView mFooterText;
    private View mEmptyView;

    private final SubscriptionCallback mCallback = new MediaBrowserCompat.SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaItem> items) {
            Log.d(TAG, "onChildrenLoaded: loaded " + items.size() + " from " + parentId);
            mItems.clear();
            mItems.addAll(items);
            updateSongCount();
            mAdapter.notifyDataSetChanged();

            final boolean isEmpty = items.size() == 0;
            setListShown(!isEmpty);
            showEmptyView(isEmpty);
        }

        @Override
        public void onError(@NonNull String parentId) {
            Log.e(TAG, "onError: error while loading " + parentId);
        }
    };

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_songs, container, false);
        mListView = (ListView) rootView.findViewById(android.R.id.list);
        ViewCompat.setNestedScrollingEnabled(mListView, true);
        mListView.setOnItemClickListener(this);

        mEmptyView = rootView.findViewById(android.R.id.empty);

        setupListHeader(inflater);
        setupListFooter(inflater);

        return rootView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {
            mItems = savedInstanceState.getParcelableArrayList(KEY_SONGS);
            mListView.setScrollY(savedInstanceState.getInt(KEY_SCROLL));
            Log.d(TAG, "onActivityCreated: state restored.");
        }

        Log.d(TAG, "onActivityCreated: item count: " + mItems.size());

        mAdapter = new SongAdapter(getContext(), mItems);
        mListView.setAdapter(mAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        MediaBrowserFragment.getInstance(getActivity().getSupportFragmentManager())
                .subscribe(MediaIDHelper.MEDIA_ID_ALL_MUSIC, mCallback);
    }

    @Override
    public void onStop() {
        super.onStop();
        MediaBrowserFragment.getInstance(getActivity().getSupportFragmentManager())
                .unsubscribe(MediaIDHelper.MEDIA_ID_ALL_MUSIC);
    }

    private void setupListHeader(final LayoutInflater inflater) {
        View listHeader = inflater.inflate(R.layout.list_header_button, mListView, false);
        mListView.addHeaderView(listHeader);
        mListView.setHeaderDividersEnabled(true);
        listHeader.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getContext(), R.string.play_all_shuffled, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupListFooter(final LayoutInflater inflater) {
        mFooterText = (TextView) inflater.inflate(R.layout.list_footer, mListView, false);
        mListView.addFooterView(mFooterText);
        updateSongCount();
    }

    void updateSongCount() {
        mFooterText.setText(getString(R.string.song_count, mItems.size()));
    }

    void setListShown(boolean isShown) {
        mListView.setVisibility(isShown ? View.VISIBLE : View.GONE);
    }

    void showEmptyView(boolean isShown) {
        if (mEmptyView instanceof ViewStub && isShown) {
            mEmptyView = ((ViewStub) mEmptyView).inflate();
            ((ImageView) mEmptyView.findViewById(R.id.empty_image)).setImageResource(R.drawable.sad_panda);
            ((TextView) mEmptyView.findViewById(R.id.empty_text)).setText(R.string.empty_songs);
        }
        mEmptyView.setVisibility(isShown ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList(KEY_SONGS, mItems);
        outState.putInt(KEY_SCROLL, mListView.getScrollY());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
        MediaItem clickedItem = mAdapter.getItem(position);
        MediaControllerCompat controller = getActivity().getSupportMediaController();
        if (controller != null) {
            Log.d(TAG, "onItemClick: playing song at position " + position);
            controller.getTransportControls().playFromMediaId(clickedItem.getMediaId(), null);
        }
    }
}
