/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.ui.library.artists

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.ui.library.databinding.ArtistGridItemBinding

/**
 * Lays out artists as cells in a grid.
 */
internal class ArtistAdapter(
    fragment: Fragment,
    private val selectArtist: (ArtistUiState) -> Unit
) : ListAdapter<ArtistUiState, ArtistAdapter.ViewHolder>(ArtistDiffer()) {

    init {
        setHasStableIds(true)
    }

    private val glide = Glide.with(fragment).asBitmap()
        .error(R.drawable.ic_person_24dp)
        .centerCrop()
        .autoClone()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(parent, glide) { position ->
            selectArtist(getItem(position))
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long = getItem(position).id.hashCode().toLong()

    class ViewHolder(
        parent: ViewGroup,
        private val glide: RequestBuilder<Bitmap>,
        onClick: (position: Int) -> Unit,
    ) : BaseHolder<ArtistUiState>(parent, R.layout.artist_grid_item) {
        private val binding = ArtistGridItemBinding.bind(itemView)

        init {
            itemView.setOnClickListener { onClick(bindingAdapterPosition) }
        }

        override fun bind(data: ArtistUiState) {
            glide.load(data.iconUri).into(binding.artistArtwork)
            binding.artistName.text = data.name
            binding.subtitle.text = itemView.resources.getQuantityString(
                R.plurals.number_of_tracks,
                data.trackCount,
                data.trackCount,
            )
        }
    }

    private class ArtistDiffer : DiffUtil.ItemCallback<ArtistUiState>() {
        override fun areItemsTheSame(
            oldItem: ArtistUiState,
            newItem: ArtistUiState
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ArtistUiState,
            newItem: ArtistUiState
        ): Boolean = oldItem == newItem
    }
}
