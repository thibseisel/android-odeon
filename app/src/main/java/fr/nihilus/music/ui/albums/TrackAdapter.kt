/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.ui.albums

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaMetadataCompat
import android.support.v7.widget.RecyclerView
import android.text.format.DateUtils
import android.view.ViewGroup
import android.widget.TextView
import fr.nihilus.music.Constants
import fr.nihilus.music.R
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.utils.MediaID

class TrackAdapter(
        private val listener: BaseAdapter.OnItemSelectedListener
) : BaseAdapter<TrackAdapter.TrackHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder {
        return TrackHolder(parent).also { holder ->
            holder.onAttachListeners(listener)
        }
    }

    override fun getItemId(position: Int): Long {
        if (hasStableIds()) {
            val mediaId = items[position].mediaId
            return MediaID.extractMusicID(mediaId)?.toLong() ?: RecyclerView.NO_ID
        }

        return RecyclerView.NO_ID
    }

    /**
     * Retrieve the position of a given metadata in the adapter.
     *
     * @param metadata Metadata whose music id matches the position of the searched track.
     * @return Position in the adapter, on `-1` if not found.
     */
    fun indexOf(metadata: MediaMetadataCompat): Int {
        // Assume the passed musicId is from ALBUMS category
        val musicId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
        val searchedMediaId = MediaID.createMediaID(musicId, MediaID.ID_ALBUMS)

        return items.indexOfFirst { searchedMediaId == it.mediaId }
    }

    class TrackHolder(parent: ViewGroup) : BaseAdapter.ViewHolder(parent, R.layout.album_track_item) {
        private val trackNo: TextView = itemView.findViewById(R.id.trackNo)
        private val title: TextView = itemView.findViewById(R.id.title)
        private val duration: TextView = itemView.findViewById(R.id.duration)

        private val timeBuilder = StringBuilder()

        override fun onAttachListeners(client: OnItemSelectedListener) {
            itemView.setOnClickListener { _ ->
                client.onItemSelected(adapterPosition, Constants.ACTION_PLAY)
            }
        }

        override fun onBind(item: MediaItem) {
            val description = item.description
            title.text = description.title

            description.extras?.let {
                trackNo.text = it.getLong(MediaItems.EXTRA_TRACK_NUMBER).toString()
                val durationMillis = it.getLong(MediaItems.EXTRA_DURATION)
                duration.text = DateUtils.formatElapsedTime(timeBuilder, durationMillis / 1000L)
            }
        }
    }
}
