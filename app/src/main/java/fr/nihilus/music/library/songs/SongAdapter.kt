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

package fr.nihilus.music.library.songs

import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat
import android.text.format.DateUtils
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SectionIndexer
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.R
import fr.nihilus.music.core.media.MediaItems
import fr.nihilus.music.core.ui.base.ListAdapter
import fr.nihilus.music.ui.AlphaSectionIndexer

/**
 * Bridge each media track to its list UI representation.
 *
 * @param fragment The fragment in which the list is displayed.
 * @param actionListener A function to be called when an action is triggered on a single track item.
 */
class SongAdapter(
    fragment: Fragment,
    private val actionListener: (MediaBrowserCompat.MediaItem, ItemAction) -> Unit
) : ListAdapter<MediaBrowserCompat.MediaItem, SongAdapter.ViewHolder>(), SectionIndexer {

    private val indexer = AlphaSectionIndexer()
    private val glideRequest = Glide.with(fragment).asBitmap()
        .error(R.drawable.ic_audiotrack_24dp)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position], glideRequest)
    }

    override fun getSections(): Array<out Any> = indexer.sections

    override fun getSectionForPosition(position: Int) = indexer.getSectionForPosition(position)

    override fun getPositionForSection(sectionIndex: Int) = indexer.getPositionForSection(sectionIndex)

    override fun submitList(newList: List<MediaBrowserCompat.MediaItem>) {
        updateIndexer(newList)
        super.submitList(newList)
    }

    private fun updateIndexer(newItems: List<MediaBrowserCompat.MediaItem>) {
        val titleSequence = newItems.asSequence().map { it.description.title?.toString() ?: "" }
        indexer.update(titleSequence)
    }

    /**
     * Holds the UI representation of a track.
     *
     * @param parent The parent list view.
     */
    inner class ViewHolder(parent: ViewGroup) : ListAdapter.ViewHolder(parent, R.layout.song_list_item) {
        private val titleView: TextView = itemView.findViewById(R.id.title)
        private val subtitleView: TextView = itemView.findViewById(R.id.subtitle_view)
        private val cover: ImageView = itemView.findViewById(R.id.album_art_view)

        init {
            // Open the popup menu when the overflow icon is clicked.
            val overflowIcon = itemView.findViewById<ImageView>(R.id.overflow_icon)
            val popup = PopupMenu(
                itemView.context,
                overflowIcon,
                Gravity.BOTTOM or Gravity.END,
                0,
                R.style.Widget_Odeon_PopupMenu_Overflow
            )
            popup.inflate(R.menu.track_popup_menu)

            popup.setOnMenuItemClickListener { item ->
                val track = items[position]

                when (item.itemId) {
                    R.id.action_playlist -> {
                        actionListener(track, ItemAction.ADD_TO_PLAYLIST)
                        true
                    }

                    R.id.action_delete -> {
                        actionListener(track, ItemAction.DELETE)
                        true
                    }

                    else -> false
                }
            }

            overflowIcon.setOnClickListener {
                popup.show()
            }

            itemView.setOnClickListener {
                val track = items[position]
                actionListener(track, ItemAction.PLAY)
            }
        }

        fun bind(item: MediaBrowserCompat.MediaItem, glide: RequestBuilder<Bitmap>) {
            with(item.description) {
                glide.load(iconUri).into(cover)
                titleView.text = title
                bindSubtitle(subtitleView, subtitle, extras!!.getLong(MediaItems.EXTRA_DURATION))
            }
        }

        private fun bindSubtitle(textView: TextView, text: CharSequence?, durationMillis: Long) {
            val duration = DateUtils.formatElapsedTime(durationMillis / 1000L)
            textView.text = textView.context.getString(R.string.song_item_subtitle, text, duration)
        }
    }

    /**
     * Enumeration of actions that could be performed on a single track item.
     */
    enum class ItemAction {

        /**
         * Start playback of the selected track.
         */
        PLAY,

        /**
         * Append the selected track to a user-defined playlist.
         */
        ADD_TO_PLAYLIST,

        /**
         * Delete the selected track from the device.
         */
        DELETE
    }
}
