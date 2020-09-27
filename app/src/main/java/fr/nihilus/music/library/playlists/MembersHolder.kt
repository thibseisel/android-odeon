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
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.text.format.DateUtils
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.R
import fr.nihilus.music.core.media.MediaItems
import fr.nihilus.music.core.ui.base.BaseHolder

/**
 * Display a playlist's track.
 */
internal class MembersHolder(
    parent: ViewGroup,
    private val glide: RequestBuilder<Bitmap>,
    onTrackSelected: (position: Int) -> Unit
) : BaseHolder<MediaItem>(parent, R.layout.playlist_track_item) {

    private val albumArt: ImageView = itemView.findViewById(R.id.album_art_view)
    private val title: TextView = itemView.findViewById(R.id.title)
    private val subtitle: TextView = itemView.findViewById(R.id.subtitle_view)

    private val subtitleTemplate = itemView.context.getString(R.string.song_item_subtitle)
    private val durationBuilder = StringBuilder()

    init {
        itemView.setOnClickListener {
            onTrackSelected(adapterPosition)
        }
    }

    override fun bind(data: MediaItem) {
        val description = data.description
        glide.load(description.iconUri).into(albumArt)
        title.text = description.title

        val millis = description.extras?.getLong(MediaItems.EXTRA_DURATION)
                ?: error("Track should have extras")
        subtitle.text = String.format(
            subtitleTemplate, description.subtitle,
            DateUtils.formatElapsedTime(durationBuilder, millis / 1000L)
        )
    }
}