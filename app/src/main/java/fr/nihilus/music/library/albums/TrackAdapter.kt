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

import android.text.format.DateUtils
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.extensions.inflate

/**
 * Manage the presentation of tracks that are part of an album in a list [RecyclerView].
 *
 * @constructor
 * @param trackSelectedListener Sets a function to be called when a track is selected from the list.
 */
internal class TrackAdapter(
    private val trackSelectedListener: (AlbumDetailState.Track) -> Unit
) : ListAdapter<AlbumDetailState.Track, TrackAdapter.ViewHolder>(Differ()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(parent).also { holder ->
            holder.itemView.setOnClickListener {
                val position = holder.adapterPosition
                trackSelectedListener(getItem(position))
            }
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        val track = getItem(position)

        if (payloads.isEmpty()) {
            holder.bind(track)
        } else {
            holder.setPlaybackState(track.isPlaying)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        onBindViewHolder(holder, position, emptyList())
    }

    class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.album_track_item)) {
        private val isPlayingIndicator: ImageView = itemView.findViewById(R.id.play_indicator)
        private val trackNo: TextView = itemView.findViewById(R.id.track_no)
        private val title: TextView = itemView.findViewById(R.id.title)
        private val duration: TextView = itemView.findViewById(R.id.duration)

        private val timeBuilder = StringBuilder()

        /**
         * Updates this holder's view to indicate if this track is currently playing.
         * Prefer this function over [bind] when only the playback state of the track has changed
         * to avoid rebinding the full item when not necessary.
         *
         * @param isPlaying Whether the track is the one currently playing.
         */
        fun setPlaybackState(isPlaying: Boolean) {
            isPlayingIndicator.isVisible = isPlaying
            trackNo.isVisible = !isPlaying
        }

        /**
         * Updates this holder's view to reflect the current track.
         *
         * @param track The track that should be displayed by this holder.
         */
        fun bind(track: AlbumDetailState.Track) {
            trackNo.text = track.number.toString()
            title.text = track.title
            duration.text = DateUtils.formatElapsedTime(timeBuilder, track.duration / 1000L)
            setPlaybackState(track.isPlaying)
        }
    }

    /**
     * Calculate the display difference between instances of [AlbumDetailState.Track].
     * A partial item bind will be requested if only its [playback state][AlbumDetailState.Track.isPlaying]
     * has changed.
     */
    private class Differ : DiffUtil.ItemCallback<AlbumDetailState.Track>() {

        override fun areItemsTheSame(
            oldItem: AlbumDetailState.Track,
            newItem: AlbumDetailState.Track
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: AlbumDetailState.Track,
            newItem: AlbumDetailState.Track
        ): Boolean = oldItem.isPlaying == newItem.isPlaying
                && oldItem.title == newItem.title
                && oldItem.number == newItem.number
                && oldItem.duration == newItem.duration

        override fun getChangePayload(
            oldItem: AlbumDetailState.Track,
            newItem: AlbumDetailState.Track
        ): Any? = when {
            oldItem.isPlaying != newItem.isPlaying -> newItem.isPlaying
            else -> null
        }
    }
}