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

package fr.nihilus.music.ui.library.search

import android.graphics.Bitmap
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.ui.library.databinding.ItemSearchSuggestionBinding

/**
 * Displays a track search result.
 */
internal class TrackHolder(
    parent: ViewGroup,
    glide: RequestBuilder<Bitmap>,
    private val onPlaylist: (Int) -> Unit,
    private val onExclude: (Int) -> Unit,
    private val onDelete: (Int) -> Unit,
    private val onSelect: (Int) -> Unit,
) : BaseHolder<SearchResult.Track>(parent, R.layout.item_search_suggestion) {

    private val binding = ItemSearchSuggestionBinding.bind(itemView)
    private val imageLoader = glide.fallback(fr.nihilus.music.core.ui.R.drawable.ic_audiotrack_24dp)

    init {
        setupTrackActionMenu()

        itemView.setOnClickListener {
            onSelect(bindingAdapterPosition)
        }
    }

    override fun bind(data: SearchResult.Track) {
        binding.trackTitle.text = data.title
        imageLoader.load(data.iconUri).into(binding.albumArtwork)
    }

    private fun setupTrackActionMenu() {
        val popup = PopupMenu(
            itemView.context,
            binding.overflowIcon,
            Gravity.END or Gravity.BOTTOM,
            0,
            fr.nihilus.music.core.ui.R.style.Widget_Odeon_PopupMenu_Overflow
        )

        popup.inflate(R.menu.track_popup_menu)
        popup.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_playlist -> {
                    onPlaylist(bindingAdapterPosition)
                    true
                }

                R.id.action_exclude -> {
                    onExclude(bindingAdapterPosition)
                    true
                }

                R.id.action_delete -> {
                    onDelete(bindingAdapterPosition)
                    true
                }

                else -> false
            }
        }

        binding.overflowIcon.setOnClickListener {
            popup.show()
        }
    }
}
