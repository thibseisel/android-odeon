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

package fr.nihilus.music.ui.library.search

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.core.ui.glide.GlideApp
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.ui.library.databinding.SectionHeaderItemBinding
import fr.nihilus.music.ui.library.extensions.resolveDefaultAlbumPalette

internal class SearchResultsAdapter(
    fragment: Fragment,
    private val onAddToPlaylist: (SearchResult.Track) -> Unit,
    private val onExclude: (SearchResult.Track) -> Unit,
    private val onDelete: (SearchResult.Track) -> Unit,
    private val onPlay: (SearchResult.Track) -> Unit,
    private val onBrowse: (SearchResult.Browsable, position: Int) -> Unit,
) : ListAdapter<SearchResult, BaseHolder<*>>(SearchResultDiffer()) {

    private val glide = Glide.with(fragment).asBitmap().autoClone()
    private val albumLoader = GlideApp.with(fragment).asAlbumArt().autoClone()
    private val defaultPalette = fragment.requireContext().resolveDefaultAlbumPalette()

    override fun getItemViewType(position: Int): Int = when (val result = getItem(position)) {
        is SearchResult.SectionHeader -> R.id.view_type_header
        is SearchResult.Track -> R.id.view_type_track
        is SearchResult.Browsable -> getMediaViewType(result)
    }

    private fun getMediaViewType(media: SearchResult.Browsable): Int {
        val (type, category, track) = media.id
        check(track == null && category != null) {
            "Invalid browsable media ${media.id}"
        }
        return when (type) {
            MediaId.TYPE_ALBUMS -> R.id.view_type_album
            MediaId.TYPE_ARTISTS -> R.id.view_type_artist
            MediaId.TYPE_PLAYLISTS -> R.id.view_type_playlist
            else -> error("Unexpected media type in search results: $type")
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseHolder<*> {
        val onSelectBrowsable = fun(adapterPosition: Int) {
            val media = getItem(adapterPosition) as SearchResult.Browsable
            onBrowse(media, adapterPosition)
        }

        return when (viewType) {
            R.id.view_type_header -> SectionHolder(parent)
            R.id.view_type_track -> TrackHolder(
                parent,
                glide,
                onPlaylist = {
                    val track = getItem(it) as SearchResult.Track
                    onAddToPlaylist(track)
                },
                onExclude = {
                    val track = getItem(it) as SearchResult.Track
                    onExclude(track)
                },
                onDelete = {
                    val track = getItem(it) as SearchResult.Track
                    onDelete(track)
                },
                onSelect = { position ->
                    val track = getItem(position) as SearchResult.Track
                    onPlay(track)
                }
            )

            R.id.view_type_album -> AlbumHolder(
                parent,
                albumLoader,
                defaultPalette,
                onSelect = onSelectBrowsable,
            )

            R.id.view_type_artist -> ArtistHolder(parent, glide, onSelectBrowsable)
            R.id.view_type_playlist -> PlaylistHolder(parent, glide, onSelectBrowsable)
            else -> error("Unexpected viewType: $viewType")
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun onBindViewHolder(holder: BaseHolder<*>, position: Int) {
        when (val result = getItem(position)) {
            is SearchResult.SectionHeader -> (holder as SectionHolder).bind(result)
            is SearchResult.Track -> (holder as BaseHolder<SearchResult.Track>).bind(result)
            is SearchResult.Browsable -> (holder as BaseHolder<SearchResult.Browsable>).bind(result)
        }
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
