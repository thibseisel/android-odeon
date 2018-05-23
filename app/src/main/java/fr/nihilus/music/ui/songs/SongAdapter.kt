/*
 * Copyright 2018 Thibault Seisel
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

package fr.nihilus.music.ui.songs

import android.graphics.Bitmap
import android.support.v4.app.Fragment
import android.support.v4.media.MediaBrowserCompat
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.SectionIndexer
import android.widget.TextView
import com.bumptech.glide.load.resource.bitmap.FitCenter
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.GlideRequest
import fr.nihilus.music.inflate
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.media.musicIdFrom
import fr.nihilus.music.utils.MediaItemIndexer

class SongAdapter(fragment: Fragment) : BaseAdapter(), SectionIndexer {

    private val songs = ArrayList<MediaBrowserCompat.MediaItem>()
    private val indexer = MediaItemIndexer(songs)
    private val glideRequest: GlideRequest<Bitmap>

    init {
        val context = checkNotNull(fragment.context) { "Fragment is not attached." }
        val cornerRadius = context.resources.getDimensionPixelSize(R.dimen.track_icon_corner_radius)

        registerDataSetObserver(indexer)
        glideRequest = GlideApp.with(fragment).asBitmap()
            .transforms(FitCenter(), RoundedCorners(cornerRadius))
            .fallback(R.drawable.ic_audiotrack_24dp)
    }

    override fun getCount() = songs.size

    override fun getItem(pos: Int) = songs[pos]

    override fun hasStableIds() = true

    override fun getItemId(pos: Int): Long {
        if (hasStableIds()) {
            val mediaId = songs[pos].mediaId
            return musicIdFrom(mediaId)?.toLong() ?: -1L
        }
        return -1L
    }

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: createItemView(parent)

        val holder = itemView.tag as ViewHolder
        bindViewHolder(holder, pos)

        return itemView
    }

    private fun createItemView(parent: ViewGroup): View {
        return parent.inflate(R.layout.song_list_item, false).apply {
            tag = ViewHolder(this)
        }
    }

    private fun bindViewHolder(holder: ViewHolder, position: Int) {
        val item = songs[position]
        holder.bind(item, glideRequest)
    }

    override fun getSections(): Array<out Any> = indexer.sections

    override fun getPositionForSection(sectionIndex: Int) =
        indexer.getPositionForSection(sectionIndex)

    override fun getSectionForPosition(position: Int) =
        indexer.getSectionForPosition(position)

    fun updateItems(newItems: List<MediaBrowserCompat.MediaItem>) {
        songs.clear()
        songs.addAll(newItems)
        notifyDataSetChanged()
    }

    private class ViewHolder(itemView: View) {
        private val titleView: TextView = itemView.findViewById(R.id.title)
        private val subtitleView: TextView = itemView.findViewById(R.id.subtitleView)
        private val cover: ImageView = itemView.findViewById(R.id.albumArtView)

        fun bind(item: MediaBrowserCompat.MediaItem, glide: GlideRequest<*>) {
            with(item.description) {
                glide.load(iconUri).into(cover)
                titleView.text = title
                bindSubtitle(subtitleView, subtitle, extras!!.getLong(MediaItems.EXTRA_DURATION))
            }
        }

        private fun bindSubtitle(textView: TextView, text: CharSequence?, durationMillis: Long) {
            val duration = DateUtils.formatElapsedTime(durationMillis / 1000L)
            val subtitle = textView.context.getString(R.string.song_item_subtitle, text, duration)
            textView.text = subtitle
        }
    }
}
