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

package fr.nihilus.music.ui.library.artists.detail

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.core.ui.R.drawable
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.core.ui.formatDuration
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.ui.library.databinding.ArtistTrackItemBinding

internal class ArtistTracksAdapter(
    fragment: Fragment,
    private val onTrackSelect: (ArtistTrackUiState) -> Unit
) : ListAdapter<ArtistTrackUiState, ArtistTracksAdapter.ViewHolder>(TrackDiffer()) {

    init {
        setHasStableIds(true)
    }

    private val trackIconLoader = Glide.with(fragment).asBitmap()
        .error(ContextCompat.getDrawable(fragment.requireContext(), drawable.ic_audiotrack_24dp))
        .centerCrop()
        .autoClone()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(parent, trackIconLoader).also { holder ->
            holder.itemView.setOnClickListener {
                onTrackSelect(getItem(holder.bindingAdapterPosition))
            }
        }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int = R.id.view_type_track

    override fun getItemId(position: Int): Long = getItem(position).id.track!!

    class ViewHolder(
        parent: ViewGroup,
        private val iconLoader: RequestBuilder<Bitmap>,
    ) : BaseHolder<ArtistTrackUiState>(parent, R.layout.artist_track_item) {
        private val binding = ArtistTrackItemBinding.bind(itemView)

        override fun bind(data: ArtistTrackUiState) {
            binding.trackTitle.text = data.title
            binding.trackDuration.text = formatDuration(data.duration.inWholeMilliseconds)
            iconLoader.load(data.iconUri).into(binding.albumArtwork)
        }
    }

    class TrackDiffer : DiffUtil.ItemCallback<ArtistTrackUiState>() {
        override fun areItemsTheSame(
            oldItem: ArtistTrackUiState,
            newItem: ArtistTrackUiState
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ArtistTrackUiState,
            newItem: ArtistTrackUiState
        ): Boolean = oldItem == newItem
    }
}
