package fr.nihilus.music.ui.artists;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.graphics.Palette;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.request.target.ImageViewTarget;

import java.util.ArrayList;
import java.util.List;

import fr.nihilus.music.R;
import fr.nihilus.music.glide.GlideApp;
import fr.nihilus.music.glide.PaletteBitmap;
import fr.nihilus.music.media.MediaItems;
import fr.nihilus.music.utils.MediaItemDiffCallback;
import fr.nihilus.music.utils.ViewUtils;

class ArtistDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ALBUM = 1;
    private static final int TYPE_TRACK = 0;

    private final Fragment mFragment;
    private final RequestBuilder<PaletteBitmap> mPaletteLoader;
    private final RequestBuilder<Bitmap> mBitmapLoader;
    private final int[] mDefaultColors;
    private final List<MediaItem> mItems = new ArrayList<>();
    private OnMediaItemSelectedListener mListener;

    ArtistDetailAdapter(@NonNull Fragment fragment) {
        mFragment = fragment;
        Context ctx = fragment.getContext();
        mDefaultColors = new int[]{
                ContextCompat.getColor(ctx, R.color.album_band_default),
                ViewUtils.resolveThemeColor(ctx, R.attr.colorAccent),
                ContextCompat.getColor(ctx, android.R.color.white),
                ContextCompat.getColor(ctx, android.R.color.white)
        };

        Drawable dummyAlbumArt = ContextCompat.getDrawable(ctx, R.drawable.ic_album_24dp);
        mPaletteLoader = GlideApp.with(fragment).as(PaletteBitmap.class)
                .centerCrop()
                .error(dummyAlbumArt)
                .region(0f, .75f, 1f, 1f);
        mBitmapLoader = GlideApp.with(fragment).asBitmap()
                .centerCrop()
                .error(dummyAlbumArt);
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
        if (hasStableIds()) {
            String mediaId = mItems.get(position).getMediaId();
            return mediaId.hashCode();
        }
        return RecyclerView.NO_ID;
    }

    @Override
    public int getItemCount() {
        return mItems.size();
    }

    @Override
    public void onViewRecycled(RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder.getItemViewType() == TYPE_ALBUM) {
            Glide.with(mFragment).clear(((AlbumHolder) holder).albumArt);
        }
    }

    void setOnMediaItemSelectedListener(@Nullable OnMediaItemSelectedListener listener) {
        mListener = listener;
    }

    MediaItem get(int position) {
        return mItems.get(position);
    }

    void updateItems(List<MediaItem> newItems) {
        MediaItemDiffCallback callback = new MediaItemDiffCallback(mItems, newItems);
        DiffUtil.DiffResult result = DiffUtil.calculateDiff(callback, false);
        mItems.clear();
        mItems.addAll(newItems);
        result.dispatchUpdatesTo(this);
    }

    private void bindAlbumHolder(final AlbumHolder holder, int position) {
        final MediaDescriptionCompat item = mItems.get(position).getDescription();
        holder.title.setText(item.getTitle());

        ViewCompat.setTransitionName(holder.albumArt, "art_" + item.getMediaId());
        holder.setColors(mDefaultColors[0], mDefaultColors[1], mDefaultColors[2], mDefaultColors[3]);

        mPaletteLoader.load(item.getIconUri()).into(new ImageViewTarget<PaletteBitmap>(holder.albumArt) {
            @Override
            protected void setResource(@Nullable PaletteBitmap resource) {
                if (resource != null) {
                    super.view.setImageBitmap(resource.getBitmap());
                    Palette.Swatch swatch = resource.getPalette().getDominantSwatch();
                    int accentColor = resource.getPalette().getVibrantColor(mDefaultColors[1]);
                    if (swatch != null) {
                        holder.setColors(swatch.getRgb(), accentColor,
                                swatch.getTitleTextColor(), swatch.getBodyTextColor());
                    }
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
        final Bundle extras = item.getExtras();
        holder.title.setText(item.getTitle());

        if (extras != null) {
            long duration = extras.getLong(MediaItems.EXTRA_DURATION);
            holder.duration.setText(formatDuration(duration));
        }

        mBitmapLoader.load(item.getIconUri()).into(holder.albumArt);

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

    private static String formatDuration(long duration) {
        return DateUtils.formatElapsedTime(duration / 1000);
    }

    interface OnMediaItemSelectedListener {
        void onAlbumSelected(AlbumHolder holder, MediaItem album);

        void onTrackSelected(TrackHolder holder, MediaItem track);
    }

    static class AlbumHolder extends RecyclerView.ViewHolder {
        @ColorInt
        final int[] colors = new int[4];
        final CardView card;
        final ImageView albumArt;
        final TextView title;

        AlbumHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            albumArt = itemView.findViewById(R.id.albumArt);
            title = itemView.findViewById(R.id.title);
        }

        void setColors(@ColorInt int primary, @ColorInt int accent, @ColorInt int title,
                       @ColorInt int body) {
            card.setCardBackgroundColor(primary);
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
