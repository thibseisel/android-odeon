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
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import fr.nihilus.music.Constants
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.GlideRequest
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.utils.MediaID

class MembersAdapter(
        fragment: Fragment,
        private val listener: BaseAdapter.OnItemSelectedListener
) : BaseAdapter<MembersAdapter.MembersHolder>() {

    private val glideRequest = GlideApp.with(fragment).asBitmap()
            .error(R.drawable.dummy_album_art)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MembersHolder =
            MembersHolder(parent, glideRequest).also { holder ->
                holder.onAttachListeners(listener)
            }

    override fun getItemId(position: Int): Long {
        return if (hasStableIds()) {
            val mediaId = items[position].mediaId!!
            MediaID.extractMusicID(mediaId)?.toLong() ?: RecyclerView.NO_ID
        } else RecyclerView.NO_ID
    }

    /**
     * Display a playlist's track.
     */
    class MembersHolder(
            parent: ViewGroup,
            private val glide: GlideRequest<Bitmap>
    ) : BaseAdapter.ViewHolder(parent, R.layout.song_list_item) {

        private val albumArt: ImageView = itemView.findViewById(R.id.cover)
        private val title: TextView = itemView.findViewById(R.id.title)
        private val subtitle: TextView = itemView.findViewById(R.id.subtitle)

        override fun onAttachListeners(client: BaseAdapter.OnItemSelectedListener) {
            itemView.setOnClickListener { _ ->
                client.onItemSelected(adapterPosition, Constants.ACTION_PLAY)
            }
        }

        override fun onBind(item: MediaBrowserCompat.MediaItem) {
            val description = item.description
            title.text = description.title
            subtitle.text = description.subtitle
            glide.load(description.iconUri).into(albumArt)
        }
    }

}
