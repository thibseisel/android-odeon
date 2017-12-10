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

package fr.nihilus.music.ui.artists

import android.graphics.Bitmap
import android.support.v4.app.Fragment
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v7.content.res.AppCompatResources
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.utils.MediaID
import fr.nihilus.music.utils.MediaItemDiffCallback
import java.util.*

internal class ArtistAdapter(
        fragment: Fragment
) : RecyclerView.Adapter<ArtistAdapter.ArtistHolder>() {

    private val mItems = ArrayList<MediaItem>()
    private val mGlide: RequestBuilder<Bitmap>
    private var mListener: OnArtistSelectedListener? = null

    init {
        val ctx = checkNotNull(fragment.context) { "Fragment is not attached" }
        val dummyCover = AppCompatResources.getDrawable(ctx, R.drawable.ic_person_24dp)

        mGlide = GlideApp.with(fragment).asBitmap()
                .error(dummyCover)
                .centerCrop()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistHolder {
        val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.artist_grid_item, parent, false)
        return ArtistHolder(v)
    }

    override fun onBindViewHolder(holder: ArtistHolder, position: Int) {
        val artist = mItems[position]
        holder.bind(artist, mGlide)

        holder.itemView.setOnClickListener { _ ->
            mListener?.onArtistSelected(holder, mItems[holder.adapterPosition])
        }
    }

    override fun getItemId(position: Int): Long {
        if (hasStableIds()) {
            val mediaId = mItems[position].mediaId
            return java.lang.Long.parseLong(MediaID.extractMusicID(mediaId))
        }
        return RecyclerView.NO_ID
    }

    override fun getItemCount() = mItems.size

    fun setOnArtistSelectedListener(listener: OnArtistSelectedListener) {
        mListener = listener
    }

    fun updateArtists(artists: List<MediaItem>) {
        if (artists.isEmpty() && mItems.isEmpty()) {
            // Dispatch a general change notification to update RecyclerFragment's empty state
            notifyDataSetChanged()
        } else {
            // Calculate diff and dispatch individual change notifications
            val diffCallback = MediaItemDiffCallback(mItems, artists)
            val result = DiffUtil.calculateDiff(diffCallback, false)
            mItems.clear()
            mItems.addAll(artists)
            result.dispatchUpdatesTo(this)
        }
    }

    interface OnArtistSelectedListener {
        fun onArtistSelected(holder: ArtistHolder, artist: MediaItem)
    }

    internal class ArtistHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val artistName: TextView = itemView.findViewById(R.id.artistName)
        private val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        private val cover: ImageView = itemView.findViewById(R.id.cover)

        fun bind(artist: MediaItem, glide: RequestBuilder<*>) {
            artistName.text = artist.description.title
            glide.load(artist.description.iconUri).into(cover)

            artist.description.extras?.let {
                val trackCount = it.getInt(MediaItems.EXTRA_NUMBER_OF_TRACKS)
                subtitle.text = subtitle.resources.getQuantityString(R.plurals.number_of_tracks,
                        trackCount, trackCount)
            }
        }
    }
}
