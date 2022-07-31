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

package fr.nihilus.music.ui.library.search

import android.graphics.Bitmap
import android.view.ViewGroup
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.ui.library.databinding.ArtistGridItemBinding
import fr.nihilus.music.ui.library.search.SearchResult

/**
 * Display an artist as a floating 16:9 card.
 */
internal class ArtistHolder(
    parent: ViewGroup,
    private val glide: RequestBuilder<Bitmap>,
    onSelect: (position: Int) -> Unit
) : BaseHolder<SearchResult.Browsable>(parent, R.layout.artist_grid_item) {

    private val binding = ArtistGridItemBinding.bind(itemView)

    init {
        itemView.setOnClickListener {
            onSelect(bindingAdapterPosition)
        }
    }

    override fun bind(data: SearchResult.Browsable) {
        binding.artistName.text = data.title
        glide.load(data.iconUri).into(binding.artistArtwork)
        binding.subtitle.text = itemView.resources.getQuantityString(
            R.plurals.number_of_tracks,
            data.tracksCount,
            data.tracksCount,
        )
    }
}
