/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.ui.library.albums

import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.core.ui.formatDuration
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.ui.library.databinding.AlbumTrackItemBinding

/**
 * Manage the presentation of tracks that are part of an album in a list [RecyclerView].
 *
 * @constructor
 * @param trackSelectedListener Sets a function to be called when a track is selected from the list.
 */
internal class TrackAdapter(
    private val trackSelectedListener: (AlbumDetailUiState.Track) -> Unit
) : ListAdapter<AlbumDetailUiState.Track, TrackAdapter.ViewHolder>(Differ()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(parent).also { holder ->
            holder.itemView.setOnClickListener {
                val position = holder.bindingAdapterPosition
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

    class ViewHolder(
        parent: ViewGroup
    ) : BaseHolder<AlbumDetailUiState.Track>(parent, R.layout.album_track_item) {

        private val binding = AlbumTrackItemBinding.bind(itemView)

        /**
         * Updates this holder's view to indicate if this track is currently playing.
         * Prefer this function over [bind] when only the playback state of the track has changed
         * to avoid rebinding the full item when not necessary.
         *
         * @param isPlaying Whether the track is the one currently playing.
         */
        fun setPlaybackState(isPlaying: Boolean) {
            binding.playIndicator.isVisible = isPlaying
            binding.trackNo.isVisible = !isPlaying
        }

        /**
         * Updates this holder's view to reflect the current track.
         *
         * @param data The track that should be displayed by this holder.
         */
        override fun bind(data: AlbumDetailUiState.Track) {
            binding.trackNo.text = data.number.toString()
            binding.trackTitle.text = data.title
            binding.trackDuration.text = formatDuration(data.duration.inWholeMilliseconds)
            setPlaybackState(data.isPlaying)
        }
    }

    /**
     * Calculate the display difference between instances of [AlbumDetailUiState.Track].
     * A partial item bind will be requested if only its [playback state][AlbumDetailUiState.Track.isPlaying]
     * has changed.
     */
    private class Differ : DiffUtil.ItemCallback<AlbumDetailUiState.Track>() {

        override fun areItemsTheSame(
            oldItem: AlbumDetailUiState.Track, newItem: AlbumDetailUiState.Track
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: AlbumDetailUiState.Track, newItem: AlbumDetailUiState.Track
        ): Boolean = oldItem.isPlaying == newItem.isPlaying
                && oldItem.title == newItem.title
                && oldItem.number == newItem.number
                && oldItem.duration == newItem.duration

        override fun getChangePayload(
            oldItem: AlbumDetailUiState.Track, newItem: AlbumDetailUiState.Track
        ): Any? = when {
            oldItem.isPlaying != newItem.isPlaying -> newItem.isPlaying
            else -> null
        }
    }
}
