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

package fr.nihilus.music.library.search

import android.graphics.Bitmap
import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.R
import fr.nihilus.music.common.media.MediaId
import fr.nihilus.music.common.media.toMediaId
import fr.nihilus.music.core.ui.extensions.inflate
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.GlideRequest
import fr.nihilus.music.ui.MediaItemDiffer

private const val TYPE_TRACK = 0
private const val TYPE_ALBUM = 1
private const val TYPE_ARTIST = 2
private const val TYPE_PLAYLIST = 3

internal class SearchResultsAdapter(
    fragment: Fragment,
    private val listener: (item: MediaBrowserCompat.MediaItem) -> Unit
) : ListAdapter<MediaBrowserCompat.MediaItem, SearchResultsAdapter.ViewHolder>(MediaItemDiffer) {

    private val glide = GlideApp.with(fragment).asBitmap()

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        val (type, category, track) = item.mediaId.toMediaId()

        return when {
            track != null -> TYPE_TRACK
            category == null -> Adapter.IGNORE_ITEM_VIEW_TYPE
            type == MediaId.TYPE_ALBUMS -> TYPE_ALBUM
            type == MediaId.TYPE_ARTISTS -> TYPE_ARTIST
            type == MediaId.TYPE_PLAYLISTS -> TYPE_PLAYLIST
            else -> Adapter.IGNORE_ITEM_VIEW_TYPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent, glide)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(
        parent: ViewGroup,
        private val glide: GlideRequest<Bitmap>
    ) : RecyclerView.ViewHolder(parent.inflate(R.layout.item_search_suggestion)) {

        private val iconView: ImageView = itemView.findViewById(R.id.icon_view)
        private val titleView: TextView = itemView.findViewById(R.id.title_view)

        init {
            itemView.setOnClickListener {
                val selectedItem = getItem(adapterPosition)
                listener(selectedItem)
            }
        }

        fun bind(result: MediaBrowserCompat.MediaItem): Unit = with(result.description) {
            titleView.text = title

            when (itemViewType) {
                TYPE_TRACK -> glide.fallback(R.drawable.ic_audiotrack_24dp).load(iconUri)
                TYPE_ALBUM -> glide.fallback(R.drawable.ic_album_24dp).load(null as Uri?)
                TYPE_ARTIST -> glide.fallback(R.drawable.ic_person_24dp).load(null as Uri?)
                TYPE_PLAYLIST -> glide.fallback(R.drawable.ic_playlist_24dp).load(iconUri)
                else -> error("Unexpected view type: $itemViewType")
            }.into(iconView)
        }
    }
}