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

package fr.nihilus.music.ui.playlist

import android.graphics.Bitmap
import android.support.v4.app.Fragment
import android.support.v4.media.MediaBrowserCompat
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.GlideRequest
import fr.nihilus.music.inflate
import fr.nihilus.music.utils.MediaItemDiffCallback

class MembersAdapter(fragment: Fragment) : RecyclerView.Adapter<MembersHolder>() {
    private val mItems: MutableList<MediaBrowserCompat.MediaItem> = ArrayList()
    private val glideRequest = GlideApp.with(fragment).asBitmap()
            .error(R.drawable.dummy_album_art)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MembersHolder =
            MembersHolder(parent, glideRequest)

    override fun onBindViewHolder(holder: MembersHolder, position: Int) {
        holder.bind(mItems[position])
    }

    override fun getItemCount() = mItems.size

    fun update(newItems: List<MediaBrowserCompat.MediaItem>) {
        val diffCallback = MediaItemDiffCallback(mItems, newItems)
        val result = DiffUtil.calculateDiff(diffCallback)
        mItems.clear()
        mItems.addAll(newItems)
        result.dispatchUpdatesTo(this)
    }
}

class MembersHolder(parent: ViewGroup, private val artLoader: GlideRequest<Bitmap>)
    : RecyclerView.ViewHolder(parent.inflate(R.layout.song_list_item)) {

    private val albumArt: ImageView = itemView.findViewById(R.id.cover)
    private val title: TextView = itemView.findViewById(R.id.title)
    private val subtitle: TextView = itemView.findViewById(R.id.subtitle)

    fun bind(item: MediaBrowserCompat.MediaItem) {
        item.description.also {
            title.text = it.title
            subtitle.text = it.subtitle
            artLoader.load(it.iconUri).into(albumArt)
        }
    }
}