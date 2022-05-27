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

package fr.nihilus.music.library.songs

import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.base.ListAdapter
import fr.nihilus.music.databinding.SongListItemBinding
import fr.nihilus.music.ui.formatDuration
import fr.nihilus.music.core.ui.R as CoreUiR

/**
 * Prepare a list of tracks to display them as rows in a RecyclerView.
 */
internal class TrackListAdapter(
    fragment: Fragment,
    private val addToPlaylist: (TrackUiState) -> Unit,
    private val delete: (TrackUiState) -> Unit,
    private val exclude: (TrackUiState) -> Unit,
    private val play: (TrackUiState) -> Unit,
) : ListAdapter<TrackUiState, TrackListAdapter.ViewHolder>() {

    private val artworkLoader = Glide.with(fragment).asBitmap()
        .error(R.drawable.ic_audiotrack_24dp)
        .autoClone()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemId(position: Int): Long = getItem(position).id.hashCode().toLong()

    inner class ViewHolder(
        parent: ViewGroup,
    ) : ListAdapter.ViewHolder(parent, R.layout.song_list_item) {
        private val binding = SongListItemBinding.bind(itemView)

        init {
            // Open the popup menu when the overflow icon is clicked.
            val popup = PopupMenu(
                itemView.context,
                binding.overflowIcon,
                Gravity.BOTTOM or Gravity.END,
                0,
                CoreUiR.style.Widget_Odeon_PopupMenu_Overflow
            ).apply {
                inflate(R.menu.track_popup_menu)
                setOnMenuItemClickListener { item ->
                    val track = items[position]

                    when (item.itemId) {
                        R.id.action_playlist -> {
                            addToPlaylist(track)
                            true
                        }
                        R.id.action_exclude -> {
                            exclude(track)
                            true
                        }
                        R.id.action_delete -> {
                            delete(track)
                            true
                        }
                        else -> false
                    }
                }
            }

            binding.overflowIcon.setOnClickListener {
                popup.show()
            }

            itemView.setOnClickListener {
                play(items[position])
            }
        }

        fun bind(track: TrackUiState) {
            artworkLoader.load(track.artworkUri).into(binding.albumArtwork)
            binding.trackTitle.text = track.title
            binding.trackMetadata.text = itemView.context.getString(
                R.string.song_item_subtitle,
                track.artist,
                formatDuration(track.duration.inWholeMilliseconds)
            )
        }
    }
}
