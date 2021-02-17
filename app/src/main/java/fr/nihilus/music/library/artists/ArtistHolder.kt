/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.library.artists

import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat
import android.view.ViewGroup
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.R
import fr.nihilus.music.core.media.MediaItems
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.databinding.ArtistGridItemBinding

/**
 * Display an artist as a floating 16:9 card.
 */
internal class ArtistHolder(
    parent: ViewGroup,
    private val glide: RequestBuilder<Bitmap>,
    onArtistSelected: (position: Int) -> Unit
) : BaseHolder<MediaBrowserCompat.MediaItem>(parent, R.layout.artist_grid_item) {

    private val binding = ArtistGridItemBinding.bind(itemView)

    init {
        itemView.setOnClickListener {
            onArtistSelected(adapterPosition)
        }
    }

    override fun bind(data: MediaBrowserCompat.MediaItem) {
        binding.artistName.text = data.description.title
        glide.load(data.description.iconUri).into(binding.artistArtwork)

        data.description.extras?.let {
            val trackCount = it.getInt(MediaItems.EXTRA_NUMBER_OF_TRACKS)
            binding.subtitle.text = itemView.resources.getQuantityString(
                R.plurals.number_of_tracks,
                trackCount, trackCount
            )
        }
    }
}