/*
 * Copyright 2017 Thibault Seisel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nihilus.music.ui.albums;

import android.content.Context;
import android.graphics.drawable.Drawable;
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
import fr.nihilus.music.glide.palette.PaletteBitmap;
import fr.nihilus.music.utils.MediaID;
import fr.nihilus.music.utils.MediaItemDiffCallback;
import fr.nihilus.music.utils.ViewUtils;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

class AlbumsAdapter extends RecyclerView.Adapter<AlbumsAdapter.AlbumHolder> {

    private static final String TAG = "AlbumsAdapter";

    private final Fragment mFragment;
    @ColorInt
    private final int[] mDefaultColors;
    private final RequestBuilder<PaletteBitmap> mGlideRequest;
    private final List<MediaItem> mAlbums = new ArrayList<>();
    private OnAlbumSelectedListener mListener;

    AlbumsAdapter(@NonNull Fragment fragment) {
        mFragment = fragment;
        final Context ctx = fragment.getContext();
        mDefaultColors = new int[]{
                ContextCompat.getColor(ctx, R.color.album_band_default),
                ViewUtils.resolveThemeColor(ctx, R.attr.colorAccent),
                ContextCompat.getColor(ctx, android.R.color.white),
                ContextCompat.getColor(ctx, android.R.color.white)
        };

        Drawable dummyAlbumArt = ContextCompat.getDrawable(ctx, R.drawable.ic_album_24dp);
        mGlideRequest = GlideApp.with(fragment).as(PaletteBitmap.class)
                .centerCrop()
                .error(dummyAlbumArt)
                .region(0f, .75f, 1f, 1f);
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

        holder.itemView.setOnClickListener(view -> {
            if (mListener != null) {
                final int clickedPosition = holder.getAdapterPosition();
                mListener.onAlbumSelected(holder, mAlbums.get(clickedPosition));
            }
        });
    }

    @Override
    public long getItemId(int position) {
        if (hasStableIds() && mAlbums != null) {
            final MediaItem item = mAlbums.get(position);
            return Long.parseLong(MediaID.extractMusicID(item.getMediaId()));
        }
        return RecyclerView.NO_ID;
    }

    @Override
    public int getItemCount() {
        return mAlbums != null ? mAlbums.size() : 0;
    }

    @Override
    public void onViewRecycled(AlbumHolder holder) {
        super.onViewRecycled(holder);
        Glide.with(mFragment).clear(holder.albumArt);
    }

    void setOnAlbumSelectedListener(OnAlbumSelectedListener listener) {
        mListener = listener;
    }

    void updateAlbums(final List<MediaItem> newAlbums) {
        Single.fromCallable(() -> {
            MediaItemDiffCallback callback = new MediaItemDiffCallback(mAlbums, newAlbums);
            return DiffUtil.calculateDiff(callback, false);
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(result -> {
                    mAlbums.clear();
                    mAlbums.addAll(newAlbums);
                    result.dispatchUpdatesTo(AlbumsAdapter.this);
                });
    }

    interface OnAlbumSelectedListener {
        void onAlbumSelected(AlbumHolder holder, MediaItem album);
    }

    static class AlbumHolder extends RecyclerView.ViewHolder {
        final CardView card;
        final ImageView albumArt;
        final TextView title;
        final TextView artist;

        @ColorInt
        final int[] colors = new int[4];

        AlbumHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card);
            albumArt = itemView.findViewById(R.id.cover);
            title = itemView.findViewById(R.id.title);
            artist = itemView.findViewById(R.id.artist);
        }

        void setColors(@ColorInt int primary, @ColorInt int accent, @ColorInt int title,
                       @ColorInt int body) {
            this.card.setCardBackgroundColor(primary);
            this.title.setTextColor(body);
            artist.setTextColor(body);
            colors[0] = primary;
            colors[1] = accent;
            colors[2] = title;
            colors[3] = body;
        }
    }
}
