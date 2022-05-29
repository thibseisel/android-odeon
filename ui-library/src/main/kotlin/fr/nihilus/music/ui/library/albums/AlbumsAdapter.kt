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

package fr.nihilus.music.ui.library.albums

import android.graphics.drawable.Drawable
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.ImageViewTarget
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.core.ui.glide.GlideApp
import fr.nihilus.music.core.ui.glide.GlideRequest
import fr.nihilus.music.core.ui.glide.palette.AlbumArt
import fr.nihilus.music.core.ui.glide.palette.AlbumPalette
import fr.nihilus.music.ui.library.databinding.AlbumGridItemBinding
import fr.nihilus.music.ui.library.extensions.resolveDefaultAlbumPalette

/**
 * Lays out a list of albums as cells in a grid.
 */
internal class AlbumsAdapter(
    fragment: Fragment,
    private val browseAlbumAt: (Int) -> Unit
) : ListAdapter<AlbumUiState, AlbumsAdapter.ViewHolder>(AlbumDiffer()) {

    private val defaultPalette = fragment.requireContext().resolveDefaultAlbumPalette()
    private val glideRequest: GlideRequest<AlbumArt>

    init {
        setHasStableIds(true)
        val context = fragment.requireContext()
        val fallbackIcon = ContextCompat.getDrawable(context, R.drawable.ic_album_24dp)
        glideRequest = GlideApp.with(fragment).asAlbumArt()
            .disallowHardwareConfig()
            .fallbackColors(defaultPalette)
            .error(fallbackIcon)
            .dontTransform()
            .autoClone()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(parent, glideRequest, defaultPalette, browseAlbumAt)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    public override fun getItem(position: Int): AlbumUiState = super.getItem(position)

    override fun getItemId(position: Int): Long =
        getItem(position).id.hashCode().toLong()

    class ViewHolder(
        parent: ViewGroup,
        private val glide: RequestBuilder<AlbumArt>,
        private val defaultPalette: AlbumPalette,
        onClick: (Int) -> Unit
    ) : BaseHolder<AlbumUiState>(parent, R.layout.album_grid_item) {

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

        init {
            itemView.setOnClickListener {
                onClick(bindingAdapterPosition)
            }
        }

        override fun bind(data: AlbumUiState) = with(binding) {
            glide.load(data.artworkUri).into(albumViewTarget)
            itemView.transitionName = data.id.encoded
            albumTitle.text = data.title
            artistName.text = data.artist
        }

        private fun applyPalette(palette: AlbumPalette) = with(binding) {
            card.setCardBackgroundColor(palette.primary)
            albumTitle.setTextColor(palette.titleText)
            artistName.setTextColor(palette.bodyText)
        }
    }

    private class AlbumDiffer : DiffUtil.ItemCallback<AlbumUiState>() {
        override fun areItemsTheSame(
            oldItem: AlbumUiState,
            newItem: AlbumUiState
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: AlbumUiState,
            newItem: AlbumUiState
        ): Boolean = oldItem == newItem
    }
}
