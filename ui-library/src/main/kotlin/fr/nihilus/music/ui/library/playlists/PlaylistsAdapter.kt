/*
 * Copyright 2021 Thibault Seisel
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

package fr.nihilus.music.ui.library.playlists

import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.ui.library.databinding.PlaylistItemBinding

/**
 * Display playlists in a list of floating cards.
 */
internal class PlaylistsAdapter(
    fragment: Fragment,
    private val selectPlaylist: (PlaylistUiState, ViewHolder) -> Unit
) : ListAdapter<PlaylistUiState, PlaylistsAdapter.ViewHolder>(PlaylistDiffer()) {

    init {
        setHasStableIds(true)
    }

    private val glideRequest = Glide.with(fragment).asBitmap()
        .fallback(R.drawable.ic_playlist_24dp)
        .autoClone()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(parent, glideRequest) { holder ->
            selectPlaylist(
                getItem(holder.adapterPosition),
                holder
            )
        }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long = getItem(position).id.hashCode().toLong()

    class ViewHolder(
        parent: ViewGroup,
        private val glide: RequestBuilder<Bitmap>,
        onClick: (ViewHolder) -> Unit,
    ) : BaseHolder<PlaylistUiState>(parent, R.layout.playlist_item) {
        private val binding = PlaylistItemBinding.bind(itemView)

        init {
            itemView.setOnClickListener {
                onClick(this)
            }
        }

        override fun bind(data: PlaylistUiState) {
            itemView.transitionName = data.id.encoded
            glide.load(data.iconUri).into(binding.playlistIcon)
            binding.playlistTitle.text = data.title
            binding.playlistDescription.text = data.subtitle
        }
    }

    private class PlaylistDiffer : DiffUtil.ItemCallback<PlaylistUiState>() {
        override fun areItemsTheSame(
            oldItem: PlaylistUiState,
            newItem: PlaylistUiState
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: PlaylistUiState,
            newItem: PlaylistUiState
        ): Boolean = oldItem == newItem
    }
}
