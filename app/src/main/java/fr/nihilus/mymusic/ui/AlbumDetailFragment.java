package fr.nihilus.mymusic.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserCompat.SubscriptionCallback;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.view.ViewCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.utils.MediaIDHelper;


public class AlbumDetailFragment extends Fragment {

    private static final String KEY_PICKED_ALBUM = "picked_album";

    private static final String TAG = "AlbumDetailFragment";
    private static final String KEY_TRACKS = "tracks";

    private ListView mListView;
    private ImageView mAlbumArt;
    private AlbumtrackAdapter mAdapter;
    private List<MediaItem> mTracks;
    private MediaItem mAlbum;

    public static AlbumDetailFragment newInstance(MediaItem pickedAlbum) {
        Bundle args = new Bundle();
        args.putParcelable(KEY_PICKED_ALBUM, pickedAlbum);
        AlbumDetailFragment fragment = new AlbumDetailFragment();
        fragment.setArguments(args);
        return fragment;
    }

    private final SubscriptionCallback mSubscriptionCallback = new SubscriptionCallback() {
        @Override
        public void onChildrenLoaded(@NonNull String parentId, List<MediaItem> children) {
            mTracks.clear();
            mTracks.addAll(children);
            mAdapter.notifyDataSetChanged();
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            mAlbum = args.getParcelable(KEY_PICKED_ALBUM);
            if (mAlbum == null) {
                throw new IllegalStateException("No album specified.");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_album_detail, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mListView = (ListView) view.findViewById(android.R.id.list);
        mAlbumArt = (ImageView) view.findViewById(R.id.albumArt);

        ViewCompat.setTransitionName(mAlbumArt, "albumArt");
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Drawable defaultAlbumArt = ContextCompat.getDrawable(getContext(), R.drawable.dummy_album_art);

        Glide.with(getContext())
                .load(mAlbum.getDescription().getIconUri()).asBitmap()
                .error(defaultAlbumArt)
                .into(mAlbumArt);

        if (savedInstanceState != null) {
            mTracks = savedInstanceState.getParcelableArrayList(KEY_TRACKS);
        } else mTracks = new ArrayList<>();

        mAdapter = new AlbumtrackAdapter(getContext(), mTracks);
        mListView.setAdapter(mAdapter);
    }

    @Override
    public void onStart() {
        super.onStart();
        /*MediaBrowserFragment.getInstance(getActivity().getSupportFragmentManager())
                .subscribe(mAlbum.getMediaId(), mSubscriptionCallback);*/
        getActivity().setTitle(mAlbum.getDescription().getTitle());
    }

    @Override
    public void onStop() {
        super.onStop();
        /*MediaBrowserFragment.getInstance(getActivity().getSupportFragmentManager())
                .unsubscribe(mAlbum.getMediaId());*/
    }

    /**
     * Un Adapter pour afficher une liste des pistes disponibles sur l'album sélectionné.
     */
    private static class AlbumtrackAdapter extends BaseAdapter {

        private final List<MediaItem> mItems;
        private final LayoutInflater mInflater;

        AlbumtrackAdapter(@NonNull Context ctx, @NonNull List<MediaItem> tracks) {
            mInflater = LayoutInflater.from(ctx);
            mItems = tracks;
        }

        @Override
        public int getCount() {
            return mItems != null ? mItems.size() : 0;
        }

        @Override
        public MediaItem getItem(int position) {
            return mItems != null ? mItems.get(position) : null;
        }

        @Override
        public long getItemId(int position) {
            if (hasStableIds() && mItems != null) {
                String mediaId = mItems.get(position).getMediaId();
                return Long.parseLong(MediaIDHelper.extractMusicIDFromMediaID(mediaId));
            }
            return ListView.NO_ID;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final AlbumTrackHolder holder;
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.track_list_item, parent, false);
                holder = new AlbumTrackHolder(convertView);
                convertView.setTag(holder);
            } else holder = (AlbumTrackHolder) convertView.getTag();

            final MediaDescriptionCompat item = mItems.get(position).getDescription();
            // TODO numéro de piste
            holder.title.setText(item.getTitle());
            holder.duration.setText(item.getDescription());
            return convertView;
        }
    }

    private static class AlbumTrackHolder {

        final TextView trackNumber;
        final TextView title;
        final TextView duration;

        AlbumTrackHolder(View rootView) {
            trackNumber = (TextView) rootView.findViewById(R.id.trackNo);
            title = (TextView) rootView.findViewById(R.id.title);
            duration = (TextView) rootView.findViewById(R.id.duration);
        }
    }
}
