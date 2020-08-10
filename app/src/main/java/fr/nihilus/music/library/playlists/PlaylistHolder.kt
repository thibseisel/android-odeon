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

package fr.nihilus.music.library.playlists

import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.base.BaseAdapter

/**
 * Display a playlist as a floating list item.
 */
internal class PlaylistHolder(
    parent: ViewGroup,
    private val glide: RequestBuilder<Bitmap>
) : BaseAdapter.ViewHolder(parent, R.layout.playlist_item) {

    private val image: ImageView = itemView.findViewById(R.id.album_art)
    private val title: TextView = itemView.findViewById(R.id.title)
    private val subtitle: TextView = itemView.findViewById(R.id.subtitle)

    override fun onAttachListeners(client: BaseAdapter.OnItemSelectedListener) {

        itemView.setOnClickListener {
            client.onItemSelected(adapterPosition)
        }
    }

    override fun onBind(item: MediaBrowserCompat.MediaItem) {
        val description = item.description
        title.text = description.title
        subtitle.text = description.subtitle
        glide.load(description.iconUri).into(image)
    }
}