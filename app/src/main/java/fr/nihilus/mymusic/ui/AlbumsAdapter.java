package fr.nihilus.mymusic.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.utils.MediaIDHelper;

class AlbumsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "AlbumsAdapter";

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final Drawable mDummyAlbumArt;
    private List<MediaItem> mAlbums;
    private OnAlbumSelectedListener mListener;

    AlbumsAdapter(@NonNull Context context, @NonNull ArrayList<MediaItem> items) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mAlbums = items;
        mDummyAlbumArt = ContextCompat.getDrawable(context, R.drawable.dummy_album_art);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = mInflater.inflate(R.layout.album_grid_item, parent, false);
        return new AlbumHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MediaDescriptionCompat item = mAlbums.get(position).getDescription();
        final AlbumHolder albumHolder = (AlbumHolder) holder;
        albumHolder.title.setText(item.getTitle());
        albumHolder.artistName.setText(item.getSubtitle());

        Glide.with(mContext).loadFromMediaStore(item.getIconUri())
                .error(mDummyAlbumArt)
                .into(albumHolder.albumArt);
        ViewCompat.setTransitionName(albumHolder.albumArt, "image_" + item.getMediaId());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mListener != null) {
                    final int clickedPosition = albumHolder.getAdapterPosition();
                    mListener.onAlbumSelected(mAlbums.get(clickedPosition), albumHolder.albumArt);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mAlbums != null ? mAlbums.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        if (hasStableIds() && mAlbums != null) {
            final MediaItem item = mAlbums.get(position);
            return Long.parseLong(MediaIDHelper.extractMusicIDFromMediaID(item.getMediaId()));
        }
        return RecyclerView.NO_ID;
    }

    void setOnAlbumSelectedListener(OnAlbumSelectedListener listener) {
        mListener = listener;
    }

    void updateAlbums(List<MediaItem> newAlbums) {
        MediaItemDiffCallback callback = new MediaItemDiffCallback(mAlbums, newAlbums);
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(callback, false);
        mAlbums = newAlbums;
        diff.dispatchUpdatesTo(this);
    }

    interface OnAlbumSelectedListener {
        void onAlbumSelected(MediaItem album, ImageView albumArt);
    }

    private static class AlbumHolder extends RecyclerView.ViewHolder {

        ImageView albumArt;
        TextView title;
        TextView artistName;

        AlbumHolder(View itemView) {
            super(itemView);
            albumArt = (ImageView) itemView.findViewById(R.id.albumArt);
            title = (TextView) itemView.findViewById(R.id.title);
            artistName = (TextView) itemView.findViewById(R.id.artist);
        }
    }
}
