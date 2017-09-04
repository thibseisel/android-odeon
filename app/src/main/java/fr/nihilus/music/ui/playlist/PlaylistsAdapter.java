package fr.nihilus.music.ui.playlist;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.BitmapRequestBuilder;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

import fr.nihilus.music.R;
import fr.nihilus.music.utils.MediaID;
import fr.nihilus.music.utils.MediaItemDiffCallback;

class PlaylistsAdapter extends RecyclerView.Adapter<PlaylistsAdapter.PlaylistHolder> {
    private List<MediaItem> mItems;
    private final BitmapRequestBuilder<Uri, Bitmap> mGlideRequest;
    private OnPlaylistSelectedListener mListener;

    PlaylistsAdapter(@NonNull Context context, List<MediaItem> playlists) {
        mItems = playlists;
        Drawable dummyAlbumArt = ContextCompat.getDrawable(context, R.drawable.ic_playlist_24dp);
        mGlideRequest = Glide.with(context)
                .fromUri()
                .asBitmap()
                .error(dummyAlbumArt)
                .diskCacheStrategy(DiskCacheStrategy.NONE);
    }

    @Override
    public PlaylistHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.artist_album_item, parent, false);
        return new PlaylistHolder(v);
    }

    @Override
    public void onBindViewHolder(final PlaylistHolder holder, int position) {
        MediaDescriptionCompat item = mItems.get(position).getDescription();
        holder.title.setText(item.getTitle());
        mGlideRequest.load(item.getIconUri()).into(holder.image);

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    MediaItem clickedItem = mItems.get(holder.getAdapterPosition());
                    mListener.onPlaylistSelected(holder, clickedItem);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mItems != null ? mItems.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        if (hasStableIds() && mItems != null) {
            String mediaId = mItems.get(position).getMediaId();
            return Long.parseLong(MediaID.extractMusicID(mediaId));
        }
        return RecyclerView.NO_ID;
    }

    @Override
    public void onViewRecycled(PlaylistHolder holder) {
        Glide.clear(holder.image);
        super.onViewRecycled(holder);
    }

    void setOnPlaylistSelectedListener(OnPlaylistSelectedListener listener) {
        mListener = listener;
    }

    void update(List<MediaItem> newItems) {
        MediaItemDiffCallback diffCallback = new MediaItemDiffCallback(mItems, newItems);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(diffCallback, false);
        mItems = newItems;
        result.dispatchUpdatesTo(this);
    }

    static class PlaylistHolder extends RecyclerView.ViewHolder {
        final TextView title;
        final ImageView image;

        PlaylistHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            image = itemView.findViewById(R.id.albumArt);
        }
    }

    interface OnPlaylistSelectedListener {
        void onPlaylistSelected(PlaylistHolder holder, MediaItem playlist);
    }
}
