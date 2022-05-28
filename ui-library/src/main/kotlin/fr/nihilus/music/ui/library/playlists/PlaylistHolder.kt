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

package fr.nihilus.music.ui.library.playlists

import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat
import android.view.ViewGroup
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.ui.library.databinding.PlaylistItemBinding

/**
 * Display a playlist as a floating list item.
 */
internal class PlaylistHolder(
    parent: ViewGroup,
    private val glide: RequestBuilder<Bitmap>,
    onPlaylistSelected: (position: Int) -> Unit
) : BaseHolder<MediaBrowserCompat.MediaItem>(parent, R.layout.playlist_item) {

    private val binding = PlaylistItemBinding.bind(itemView)

    init {
        itemView.setOnClickListener {
            onPlaylistSelected(adapterPosition)
        }
    }

    override fun bind(data: MediaBrowserCompat.MediaItem) {
        val description = data.description
        itemView.transitionName = description.mediaId
        binding.playlistTitle.text = description.title
        binding.playlistDescription.text = description.subtitle
        glide.load(description.iconUri).into(binding.playlistIcon)
    }
}
