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
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.R
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.ui.BaseAdapter

/**
 * Display a playlist as a floating grid item.
 * Playlists that are marked as playable could be played by taping the play action icon.
 */
internal class PlaylistHolder(
    parent: ViewGroup,
    private val glide: RequestBuilder<Bitmap>
) : BaseAdapter.ViewHolder(parent, R.layout.playlist_item) {

    private val title: TextView = itemView.findViewById(R.id.title)
    private val image: ImageView = itemView.findViewById(R.id.album_art)
    private val actionPlay: View = itemView.findViewById(R.id.play_fab)

    override fun onAttachListeners(client: BaseAdapter.OnItemSelectedListener) {

        itemView.setOnClickListener { _ ->
            client.onItemSelected(adapterPosition, R.id.action_browse_item)
        }

        actionPlay.setOnClickListener { _ ->
            client.onItemSelected(adapterPosition, R.id.action_play_item)
        }
    }

    override fun onBind(item: MediaBrowserCompat.MediaItem) {
        val description = item.description
        val iconResId = description.extras?.getInt(MediaItems.EXTRA_ICON_ID, 0) ?: 0
        title.text = description.title

        if (iconResId != 0) {
            image.setImageResource(iconResId)
        } else {
            glide.load(description.iconUri).into(image)
        }

        // The play button is only shown if the item is playable
        actionPlay.visibility = if (item.isPlayable) View.VISIBLE else View.GONE
    }
}