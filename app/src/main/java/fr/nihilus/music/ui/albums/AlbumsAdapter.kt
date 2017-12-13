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

package fr.nihilus.music.ui.albums

import android.support.annotation.ColorInt
import android.support.v4.app.Fragment
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.view.ViewCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.ImageViewTarget
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.palette.PaletteBitmap
import fr.nihilus.music.utils.MediaID
import fr.nihilus.music.utils.MediaItemDiffCallback
import fr.nihilus.music.utils.resolveThemeColor
import java.util.*

class AlbumsAdapter(
        private val fragment: Fragment
) : RecyclerView.Adapter<AlbumsAdapter.AlbumHolder>() {

    private val mGlideRequest: RequestBuilder<PaletteBitmap>
    private val mAlbums = ArrayList<MediaItem>()
    private var mListener: OnAlbumSelectedListener? = null
    private val mDefaultColors: IntArray

    init {
        val ctx = checkNotNull(fragment.context) { "Fragment is not attached" }
        mDefaultColors = intArrayOf(
                ContextCompat.getColor(ctx, R.color.album_band_default),
                resolveThemeColor(ctx, R.attr.colorAccent),
                ContextCompat.getColor(ctx, android.R.color.white),
                ContextCompat.getColor(ctx, android.R.color.white)
        )

        val dummyAlbumArt = ContextCompat.getDrawable(ctx, R.drawable.ic_album_24dp)
        mGlideRequest = GlideApp.with(fragment).`as`(PaletteBitmap::class.java)
                .centerCrop()
                .error(dummyAlbumArt)
                .region(0f, .75f, 1f, 1f)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.album_grid_item, parent, false)

        return AlbumHolder(v).also { holder ->
            holder.itemView.setOnClickListener { _ ->
                mListener?.let {
                    val album = mAlbums[holder.adapterPosition]
                    it.onAlbumSelected(holder, album)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: AlbumHolder, position: Int) {
        val item = mAlbums[position].description
        holder.title.text = item.title
        holder.artist.text = item.subtitle

        ViewCompat.setTransitionName(holder.albumArt, "image_" + item.mediaId!!)
        holder.setColors(mDefaultColors[0], mDefaultColors[1], mDefaultColors[2], mDefaultColors[3])

        mGlideRequest.load(item.iconUri)
                .into(object : ImageViewTarget<PaletteBitmap>(holder.albumArt) {
                    override fun setResource(resource: PaletteBitmap?) {
                        if (resource != null) {
                            super.view.setImageBitmap(resource.bitmap)
                            val swatch = resource.palette.dominantSwatch
                            val accentColor = resource.palette.getVibrantColor(mDefaultColors[1])
                            if (swatch != null) {
                                holder.setColors(swatch.rgb, accentColor,
                                        swatch.titleTextColor, swatch.bodyTextColor)
                            }
                        }
                    }
                })
    }

    override fun getItemId(position: Int): Long {
        if (hasStableIds()) {
            val item = mAlbums[position]
            return MediaID.extractMusicID(item.mediaId)?.toLong() ?: RecyclerView.NO_ID
        }
        return RecyclerView.NO_ID
    }

    override fun getItemCount() = mAlbums.size

    override fun onViewRecycled(holder: AlbumHolder?) {
        super.onViewRecycled(holder)
        Glide.with(fragment).clear(holder!!.albumArt)
    }

    fun setOnAlbumSelectedListener(listener: OnAlbumSelectedListener) {
        mListener = listener
    }

    fun updateAlbums(newAlbums: List<MediaItem>) {
        if (newAlbums.isEmpty() && mAlbums.isEmpty()) {
            // Dispatch a general change notification to update RecyclerFragment's empty state
            notifyDataSetChanged()
        } else {
            // Calculate diff and dispatch individual changes
            val callback = MediaItemDiffCallback(mAlbums, newAlbums)
            val result = DiffUtil.calculateDiff(callback, false)
            mAlbums.clear()
            mAlbums.addAll(newAlbums)
            result.dispatchUpdatesTo(this)
        }
    }

    interface OnAlbumSelectedListener {
        fun onAlbumSelected(holder: AlbumHolder, album: MediaItem)
    }

    class AlbumHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val card: CardView = itemView.findViewById(R.id.card)
        val albumArt: ImageView = itemView.findViewById(R.id.cover)
        val title: TextView = itemView.findViewById(R.id.title)
        val artist: TextView = itemView.findViewById(R.id.artist)

        @ColorInt
        val colors = IntArray(4)

        fun setColors(@ColorInt primary: Int, @ColorInt accent: Int, @ColorInt title: Int,
                      @ColorInt body: Int) {
            this.card.setCardBackgroundColor(primary)
            this.title.setTextColor(body)
            artist.setTextColor(body)
            colors[0] = primary
            colors[1] = accent
            colors[2] = title
            colors[3] = body
        }
    }
}
