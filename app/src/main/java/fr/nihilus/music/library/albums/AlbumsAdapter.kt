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

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ListAdapter
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.base.MediaItemDiffer
import fr.nihilus.music.core.ui.glide.GlideApp
import fr.nihilus.music.core.ui.glide.GlideRequest
import fr.nihilus.music.core.ui.glide.palette.AlbumArt
import fr.nihilus.music.extensions.resolveDefaultAlbumPalette

internal class AlbumsAdapter(
    fragment: Fragment,
    private val onAlbumSelected: (position: Int) -> Unit
) : ListAdapter<MediaItem, AlbumHolder>(MediaItemDiffer) {

    private val defaultPalette = fragment.requireContext().resolveDefaultAlbumPalette()
    private val glideRequest: GlideRequest<AlbumArt>

    init {
        val context = fragment.requireContext()
        val fallbackIcon = ContextCompat.getDrawable(context, R.drawable.ic_album_24dp)
        glideRequest = GlideApp.with(fragment).asAlbumArt()
            .disallowHardwareConfig()
            .fallbackColors(defaultPalette)
            .error(fallbackIcon)
            .dontTransform()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumHolder {
        return AlbumHolder(
            parent,
            glideRequest,
            defaultPalette,
            isArtistAlbum = false,
            onAlbumSelected
        )
    }

    override fun onBindViewHolder(holder: AlbumHolder, position: Int) {
        holder.bind(super.getItem(position))
    }

    public override fun getItem(position: Int): MediaItem = super.getItem(position)
}
