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

package fr.nihilus.music.library.albums

import android.graphics.drawable.Drawable
import android.support.v4.media.MediaBrowserCompat
import android.view.ViewGroup
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.ImageViewTarget
import fr.nihilus.music.R
import fr.nihilus.music.core.media.MediaItems
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.core.ui.glide.palette.AlbumArt
import fr.nihilus.music.core.ui.glide.palette.AlbumPalette
import fr.nihilus.music.databinding.AlbumGridItemBinding

internal class AlbumHolder(
    parent: ViewGroup,
    private val glide: RequestBuilder<AlbumArt>,
    private val defaultPalette: AlbumPalette,
    private val isArtistAlbum: Boolean,
    onAlbumSelected: (position: Int) -> Unit
) : BaseHolder<MediaBrowserCompat.MediaItem>(parent, R.layout.album_grid_item) {

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
            onAlbumSelected(adapterPosition)
        }
    }

    private fun applyPalette(palette: AlbumPalette) = with(binding) {
        card.setCardBackgroundColor(palette.primary)
        albumTitle.setTextColor(palette.titleText)
        artistName.setTextColor(palette.bodyText)
    }

    override fun bind(data: MediaBrowserCompat.MediaItem) {
        val description = data.description
        itemView.transitionName = description.mediaId
        binding.albumTitle.text = description.title

        binding.artistName.text = if (isArtistAlbum) {
            val trackNb = description.extras!!.getInt(MediaItems.EXTRA_NUMBER_OF_TRACKS)
            itemView.resources.getQuantityString(R.plurals.number_of_tracks, trackNb, trackNb)
        } else {
            description.subtitle
        }

        glide.load(description.iconUri).into(albumViewTarget)
    }
}