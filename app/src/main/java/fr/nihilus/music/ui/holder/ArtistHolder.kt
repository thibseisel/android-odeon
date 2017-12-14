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

package fr.nihilus.music.ui.holder

import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.R
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.ui.BaseAdapter

/**
 * Display an artist as a floating 16:9 card.
 */
internal class ArtistHolder(
        parent: ViewGroup,
        private val glide: RequestBuilder<Bitmap>
) : BaseAdapter.ViewHolder(parent, R.layout.artist_grid_item) {

    private val artistName: TextView = itemView.findViewById(R.id.artistName)
    private val subtitle: TextView = itemView.findViewById(R.id.subtitleView)
    private val cover: ImageView = itemView.findViewById(R.id.albumArtView)

    override fun onAttachListeners(client: BaseAdapter.OnItemSelectedListener) {
        itemView.setOnClickListener { _ ->
            client.onItemSelected(adapterPosition, R.id.action_browse_item)
        }
    }

    override fun onBind(item: MediaBrowserCompat.MediaItem) {
        artistName.text = item.description.title
        glide.load(item.description.iconUri).into(cover)

        item.description.extras?.let {
            val trackCount = it.getInt(MediaItems.EXTRA_NUMBER_OF_TRACKS)
            subtitle.text = subtitle.resources.getQuantityString(R.plurals.number_of_tracks,
                    trackCount, trackCount)
        }
    }
}