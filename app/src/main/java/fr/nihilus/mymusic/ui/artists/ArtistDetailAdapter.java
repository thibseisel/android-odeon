package fr.nihilus.mymusic.ui.artists;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.Palette;
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
import com.bumptech.glide.request.target.ImageViewTarget;

import java.util.List;

import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.palette.BottomPaletteTranscoder;
import fr.nihilus.mymusic.palette.PaletteBitmap;
import fr.nihilus.mymusic.utils.MediaItemDiffCallback;
import fr.nihilus.mymusic.utils.ViewUtils;

class ArtistDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ALBUM = 1;
    private static final int TYPE_TRACK = 0;

    private final BitmapRequestBuilder<Uri, PaletteBitmap> mGlide;
    private final int[] mDefaultColors;
    private List<MediaItem> mItems;
    private OnMediaItemSelectedListener mListener;

    ArtistDetailAdapter(@NonNull Context context, List<MediaItem> items) {
        mItems = items;
        mDefaultColors = new int[]{
                ContextCompat.getColor(context, R.color.album_band_default),
                ViewUtils.resolveThemeColor(context, R.attr.colorAccent),
                ContextCompat.getColor(context, android.R.color.white),
                ContextCompat.getColor(context, android.R.color.white)
        };
        Drawable dummyAlbumArt = ContextCompat.getDrawable(context, R.drawable.ic_album_24dp);
        mGlide = Glide.with(context).fromUri().asBitmap()
                .transcode(new BottomPaletteTranscoder(context), PaletteBitmap.class)
                .centerCrop()
                .error(dummyAlbumArt)
                .diskCacheStrategy(DiskCacheStrategy.NONE);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ALBUM) {
            View v = inflater.inflate(R.layout.artist_album_item, parent, false);
            return new AlbumHolder(v);
        } else {
            View v = inflater.inflate(R.layout.artist_track_item, parent, false);
            return new TrackHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == TYPE_ALBUM) {
            bindAlbumHolder(((AlbumHolder) holder), position);
        } else {
            bindTrackHolder(((TrackHolder) holder), position);
        }
    }

    @Override
    public int getItemViewType(int position) {
        Bundle extras = mItems.get(position).getDescription().getExtras();
        if (extras != null) {
            boolean isAlbum = extras.getString(AlbumColumns.ALBUM_KEY, null) != null;
            return isAlbum ? TYPE_ALBUM : TYPE_TRACK;
        }
        return TYPE_TRACK;
    }

    @Override
    public long getItemId(int position) {
        if (hasStableIds() && mItems != null) {
            String mediaId = mItems.get(position).getMediaId();
            return mediaId.hashCode();
        }
        return RecyclerView.NO_ID;
    }

    @Override
    public int getItemCount() {
        return mItems != null ? mItems.size() : 0;
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.getItemViewType() == TYPE_ALBUM) {
            Glide.clear(((AlbumHolder) holder).albumArt);
        }
    }

    void setOnMediaItemSelectedListener(@Nullable OnMediaItemSelectedListener listener) {
        mListener = listener;
    }

    void updateItems(List<MediaItem> newItems) {
        MediaItemDiffCallback callback = new MediaItemDiffCallback(mItems, newItems);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback, false);
        mItems = newItems;
        result.dispatchUpdatesTo(this);
    }

    private void bindAlbumHolder(final AlbumHolder holder, int position) {
        final MediaDescriptionCompat item = mItems.get(position).getDescription();
        holder.title.setText(item.getTitle());

        ViewCompat.setTransitionName(holder.albumArt, "art_" + item.getMediaId());
        holder.setColors(mDefaultColors[0], mDefaultColors[1], mDefaultColors[2], mDefaultColors[3]);

        mGlide.load(item.getIconUri()).into(new ImageViewTarget<PaletteBitmap>(holder.albumArt) {
            @Override
            protected void setResource(PaletteBitmap resource) {
                super.view.setImageBitmap(resource.bitmap);
                Palette.Swatch swatch = resource.palette.getDominantSwatch();
                int accentColor = resource.palette.getVibrantColor(mDefaultColors[1]);
                if (swatch != null) {
                    holder.setColors(swatch.getRgb(), accentColor,
                            swatch.getTitleTextColor(), swatch.getBodyTextColor());
                }
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    int position = holder.getAdapterPosition();
                    mListener.onAlbumSelected(holder, mItems.get(position));
                }
            }
        });
    }

    private void bindTrackHolder(final TrackHolder holder, int position) {
        final MediaDescriptionCompat item = mItems.get(position).getDescription();
        holder.title.setText(item.getTitle());
        holder.duration.setText(item.getSubtitle());

        mGlide.load(item.getIconUri()).into(new ImageViewTarget<PaletteBitmap>(holder.albumArt) {
            @Override
            protected void setResource(PaletteBitmap resource) {
                super.view.setImageBitmap(resource.bitmap);
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mListener != null) {
                    int position = holder.getAdapterPosition();
                    mListener.onTrackSelected(holder, mItems.get(position));
                }
            }
        });
    }

    interface OnMediaItemSelectedListener {
        void onAlbumSelected(AlbumHolder holder, MediaItem album);

        void onTrackSelected(TrackHolder holder, MediaItem track);
    }

    static class AlbumHolder extends RecyclerView.ViewHolder {
        @ColorInt
        final int[] colors = new int[4];
        ImageView albumArt;
        TextView title;

        AlbumHolder(View itemView) {
            super(itemView);
            albumArt = itemView.findViewById(R.id.albumArt);
            title = itemView.findViewById(R.id.title);
        }

        void setColors(@ColorInt int primary, @ColorInt int accent, @ColorInt int title,
                       @ColorInt int body) {
            this.title.setBackgroundColor(primary);
            this.title.setTextColor(body);
            colors[0] = primary;
            colors[1] = accent;
            colors[2] = title;
            colors[3] = body;
        }
    }

    static class TrackHolder extends RecyclerView.ViewHolder {
        final ImageView albumArt;
        final TextView title;
        final TextView duration;

        TrackHolder(View itemView) {
            super(itemView);
            albumArt = itemView.findViewById(R.id.albumArt);
            title = itemView.findViewById(R.id.title);
            duration = itemView.findViewById(R.id.duration);
        }
    }
}
