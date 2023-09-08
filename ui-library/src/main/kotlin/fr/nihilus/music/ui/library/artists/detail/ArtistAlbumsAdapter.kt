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

package fr.nihilus.music.ui.library.artists.detail

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.ImageViewTarget
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.core.ui.glide.asAlbumArt
import fr.nihilus.music.core.ui.glide.fallbackColors
import fr.nihilus.music.core.ui.glide.palette.AlbumArt
import fr.nihilus.music.core.ui.glide.palette.AlbumPalette
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.ui.library.databinding.AlbumGridItemBinding
import fr.nihilus.music.ui.library.extensions.resolveDefaultAlbumPalette

internal class ArtistAlbumsAdapter(
    fragment: Fragment,
    private val onAlbumSelect: (ArtistAlbumUiState, ViewHolder) -> Unit
) : ListAdapter<ArtistAlbumUiState, ArtistAlbumsAdapter.ViewHolder>(AlbumDiffer()) {

    private val artworkLoader: RequestBuilder<AlbumArt>
    private val defaultPalette: AlbumPalette

    init {
        setHasStableIds(true)
        val context = fragment.requireContext()
        defaultPalette = fragment.requireContext().resolveDefaultAlbumPalette()
        val defaultAlbumIcon = ContextCompat.getDrawable(context, R.drawable.ic_album_24dp)
        artworkLoader = Glide.with(fragment).asAlbumArt()
            .disallowHardwareConfig()
            .fallbackColors(defaultPalette)
            .error(defaultAlbumIcon)
            .centerCrop()
            .autoClone()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(parent, artworkLoader, defaultPalette).also { holder ->
            holder.itemView.setOnClickListener {
                val album = getItem(holder.bindingAdapterPosition)
                onAlbumSelect(album, holder)
            }
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int = R.id.view_type_album

    override fun getItemId(position: Int): Long = getItem(position).id.category!!.toLong()

    class ViewHolder(
        parent: ViewGroup,
        private val artworkLoader: RequestBuilder<AlbumArt>,
        private val defaultPalette: AlbumPalette,
    ) : BaseHolder<ArtistAlbumUiState>(parent, R.layout.album_grid_item) {
        private val binding = AlbumGridItemBinding.bind(itemView)
        private val albumViewTarget = object : ImageViewTarget<AlbumArt>(binding.albumArtwork) {

            override fun setResource(resource: AlbumArt?) {
                if (resource != null) {
                    applyPalette(resource.palette)
                    super.view.setImageBitmap(resource.bitmap)
                }
            }

            override fun onLoadFailed(errorDrawable: Drawable?) {
                super.onLoadFailed(errorDrawable)
                applyPalette(defaultPalette)
            }
        }

        override fun bind(data: ArtistAlbumUiState) {
            artworkLoader.load(data.artworkUri).into(albumViewTarget)
            itemView.transitionName = data.id.encoded
            binding.albumTitle.text = data.title
            binding.artistName.text = itemView.resources.getQuantityString(
                R.plurals.number_of_tracks,
                data.trackCount,
                data.trackCount
            )
        }

        private fun applyPalette(palette: AlbumPalette) = with(binding) {
            card.setCardBackgroundColor(palette.primary)
            albumTitle.setTextColor(palette.titleText)
            artistName.setTextColor(palette.bodyText)
        }
    }

    class AlbumDiffer : DiffUtil.ItemCallback<ArtistAlbumUiState>() {

        override fun areItemsTheSame(
            oldItem: ArtistAlbumUiState,
            newItem: ArtistAlbumUiState
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ArtistAlbumUiState,
            newItem: ArtistAlbumUiState
        ): Boolean = oldItem == newItem
    }
}
