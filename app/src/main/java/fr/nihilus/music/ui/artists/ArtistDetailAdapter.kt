/*
 * Copyright 2018 Thibault Seisel
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
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.GlideRequest
import fr.nihilus.music.glide.palette.AlbumArt
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.albums.AlbumPalette
import fr.nihilus.music.ui.holder.AlbumHolder
import fr.nihilus.music.ui.holder.TrackHolder

internal class ArtistDetailAdapter(
    fragment: Fragment,
    private val defaultPalette: AlbumPalette,
    private val listener: BaseAdapter.OnItemSelectedListener
) : BaseAdapter<BaseAdapter.ViewHolder>() {

    private val paletteLoader: GlideRequest<AlbumArt>
    private val bitmapLoader: GlideRequest<Bitmap>

    init {
        val context = fragment.requireContext()
        val defaultAlbumIcon = context.getDrawable(R.drawable.ic_album_24dp)
        val defaultTrackIcon = context.getDrawable(R.drawable.ic_audiotrack_24dp)
        paletteLoader = GlideApp.with(fragment).asAlbumArt()
            .fallbackColors(defaultPalette)
            .fallback(defaultAlbumIcon)
            .centerCrop()
        bitmapLoader = GlideApp.with(fragment).asBitmap()
            .fallback(defaultTrackIcon)
            .centerCrop()
    }

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item.isBrowsable) R.id.view_type_album else R.id.view_type_track
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseAdapter.ViewHolder {
        return when (viewType) {
            R.id.view_type_album -> AlbumHolder(parent, paletteLoader, defaultPalette, true)
            R.id.view_type_track -> TrackHolder(parent, bitmapLoader)
            else -> error("Unexpected view type: $viewType")
        }.apply {
            onAttachListeners(listener)
        }
    }

    override fun getItemId(position: Int): Long {
        return if (hasStableIds()) {
            val mediaId = getItem(position).mediaId!!
            mediaId.hashCode().toLong()
        } else RecyclerView.NO_ID
    }

}
