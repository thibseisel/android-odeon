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

package fr.nihilus.music.devmenu.features

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.inflate
import fr.nihilus.music.devmenu.R
import fr.nihilus.music.media.provider.Track
import kotlinx.android.synthetic.main.dev_fragment_unlinked_tracks.*

/**
 * A screen displaying tracks having no corresponding Spotify tracks in a list.
 * This may be because those have not been synced yet.
 *
 * If no tracks are out-of-sync, then a visual indicator is displayed instead of the list.
 */
internal class UnlinkedTrackFragment : BaseFragment(R.layout.dev_fragment_unlinked_tracks) {
    private val viewModel by viewModels<UnlinkedTrackViewModel> { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = TrackAdapter()
        adapter.setHasStableIds(true)

        val listDividers = DividerItemDecoration(requireContext(), DividerItemDecoration.HORIZONTAL)
        unlinked_track_list.addItemDecoration(listDividers)
        unlinked_track_list.adapter = adapter

        viewModel.unlinkedTracks.observe(viewLifecycleOwner) { request ->
            when (request) {
                is LoadRequest.Pending -> {
                    unlinked_track_list.isVisible = false
                    group_when_empty.isVisible = false
                }

                is LoadRequest.Success -> {
                    val unlinkedTracks = request.data
                    val hasUnlinkedTracks = unlinkedTracks.isNotEmpty()
                    unlinked_track_list.isVisible = hasUnlinkedTracks
                    group_when_empty.isVisible = !hasUnlinkedTracks

                    adapter.submitList(unlinkedTracks)
                }
            }
        }
    }

    /**
     * Map each track to its visual two-line list row.
     */
    private class TrackAdapter : ListAdapter<Track, TrackAdapter.Holder>(TrackDiffer()) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = Holder(parent)

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder.bind(currentList[position])
        }

        override fun getItemId(position: Int): Long = when (hasStableIds()) {
            true -> currentList[position].id
            else -> RecyclerView.NO_ID
        }

        private class Holder(parent: ViewGroup) :
            RecyclerView.ViewHolder(parent.inflate(R.layout.dev_unlinked_track_row)) {
            private val title: TextView = itemView.findViewById(R.id.track_title)
            private val artist: TextView = itemView.findViewById(R.id.track_artist)

            fun bind(track: Track) {
                title.text = track.title
                artist.text = track.artist
            }
        }
    }

    private class TrackDiffer : DiffUtil.ItemCallback<Track>() {

        override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean =
            oldItem.title == newItem.title && oldItem.artist == newItem.artist
    }
}