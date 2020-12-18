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

package fr.nihilus.music.library.artists.detail

import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.core.ui.base.MediaItemDiffer
import fr.nihilus.music.core.ui.glide.GlideApp
import fr.nihilus.music.core.ui.glide.GlideRequest
import fr.nihilus.music.core.ui.glide.palette.AlbumArt
import fr.nihilus.music.extensions.resolveDefaultAlbumPalette
import fr.nihilus.music.library.albums.AlbumHolder

internal class ArtistDetailAdapter(
    fragment: Fragment,
    private val selectionListener: SelectionListener
) : ListAdapter<MediaItem, BaseHolder<MediaItem>>(MediaItemDiffer) {

    private val paletteLoader: GlideRequest<AlbumArt>
    private val bitmapLoader: RequestBuilder<Bitmap>
    private val defaultPalette = fragment.requireContext().resolveDefaultAlbumPalette()

    init {
        val context = fragment.requireContext()
        val defaultAlbumIcon = ContextCompat.getDrawable(context, R.drawable.ic_album_24dp)
        val defaultTrackIcon = ContextCompat.getDrawable(context, R.drawable.ic_audiotrack_24dp)
        paletteLoader = GlideApp.with(fragment).asAlbumArt()
            .disallowHardwareConfig()
            .fallbackColors(defaultPalette)
            .error(defaultAlbumIcon)
            .centerCrop()
        bitmapLoader = Glide.with(fragment).asBitmap()
            .error(defaultTrackIcon)
            .centerCrop()
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item.isBrowsable) R.id.view_type_album else R.id.view_type_track
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseHolder<MediaItem> {
        return when (viewType) {
            R.id.view_type_album -> AlbumHolder(parent, paletteLoader, defaultPalette, isArtistAlbum = true) { albumPosition ->
                selectionListener.onAlbumSelected(albumPosition)
            }

            R.id.view_type_track -> ArtistTrackHolder(parent, bitmapLoader) { trackPosition ->
                selectionListener.onTrackSelected(trackPosition)
            }

            else -> error("Unexpected view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: BaseHolder<MediaItem>, position: Int) {
        holder.bind(getItem(position))
    }

    public override fun getItem(position: Int): MediaItem = super.getItem(position)

    interface SelectionListener {
        fun onAlbumSelected(position: Int)
        fun onTrackSelected(position: Int)
    }
}
