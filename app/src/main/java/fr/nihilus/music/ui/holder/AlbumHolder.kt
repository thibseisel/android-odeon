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

import android.support.annotation.ColorInt
import android.support.v4.media.MediaBrowserCompat
import android.support.v7.graphics.Palette
import android.support.v7.widget.CardView
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.request.target.ImageViewTarget
import fr.nihilus.music.R
import fr.nihilus.music.glide.palette.PaletteBitmap
import fr.nihilus.music.ui.BaseAdapter

/**
 *
 */
internal class AlbumHolder(
    parent: ViewGroup,
    private val glide: RequestBuilder<PaletteBitmap>,
    private val defaultColors: DefaultColors
) : BaseAdapter.ViewHolder(parent, R.layout.album_grid_item) {

    private val card: CardView = itemView.findViewById(R.id.card)
    private val albumArt: ImageView = itemView.findViewById(R.id.albumArtView)
    private val title: TextView = itemView.findViewById(R.id.title)
    private val artist: TextView = itemView.findViewById(R.id.artist)

    private val albumViewTarget = object : ImageViewTarget<PaletteBitmap>(albumArt) {

        override fun setResource(resource: PaletteBitmap?) {
            if (resource != null) {
                applyPalette(resource.palette)
                super.view.setImageBitmap(resource.bitmap)
            }
        }
    }

    inline val transitionView get() = albumArt

    var palette: Palette? = null

    private fun applyPalette(palette: Palette?) {
        this.palette = palette

        palette?.dominantSwatch?.let {
            val primaryColor = it.rgb
            val bodyColor = it.bodyTextColor
            setColors(primaryColor, bodyColor)

        } ?: applyDefaultColors()
    }

    private fun applyDefaultColors() {
        val (primaryColor, _, _, bodyColor) = defaultColors
        setColors(primaryColor, bodyColor)
    }

    private fun setColors(
        @ColorInt primary: Int,
        @ColorInt body: Int
    ) {
        card.setCardBackgroundColor(primary)
        title.setTextColor(body)
        artist.setTextColor(body)
    }

    override fun onAttachListeners(client: BaseAdapter.OnItemSelectedListener) {
        itemView.setOnClickListener { _ ->
            client.onItemSelected(adapterPosition, R.id.action_browse_item)
        }
    }

    override fun onBind(item: MediaBrowserCompat.MediaItem) {
        val description = item.description
        title.text = description.title
        artist.text = description.subtitle

        glide.load(description.iconUri).into(albumViewTarget)
        albumArt.transitionName = "image_" + description.mediaId
    }

    /**
     * A set of color to be used as fallbacks when no such colors can be extracted
     * from the image displayed by this album holder.
     */
    data class DefaultColors(
        @ColorInt val primary: Int,
        @ColorInt val accent: Int,
        @ColorInt val title: Int,
        @ColorInt val body: Int
    )
}