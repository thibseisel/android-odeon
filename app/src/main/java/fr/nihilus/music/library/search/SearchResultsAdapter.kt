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

import android.net.Uri
import android.support.v4.media.MediaBrowserCompat
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.R
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.toMediaId
import fr.nihilus.music.core.ui.extensions.inflate
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.ui.MediaItemDiffer

internal class SearchResultsAdapter(
    fragment: Fragment,
    private val listener: (item: MediaBrowserCompat.MediaItem, action: ItemAction) -> Unit
) : ListAdapter<MediaBrowserCompat.MediaItem, SearchResultsAdapter.ViewHolder>(MediaItemDiffer) {

    private val glide = GlideApp.with(fragment).asBitmap()

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        val (type, category, track) = item.mediaId.toMediaId()

        return when {
            track != null -> R.id.view_type_track
            category == null -> Adapter.IGNORE_ITEM_VIEW_TYPE
            type == MediaId.TYPE_ALBUMS -> R.id.view_type_album
            type == MediaId.TYPE_ARTISTS -> R.id.view_type_artist
            type == MediaId.TYPE_PLAYLISTS -> R.id.view_type_playlist
            else -> Adapter.IGNORE_ITEM_VIEW_TYPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent, viewType)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
    }

    inner class ViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) : RecyclerView.ViewHolder(parent.inflate(R.layout.item_search_suggestion)) {

        private val iconView: ImageView = itemView.findViewById(R.id.icon_view)
        private val titleView: TextView = itemView.findViewById(R.id.title_view)

        init {
            setupTrackActionMenu(viewType)

            itemView.setOnClickListener {
                val selectedItem = getItem(adapterPosition)
                listener(selectedItem, ItemAction.PRIMARY)
            }
        }

        fun bind(result: MediaBrowserCompat.MediaItem): Unit = with(result.description) {
            titleView.text = title

            when (itemViewType) {
                R.id.view_type_track -> glide.fallback(R.drawable.ic_audiotrack_24dp).load(iconUri)
                R.id.view_type_album -> glide.fallback(R.drawable.ic_album_24dp).load(null as Uri?)
                R.id.view_type_artist -> glide.fallback(R.drawable.ic_person_24dp).load(null as Uri?)
                R.id.view_type_playlist -> glide.fallback(R.drawable.ic_playlist_24dp).load(iconUri)
                else -> error("Unexpected view type: $itemViewType")
            }.into(iconView)
        }

        private fun setupTrackActionMenu(viewType: Int) {
            val overflowIcon: ImageView = itemView.findViewById(R.id.overflow_icon)
            val isTrack = (viewType == R.id.view_type_track)
            overflowIcon.isVisible = isTrack

            if (isTrack) {
                val popup = PopupMenu(
                    itemView.context,
                    overflowIcon,
                    Gravity.END or Gravity.BOTTOM,
                    0,
                    R.style.Widget_AppTheme_PopupMenu_Overflow
                )

                popup.inflate(R.menu.track_popup_menu)
                popup.setOnMenuItemClickListener {
                    val item = getItem(adapterPosition)

                    when (it.itemId) {
                        R.id.action_playlist -> {
                            listener(item, ItemAction.ADD_TO_PLAYLIST)
                            true
                        }

                        R.id.action_delete -> {
                            listener(item, ItemAction.DELETE)
                            true
                        }

                        else -> false
                    }
                }

                overflowIcon.setOnClickListener {
                    popup.show()
                }
            }
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
}