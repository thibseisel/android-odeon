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
import android.support.v4.content.ContextCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.Constants
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.utils.MediaID
import java.util.*

/**
 * Display playlist media items as a grid of floating cards.
 */
class PlaylistsAdapter(
        fragment: Fragment,
        private val listener: BaseAdapter.OnItemSelectedListener
) : BaseAdapter<PlaylistsAdapter.PlaylistHolder>() {

    private val playlists = ArrayList<MediaItem>()
    private val glideRequest: RequestBuilder<Bitmap>

    init {
        val ctx = checkNotNull(fragment.context) { "Fragment is not attached." }
        val dummyAlbumArt = ContextCompat.getDrawable(ctx, R.drawable.ic_playlist_24dp)
        glideRequest = GlideApp.with(fragment).asBitmap()
                .error(dummyAlbumArt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistHolder {
        return PlaylistHolder(parent, glideRequest).also { holder ->
            holder.onAttachListeners(listener)
        }
    }

    override fun getItemId(position: Int): Long {
        return if (hasStableIds()) {
            val mediaId = playlists[position].mediaId
            MediaID.extractMusicID(mediaId)?.toLong() ?: RecyclerView.NO_ID
        } else RecyclerView.NO_ID
    }

    /**
     * Display a playlist as a floating grid item.
     * Playlists that are marked as playable could be played by taping the play action icon.
     */
    class PlaylistHolder(
            parent: ViewGroup,
            private val glide: RequestBuilder<Bitmap>
    ) : BaseAdapter.ViewHolder(parent, R.layout.playlist_item) {

        private val title: TextView = itemView.findViewById(R.id.title)
        private val image: ImageView = itemView.findViewById(R.id.albumArt)
        private val actionPlay: View = itemView.findViewById(R.id.action_play)

        override fun onAttachListeners(client: BaseAdapter.OnItemSelectedListener) {

            itemView.setOnClickListener { _ ->
                client.onItemSelected(adapterPosition, Constants.ACTION_BROWSE)
            }

            actionPlay.setOnClickListener { _ ->
                client.onItemSelected(adapterPosition, Constants.ACTION_PLAY)
            }
        }

        override fun onBind(item: MediaItem) {
            val description = item.description
            title.text = description.title
            glide.load(description.iconUri).into(image)

            // The play button is only shown if the item is playable
            actionPlay.visibility = if (item.isPlayable) View.VISIBLE else View.GONE
        }
    }
}
