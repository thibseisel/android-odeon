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

package fr.nihilus.music.ui.library.playlists.details

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.core.ui.formatDuration
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.ui.library.databinding.PlaylistTrackItemBinding
import fr.nihilus.music.core.ui.R as CoreUiR

internal class PlaylistTracksAdapter(
    fragment: Fragment,
    private val onSelectTrack: (PlaylistTrackUiState) -> Unit
) : ListAdapter<PlaylistTrackUiState, PlaylistTracksAdapter.TrackHolder>(PlaylistTrackDiffer()) {

    // Note: Glide is attached to the context of the activity to workaround a bug in
    // MaterialContainerTransform not capturing images in return transition.
    private val glideRequest = Glide.with(fragment.requireActivity()).asBitmap()
        .error(CoreUiR.drawable.ic_audiotrack_24dp)
        .autoClone()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): TrackHolder = TrackHolder(parent, glideRequest).also { holder ->
        holder.itemView.setOnClickListener {
            onSelectTrack(getItem(holder.bindingAdapterPosition))
        }
    }

    override fun onBindViewHolder(holder: TrackHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long = getItem(position).id.track!!

    class TrackHolder(
        parent: ViewGroup,
        private val artworkLoader: RequestBuilder<Bitmap>,
    ) : BaseHolder<PlaylistTrackUiState>(parent, R.layout.playlist_track_item) {
        private val binding = PlaylistTrackItemBinding.bind(itemView)

        override fun bind(data: PlaylistTrackUiState) {
            artworkLoader.load(data.artworkUri).into(binding.albumArtwork)
            binding.trackTitle.text = data.title
            binding.trackMetadata.text = itemView.resources.getString(
                R.string.song_item_subtitle,
                data.artistName,
                formatDuration(data.duration.inWholeMilliseconds)
            )
        }
    }

    class PlaylistTrackDiffer : DiffUtil.ItemCallback<PlaylistTrackUiState>() {
        override fun areItemsTheSame(
            oldItem: PlaylistTrackUiState,
            newItem: PlaylistTrackUiState
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: PlaylistTrackUiState,
            newItem: PlaylistTrackUiState
        ): Boolean = oldItem == newItem
    }
}
