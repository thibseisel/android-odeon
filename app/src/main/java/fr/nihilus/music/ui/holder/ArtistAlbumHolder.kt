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

package fr.nihilus.music.ui.holder

import android.support.v4.media.MediaBrowserCompat
import android.support.v7.widget.CardView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.ImageViewTarget
import fr.nihilus.music.R
import fr.nihilus.music.glide.palette.AlbumArt
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.albums.AlbumPalette

internal class ArtistAlbumHolder(
    parent: ViewGroup,
    private val glide: RequestBuilder<AlbumArt>,
    private val defaultPalette: AlbumPalette
) : BaseAdapter.ViewHolder(parent, R.layout.artist_album_item) {

    private val card: CardView = itemView.findViewById(R.id.card)

    private val albumArt: ImageView = itemView.findViewById(R.id.albumArt)
    private val title: TextView = itemView.findViewById(R.id.title)

    val transitionView: View get() = albumArt
    var colorPalette: AlbumPalette? = null
        private set

    private val albumArtTarget = object : ImageViewTarget<AlbumArt>(albumArt) {
        override fun setResource(resource: AlbumArt?) {
            if (resource != null) {
                super.view.setImageBitmap(resource.bitmap)
                applyPalette(resource.palette)
            }
        }
    }

    private fun applyPalette(palette: AlbumPalette) {
        colorPalette = palette
        card.setCardBackgroundColor(palette.primary)
        title.setTextColor(palette.bodyText)
    }

    override fun onAttachListeners(client: BaseAdapter.OnItemSelectedListener) {
        itemView.setOnClickListener { _ ->
            client.onItemSelected(adapterPosition, R.id.action_browse_item)
        }
    }

    override fun onBind(item: MediaBrowserCompat.MediaItem) {
        val description = item.description
        title.text = description.title
        applyPalette(defaultPalette)

        albumArt.transitionName = "art_" + item.mediaId

        glide.load(description.iconUri).into(albumArtTarget)
    }
}