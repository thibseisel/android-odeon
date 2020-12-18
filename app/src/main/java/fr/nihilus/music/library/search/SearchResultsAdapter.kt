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

package fr.nihilus.music.library.search

import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.R
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.parse
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.core.ui.glide.GlideApp
import fr.nihilus.music.databinding.ItemSearchSuggestionBinding
import fr.nihilus.music.databinding.SectionHeaderItemBinding
import fr.nihilus.music.extensions.resolveDefaultAlbumPalette
import fr.nihilus.music.library.albums.AlbumHolder
import fr.nihilus.music.library.artists.ArtistHolder
import fr.nihilus.music.library.playlists.PlaylistHolder

internal class SearchResultsAdapter(
    fragment: Fragment,
    private val listener: (item: MediaItem, adapterPosition: Int, action: ItemAction) -> Unit
) : ListAdapter<SearchResult, BaseHolder<*>>(SearchResultDiffer()) {

    private val glide = Glide.with(fragment).asBitmap()
    private val albumLoader = GlideApp.with(fragment).asAlbumArt()
    private val defaultPalette = fragment.requireContext().resolveDefaultAlbumPalette()

    override fun getItemViewType(position: Int): Int = when (val result = getItem(position)) {
        is SearchResult.SectionHeader -> R.id.view_type_header
        is SearchResult.Media -> getMediaViewType(result.item)
    }

    private fun getMediaViewType(item: MediaItem): Int {
        val (type, category, track) = item.mediaId.parse()
        return when {
            track != null -> R.id.view_type_track
            category == null -> error("Expected search result to have a media category")
            type == MediaId.TYPE_ALBUMS -> R.id.view_type_album
            type == MediaId.TYPE_ARTISTS -> R.id.view_type_artist
            type == MediaId.TYPE_PLAYLISTS -> R.id.view_type_playlist
            else -> error("Unexpected media type in search results: $type")
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseHolder<*> {
        val browsableSelectedListener = { adapterPosition: Int ->
            val result = getItem(adapterPosition) as SearchResult.Media
            listener(result.item, adapterPosition, ItemAction.PRIMARY)
        }

        return when (viewType) {
            R.id.view_type_header -> SectionHolder(parent)
            R.id.view_type_track -> TrackHolder(parent, glide) { position, action ->
                val result = getItem(position) as SearchResult.Media
                listener(result.item, position, action)
            }
            R.id.view_type_album -> AlbumHolder(
                parent,
                albumLoader,
                defaultPalette,
                isArtistAlbum = false,
                browsableSelectedListener
            )
            R.id.view_type_artist -> ArtistHolder(parent, glide, browsableSelectedListener)
            R.id.view_type_playlist -> PlaylistHolder(parent, glide, browsableSelectedListener)
            else -> error("Unexpected viewType: $viewType")
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: BaseHolder<*>, position: Int) {
        when (val result = getItem(position)) {
            is SearchResult.SectionHeader -> (holder as SectionHolder).bind(result)
            is SearchResult.Media -> (holder as BaseHolder<MediaItem>).bind(result.item)
        }
    }

    /**
     * Set of actions that could be performed on a search result.
     */
    enum class ItemAction {

        /**
         * Given the nature of the selected media, either play it (if it is playable)
         * or browse its content (if it is browsable).
         */
        PRIMARY,

        /**
         * Append the selected media to a playlist.
         * This is only applicable to tracks.
         */
        ADD_TO_PLAYLIST,

        /**
         * Delete the selected media.
         * This is only applicable to tracks.
         */
        DELETE
    }

    /**
     * Displays a section title that separates groups of media of the same type.
     */
    private class SectionHolder(
        parent: ViewGroup
    ) : BaseHolder<SearchResult.SectionHeader>(parent, R.layout.section_header_item) {

        private val binding = SectionHeaderItemBinding.bind(itemView)

        override fun bind(data: SearchResult.SectionHeader) {
            binding.sectionTitle.setText(data.titleResId)
        }
    }

    /**
     * Displays a track search result.
     */
    private class TrackHolder(
        parent: ViewGroup,
        glide: RequestBuilder<Bitmap>,
        private val onItemAction: (position: Int, action: ItemAction) -> Unit
    ) : BaseHolder<MediaItem>(parent, R.layout.item_search_suggestion) {

        private val binding = ItemSearchSuggestionBinding.bind(itemView)
        private val imageLoader = glide.fallback(R.drawable.placeholder_track_icon)

        init {
            setupTrackActionMenu()

            itemView.setOnClickListener {
                onItemAction(adapterPosition, ItemAction.PRIMARY)
            }
        }

        override fun bind(data: MediaItem) {
            with(data.description) {
                binding.trackTitle.text = title
                imageLoader.load(iconUri).into(binding.albumArtwork)
            }
        }

        private fun setupTrackActionMenu() {
            val popup = PopupMenu(
                itemView.context,
                binding.overflowIcon,
                Gravity.END or Gravity.BOTTOM,
                0,
                R.style.Widget_Odeon_PopupMenu_Overflow
            )

            popup.inflate(R.menu.track_popup_menu)
            popup.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_playlist -> {
                        onItemAction(adapterPosition, ItemAction.ADD_TO_PLAYLIST)
                        true
                    }

                    R.id.action_delete -> {
                        onItemAction(adapterPosition, ItemAction.DELETE)
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

    private class SearchResultDiffer : DiffUtil.ItemCallback<SearchResult>() {

        override fun areItemsTheSame(
            oldItem: SearchResult,
            newItem: SearchResult
        ): Boolean = oldItem.hasSameId(newItem)

        override fun areContentsTheSame(
            oldItem: SearchResult,
            newItem: SearchResult
        ): Boolean = newItem.hasSameContent(newItem)
    }
}