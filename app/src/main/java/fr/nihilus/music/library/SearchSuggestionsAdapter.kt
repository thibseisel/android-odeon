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

package fr.nihilus.music.library

import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.R
import fr.nihilus.music.common.media.MediaId
import fr.nihilus.music.core.ui.extensions.inflate

private const val TYPE_TRACK = 1 shl 0
private const val TYPE_ALBUM = 1 shl 1
private const val TYPE_ARTIST = 1 shl 2
private const val TYPE_PLAYLIST = 1 shl 3

internal class SearchSuggestionsAdapter(fragment: Fragment) : BaseAdapter() {
    private val items = mutableListOf<MediaBrowserCompat.MediaItem>()
    private val glide = Glide.with(fragment).asBitmap()

    override fun getItemViewType(position: Int): Int {
        val item = items[position]
        val (type, category, track) = MediaId.parse(item.mediaId)

        return when {
            track != null -> TYPE_TRACK
            category == null -> Adapter.IGNORE_ITEM_VIEW_TYPE
            type == MediaId.TYPE_ALBUMS -> TYPE_ALBUM
            type == MediaId.TYPE_ARTISTS -> TYPE_ARTIST
            type == MediaId.TYPE_PLAYLISTS -> TYPE_PLAYLIST
            else -> Adapter.IGNORE_ITEM_VIEW_TYPE
        }
    }

    override fun getViewTypeCount(): Int = 4

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder = if (convertView == null) {
            val viewType = getItemViewType(position)
            onCreateViewHolder(parent, viewType).also { it.itemView.tag = it }
        } else {
            convertView.tag as ViewHolder
        }

        onBindViewHolder(holder, position)
        return holder.itemView
    }

    private fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder = ViewHolder(parent, glide, viewType)

    private fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItem(position: Int): MediaBrowserCompat.MediaItem {
        require(position in items.indices) { "Invalid item position: $position" }
        return items[position]
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = false

    override fun getCount(): Int = items.size

    fun submitList(newItems: List<MediaBrowserCompat.MediaItem>) {
        items.clear()
        items += newItems
        notifyDataSetChanged()
    }

    class ViewHolder(parent: ViewGroup, glide: RequestBuilder<Bitmap>, val viewType: Int) {
        val itemView = parent.inflate(R.layout.item_search_suggestion)
        private val iconView = itemView.findViewById<ImageView>(R.id.icon_view)
        private val titleView = itemView.findViewById<TextView>(R.id.title_view)

        private val glide = when (viewType) {
            TYPE_TRACK -> glide.fallback(R.drawable.ic_audiotrack_24dp)
            TYPE_PLAYLIST -> glide.fallback(R.drawable.ic_playlist_24dp)
            else -> glide
        }

        fun bind(item: MediaBrowserCompat.MediaItem) {
            when (viewType) {
                TYPE_TRACK -> bindTrack(item)
                TYPE_ALBUM -> bindAlbum(item)
                TYPE_ARTIST -> bindArtist(item)
                TYPE_PLAYLIST -> bindPlaylist(item)
            }
        }

        private fun bindTrack(track: MediaBrowserCompat.MediaItem) = with(track.description) {
            glide.load(iconUri).into(iconView)
            titleView.text = title
        }

        private fun bindAlbum(album: MediaBrowserCompat.MediaItem) = with(album.description) {
            glide.load(R.drawable.ic_album_24dp).into(iconView)
            titleView.text = title
        }

        private fun bindArtist(artist: MediaBrowserCompat.MediaItem) = with(artist.description) {
            glide.load(R.drawable.ic_person_24dp).into(iconView)
            titleView.text = title
        }

        private fun bindPlaylist(playlist: MediaBrowserCompat.MediaItem) = with(playlist.description) {
            glide.load(iconUri).into(iconView)
            titleView.text = title
        }
    }
}