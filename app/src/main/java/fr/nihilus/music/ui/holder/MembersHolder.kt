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

import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat
import android.text.format.DateUtils
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideRequest
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.ui.BaseAdapter

/**
 * Display a playlist's track.
 */
internal class MembersHolder(
    parent: ViewGroup,
    private val glide: GlideRequest<Bitmap>
) : BaseAdapter.ViewHolder(parent, R.layout.song_list_item) {

    private val albumArt: ImageView = itemView.findViewById(R.id.album_art_view)
    private val title: TextView = itemView.findViewById(R.id.title)
    private val subtitle: TextView = itemView.findViewById(R.id.subtitle_view)

    private val subtitleTemplate = itemView.context.getString(R.string.song_item_subtitle)
    private val durationBuilder = StringBuilder()

    override fun onAttachListeners(client: BaseAdapter.OnItemSelectedListener) {
        itemView.setOnClickListener {
            client.onItemSelected(adapterPosition, R.id.action_play_item)
        }
    }

    override fun onBind(item: MediaBrowserCompat.MediaItem) {
        val description = item.description
        glide.load(description.iconUri).into(albumArt)
        title.text = description.title

        val millis = description.extras?.getLong(MediaItems.EXTRA_DURATION)
                ?: throw IllegalStateException("Track should have extras")
        subtitle.text = String.format(
            subtitleTemplate, description.subtitle,
            DateUtils.formatElapsedTime(durationBuilder, millis / 1000L)
        )
    }
}