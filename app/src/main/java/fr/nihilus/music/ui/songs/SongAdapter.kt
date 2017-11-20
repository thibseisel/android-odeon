/*
 * Copyright 2017 Thibault Seisel
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
import android.support.v7.content.res.AppCompatResources
import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.SectionIndexer
import android.widget.TextView
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.GlideRequest
import fr.nihilus.music.inflate
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.utils.MediaID
import fr.nihilus.music.utils.MediaItemIndexer

class SongAdapter(fragment: Fragment) : BaseAdapter(), SectionIndexer {

    private val mGlideRequest: GlideRequest<Bitmap>
    private val mIndexer: MediaItemIndexer
    private val mSongs = ArrayList<MediaBrowserCompat.MediaItem>()

    init {
        mIndexer = MediaItemIndexer(mSongs)
        registerDataSetObserver(mIndexer)

        val defaultAlbumArt = AppCompatResources.getDrawable(fragment.context!!,
                R.drawable.dummy_album_art)

        mGlideRequest = GlideApp.with(fragment).asBitmap()
                .error(defaultAlbumArt)
                .fitCenter()
    }

    override fun getCount() = mSongs.size

    override fun getItem(pos: Int) = mSongs[pos]

    override fun hasStableIds() = true

    override fun getItemId(pos: Int): Long {
        if (hasStableIds()) {
            val mediaId = mSongs[pos].mediaId
            return MediaID.extractMusicID(mediaId)!!.toLong()
        }
        return -1L
    }

    override fun getView(pos: Int, convertView: View?, parent: ViewGroup): View {
        val itemView = convertView ?: createItemView(parent)

        val holder = itemView.tag as ViewHolder
        bindViewHolder(holder, pos)

        return itemView
    }

    private fun bindViewHolder(holder: ViewHolder, position: Int) {
        val item = mSongs[position]
        holder.bind(item, mGlideRequest)
    }

    private fun createItemView(parent: ViewGroup): View {
        return parent.inflate(R.layout.song_list_item, false).also {
            it.tag = ViewHolder(it)
        }
    }

    override fun getSections(): Array<out Any> = mIndexer.sections

    override fun getPositionForSection(sectionIndex: Int) =
            mIndexer.getPositionForSection(sectionIndex)

    override fun getSectionForPosition(position: Int) =
            mIndexer.getSectionForPosition(position)

    fun updateItems(newItems: List<MediaBrowserCompat.MediaItem>) {
        mSongs.clear()
        mSongs.addAll(newItems)
        notifyDataSetChanged()
    }

    private class ViewHolder(itemView: View) {
        private val mTitle: TextView = itemView.findViewById(R.id.title)
        private val mSubtitle: TextView = itemView.findViewById(R.id.subtitle)
        private val mCover: ImageView = itemView.findViewById(R.id.cover)

        fun bind(item: MediaBrowserCompat.MediaItem, glide: GlideRequest<*>) {
            with(item.description) {
                mTitle.text = title
                bindSubtitle(mSubtitle, subtitle, extras!!.getLong(MediaItems.EXTRA_DURATION))
                glide.load(iconUri).into(mCover)
            }
        }

        private fun bindSubtitle(textView: TextView, text: CharSequence?, durationMillis: Long) {
            val duration = DateUtils.formatElapsedTime(durationMillis / 1000)
            val subtitle = textView.context.getString(R.string.song_item_subtitle, text, duration)
            textView.text = subtitle
        }
    }
}
