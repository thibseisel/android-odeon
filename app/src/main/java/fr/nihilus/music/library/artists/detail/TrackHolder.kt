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

package fr.nihilus.music.library.artists.detail

import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat
import android.text.format.DateUtils
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.R
import fr.nihilus.music.common.media.MediaItems
import fr.nihilus.music.ui.BaseAdapter

internal class TrackHolder(
    parent: ViewGroup,
    private val glide: RequestBuilder<Bitmap>
) : BaseAdapter.ViewHolder(parent, R.layout.artist_track_item) {

    val albumArt: ImageView = itemView.findViewById(R.id.album_art)
    val title: TextView = itemView.findViewById(R.id.title)
    val duration: TextView = itemView.findViewById(R.id.duration)

    override fun onAttachListeners(client: BaseAdapter.OnItemSelectedListener) {
        itemView.setOnClickListener {
            client.onItemSelected(adapterPosition, R.id.action_play_item)
        }
    }

    override fun onBind(item: MediaBrowserCompat.MediaItem) {
        val description = item.description
        title.text = description.title
        glide.load(description.iconUri).into(albumArt)

        description.extras?.let {
            val millis = it.getLong(MediaItems.EXTRA_DURATION)
            duration.text = DateUtils.formatElapsedTime(millis / 1000L)
        }

    }
}