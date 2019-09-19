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

package fr.nihilus.music.library.songs

import android.graphics.Bitmap
import android.support.v4.media.MediaBrowserCompat
import android.text.format.DateUtils
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SectionIndexer
import android.widget.TextView
import androidx.fragment.app.Fragment
import fr.nihilus.music.R
import fr.nihilus.music.common.media.MediaItems
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.GlideRequest
import fr.nihilus.music.ui.AlphaSectionIndexer
import fr.nihilus.music.ui.ListAdapter

class SongAdapter(
    fragment: Fragment
) : ListAdapter<MediaBrowserCompat.MediaItem, SongAdapter.ViewHolder>(), SectionIndexer {

    private val indexer = AlphaSectionIndexer()
    private val glideRequest: GlideRequest<Bitmap>

    init {
        val context = fragment.requireContext()
        val cornerRadius = context.resources.getDimensionPixelSize(R.dimen.track_icon_corner_radius)

        glideRequest = GlideApp.with(fragment).asBitmap()
            .roundedCorners(cornerRadius)
            .fallback(R.drawable.ic_audiotrack_24dp)
    }

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

    class ViewHolder(parent: ViewGroup) : ListAdapter.ViewHolder(parent, R.layout.song_list_item) {
        private val titleView: TextView = itemView.findViewById(R.id.title)
        private val subtitleView: TextView = itemView.findViewById(R.id.subtitle_view)
        private val cover: ImageView = itemView.findViewById(R.id.album_art_view)

        fun bind(item: MediaBrowserCompat.MediaItem, glide: GlideRequest<*>) {
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
}
