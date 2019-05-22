/*
 * Copyright 2019 Thibault Seisel
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

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.GlideRequest
import fr.nihilus.music.glide.palette.AlbumArt
import fr.nihilus.music.ui.BaseAdapter

internal class AlbumsAdapter(
    fragment: Fragment,
    private val defaultPalette: AlbumPalette,
    private val listener: OnItemSelectedListener
) : BaseAdapter<AlbumHolder>() {

    private val glideRequest: GlideRequest<AlbumArt>

    init {
        val context = fragment.requireContext()
        val fallbackIcon = context.getDrawable(R.drawable.ic_album_24dp)
        glideRequest = GlideApp.with(fragment).asAlbumArt()
            .fallbackColors(defaultPalette)
            .fallback(fallbackIcon)
            .centerCrop()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumHolder {
        return AlbumHolder(
            parent,
            glideRequest,
            defaultPalette,
            false
        ).also {
            it.onAttachListeners(listener)
        }
    }
}
