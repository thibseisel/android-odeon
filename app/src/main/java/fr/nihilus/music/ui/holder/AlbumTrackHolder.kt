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
import android.text.format.DateUtils
import android.view.ViewGroup
import android.widget.TextView
import fr.nihilus.music.R
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.ui.BaseAdapter

internal class AlbumTrackHolder(parent: ViewGroup) :
    BaseAdapter.ViewHolder(parent, R.layout.album_track_item) {
    private val trackNo: TextView = itemView.findViewById(R.id.trackNo)
    private val title: TextView = itemView.findViewById(R.id.title)
    private val duration: TextView = itemView.findViewById(R.id.duration)

    private val timeBuilder = StringBuilder()

    override fun onAttachListeners(client: BaseAdapter.OnItemSelectedListener) {
        itemView.setOnClickListener { _ ->
            client.onItemSelected(adapterPosition, R.id.action_play_item)
        }
    }

    override fun onBind(item: MediaBrowserCompat.MediaItem) {
        val description = item.description
        title.text = description.title

        description.extras?.let {
            trackNo.text = it.getLong(MediaItems.EXTRA_TRACK_NUMBER).toString()
            val durationMillis = it.getLong(MediaItems.EXTRA_DURATION)
            duration.text = DateUtils.formatElapsedTime(timeBuilder, durationMillis / 1000L)
        }
    }
}