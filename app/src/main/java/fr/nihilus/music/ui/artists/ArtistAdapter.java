package fr.nihilus.music.ui.artists;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v7.content.res.AppCompatResources;
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
import fr.nihilus.music.media.MediaItems;
import fr.nihilus.music.utils.MediaID;
import fr.nihilus.music.utils.MediaItemDiffCallback;

class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ArtistHolder> {

    private List<MediaItem> mItems;
    private final BitmapRequestBuilder<Uri, Bitmap> mGlide;
    private OnArtistSelectedListener mListener;

    ArtistAdapter(@NonNull Context context, List<MediaItem> artists) {
        mItems = artists;
        Drawable dummyCover = AppCompatResources.getDrawable(context, R.drawable.ic_person_24dp);
        mGlide = Glide.with(context).fromUri().asBitmap()
                .error(dummyCover)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE);
    }

    @Override
    public ArtistHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.artist_grid_item, parent, false);
        return new ArtistHolder(v);
    }

    @Override
    public void onBindViewHolder(final ArtistHolder holder, int position) {
        final MediaItem artist = mItems.get(position);
        holder.artistName.setText(artist.getDescription().getTitle());
        mGlide.load(artist.getDescription().getIconUri()).into(holder.cover);

        Bundle extras = artist.getDescription().getExtras();
        if (extras != null) {
            int trackCount = extras.getInt(MediaItems.EXTRA_NUMBER_OF_TRACKS);
            String subtitle = holder.subtitle.getResources()
                    .getQuantityString(R.plurals.number_of_tracks, trackCount, trackCount);
            holder.subtitle.setText(subtitle);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    mListener.onArtistSelected(holder, mItems.get(holder.getAdapterPosition()));
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
        if (mItems != null && hasStableIds()) {
            String mediaId = mItems.get(position).getMediaId();
            return Long.parseLong(MediaID.extractMusicID(mediaId));
        }
        return RecyclerView.NO_ID;
    }

    @Override
    public void onViewRecycled(ArtistHolder holder) {
        super.onViewRecycled(holder);
        Glide.clear(holder.cover);
    }

    void setOnArtistSelectedListener(OnArtistSelectedListener listener) {
        mListener = listener;
    }

    void updateArtists(List<MediaItem> artists) {
        MediaItemDiffCallback diffCallback = new MediaItemDiffCallback(mItems, artists);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(diffCallback, false);
        mItems = artists;
        result.dispatchUpdatesTo(this);
    }

    static class ArtistHolder extends RecyclerView.ViewHolder {
        TextView artistName;
        TextView subtitle;
        ImageView cover;

        ArtistHolder(View itemView) {
            super(itemView);
            artistName = itemView.findViewById(R.id.artistName);
            subtitle = itemView.findViewById(R.id.subtitle);
            cover = itemView.findViewById(R.id.cover);
        }
    }

    interface OnArtistSelectedListener {
        void onArtistSelected(ArtistHolder holder, MediaItem artist);
    }
}
