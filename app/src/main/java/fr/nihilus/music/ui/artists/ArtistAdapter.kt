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
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.Constants
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.utils.MediaID

internal class ArtistAdapter(
        fragment: Fragment,
        private val listener: BaseAdapter.OnItemSelectedListener
) : BaseAdapter<ArtistAdapter.ArtistHolder>() {

    private val glide: RequestBuilder<Bitmap>

    init {
        val ctx = checkNotNull(fragment.context) { "Fragment is not attached" }
        val dummyCover = AppCompatResources.getDrawable(ctx, R.drawable.ic_person_24dp)

        glide = GlideApp.with(fragment).asBitmap()
                .error(dummyCover)
                .centerCrop()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistHolder {
        return ArtistHolder(parent, glide).also { holder ->
            holder.onAttachListeners(listener)
        }
    }

    override fun getItemId(position: Int): Long {
        if (hasStableIds()) {
            val mediaId = items[position].mediaId
            return MediaID.extractMusicID(mediaId)?.toLong() ?: RecyclerView.NO_ID
        }
        return RecyclerView.NO_ID
    }

    /**
     * Display an artist as a floating 16:9 card.
     */
    internal class ArtistHolder(
            parent: ViewGroup,
            private val glide: RequestBuilder<Bitmap>
    ) : BaseAdapter.ViewHolder(parent, R.layout.artist_grid_item) {

        private val artistName: TextView = itemView.findViewById(R.id.artistName)
        private val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        private val cover: ImageView = itemView.findViewById(R.id.cover)

        override fun onAttachListeners(client: BaseAdapter.OnItemSelectedListener) {
            itemView.setOnClickListener { _ ->
                client.onItemSelected(adapterPosition, Constants.ACTION_BROWSE)
            }
        }

        override fun onBind(item: MediaItem) {
            artistName.text = item.description.title
            glide.load(item.description.iconUri).into(cover)

            item.description.extras?.let {
                val trackCount = it.getInt(MediaItems.EXTRA_NUMBER_OF_TRACKS)
                subtitle.text = subtitle.resources.getQuantityString(R.plurals.number_of_tracks,
                        trackCount, trackCount)
            }
        }
    }
}
