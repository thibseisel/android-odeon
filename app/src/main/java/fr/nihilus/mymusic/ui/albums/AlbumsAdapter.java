package fr.nihilus.mymusic.ui.albums;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
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

import java.util.ArrayList;
import java.util.List;

import fr.nihilus.mymusic.R;
import fr.nihilus.mymusic.palette.PaletteBitmap;
import fr.nihilus.mymusic.palette.PaletteBitmapTranscoder;
import fr.nihilus.mymusic.utils.MediaID;
import fr.nihilus.mymusic.utils.MediaItemDiffCallback;
import fr.nihilus.mymusic.utils.ViewUtils;

class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.AlbumHolder> {

    private static final String TAG = "AlbumsAdapter";

    @ColorInt
    private final int[] mDefaultColors;
    private final BitmapRequestBuilder<Uri, PaletteBitmap> mGlideRequest;
    private List<MediaItem> mAlbums;
    private OnAlbumSelectedListener mListener;

    AlbumsAdapter(@NonNull Context context, @NonNull ArrayList<MediaItem> items) {
        mAlbums = items;
        mDefaultColors = new int[]{
                ContextCompat.getColor(context, R.color.album_band_default),
                ViewUtils.resolveThemeColor(context, R.attr.colorAccent),
                ContextCompat.getColor(context, android.R.color.white),
                ContextCompat.getColor(context, android.R.color.white)
        };
        Drawable dummyAlbumArt = ContextCompat.getDrawable(context, R.drawable.ic_album_24dp);
        mGlideRequest = Glide.with(context)
                .fromUri()
                .asBitmap()
                .transcode(new BottomPaletteTranscoder(context), PaletteBitmap.class)
                .fitCenter()
                .error(dummyAlbumArt)
                .diskCacheStrategy(DiskCacheStrategy.RESULT);
    }

    @Override
    public AlbumHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.album_grid_item, parent, false);
        return new AlbumHolder(v);
    }

    @Override
    public void onBindViewHolder(final AlbumHolder holder, int position) {
        MediaDescriptionCompat item = mAlbums.get(position).getDescription();
        holder.title.setText(item.getTitle());
        holder.artist.setText(item.getSubtitle());

        ViewCompat.setTransitionName(holder.albumArt, "image_" + item.getMediaId());
        holder.setColors(mDefaultColors[0], mDefaultColors[1], mDefaultColors[2], mDefaultColors[3]);

        mGlideRequest.load(item.getIconUri())
                .into(new ImageViewTarget<PaletteBitmap>(holder.albumArt) {
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
            public void onClick(View view) {
                if (mListener != null) {
                    final int clickedPosition = holder.getAdapterPosition();
                    mListener.onAlbumSelected(mAlbums.get(clickedPosition), holder);
                }
            }
        });
    }

    @Override
    public void onViewRecycled(AlbumHolder holder) {
        super.onViewRecycled(holder);
        Glide.clear(holder.albumArt);
    }

    @Override
    public int getItemCount() {
        return mAlbums != null ? mAlbums.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        if (hasStableIds() && mAlbums != null) {
            final MediaItem item = mAlbums.get(position);
            return Long.parseLong(MediaID.extractMusicIDFromMediaID(item.getMediaId()));
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
        void onAlbumSelected(MediaItem album, AlbumHolder holder);
    }

    static class AlbumHolder extends RecyclerView.ViewHolder {

        final View band;
        final ImageView albumArt;
        final TextView title;
        final TextView artist;

        @ColorInt
        final int[] colors = new int[4];

        AlbumHolder(View itemView) {
            super(itemView);
            band = itemView.findViewById(R.id.band);
            albumArt = (ImageView) itemView.findViewById(R.id.albumArt);
            title = (TextView) itemView.findViewById(R.id.title);
            artist = (TextView) itemView.findViewById(R.id.artist);
        }

        void setColors(@ColorInt int primary, @ColorInt int accent, @ColorInt int title,
                       @ColorInt int body) {
            this.band.setBackgroundColor(primary);
            this.title.setTextColor(body);
            artist.setTextColor(body);
            colors[0] = primary;
            colors[1] = accent;
            colors[2] = title;
            colors[3] = body;
        }
    }

    private static class BottomPaletteTranscoder extends PaletteBitmapTranscoder {

        BottomPaletteTranscoder(@NonNull Context context) {
            super(context);
        }

        @NonNull
        @Override
        protected Palette onGeneratePalette(Bitmap bitmap) {
            return Palette.from(bitmap)
                    .setRegion(0, bitmap.getHeight() / 5, bitmap.getWidth(), bitmap.getHeight())
                    .maximumColorCount(16)
                    .generate();
        }
    }
}