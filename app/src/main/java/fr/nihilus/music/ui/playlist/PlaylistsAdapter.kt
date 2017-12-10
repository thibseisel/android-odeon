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
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.utils.MediaID
import fr.nihilus.music.utils.MediaItemDiffCallback
import java.util.*

class PlaylistsAdapter(
        fragment: Fragment
) : RecyclerView.Adapter<PlaylistsAdapter.PlaylistHolder>() {

    private val mPlaylists = ArrayList<MediaItem>()
    private val mGlideRequest: RequestBuilder<Bitmap>
    private var mListener: OnPlaylistSelectedListener? = null

    init {
        val ctx = checkNotNull(fragment.context) { "Fragment is not attached." }
        val dummyAlbumArt = ContextCompat.getDrawable(ctx, R.drawable.ic_playlist_24dp)
        mGlideRequest = GlideApp.with(fragment).asBitmap()
                .error(dummyAlbumArt)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistHolder {
        val inflater = LayoutInflater.from(parent.context)
        val v = inflater.inflate(R.layout.playlist_item, parent, false)

        return PlaylistHolder(v).also { holder ->

            // Dispatch clicks on the whole item
            holder.itemView.setOnClickListener { _ ->
                mListener?.let {
                    val clickedItem = mPlaylists[holder.adapterPosition]
                    if (clickedItem.isBrowsable) {
                        // Only browsable items show their content
                        it.onPlaylistSelected(holder, clickedItem)
                    }
                }
            }

            // Dispatch clicks on the play action
            holder.actionPlay.setOnClickListener { _ ->
                mListener?.let {
                    val clickedItem = mPlaylists[holder.adapterPosition]
                    it.onPlay(clickedItem)
                }
            }
        }
    }

    override fun onBindViewHolder(holder: PlaylistHolder, position: Int) {
        holder.bind(mPlaylists[position], mGlideRequest)
    }

    override fun getItemCount() = mPlaylists.size

    override fun getItemId(position: Int): Long {
        if (hasStableIds()) {
            val mediaId = mPlaylists[position].mediaId
            return MediaID.extractMusicID(mediaId)?.toLong() ?: RecyclerView.NO_ID
        }

        return RecyclerView.NO_ID
    }

    fun setOnPlaylistSelectedListener(listener: OnPlaylistSelectedListener) {
        mListener = listener
    }

    fun update(newItems: List<MediaItem>) {
        if (newItems.isEmpty() && mPlaylists.isEmpty()) {
            // Dispatch a general change notification to update RecyclerFragment's empty state
            notifyDataSetChanged()
        } else {
            val diffCallback = MediaItemDiffCallback(mPlaylists, newItems)
            val result = DiffUtil.calculateDiff(diffCallback, false)
            mPlaylists.clear()
            mPlaylists.addAll(newItems)
            result.dispatchUpdatesTo(this)
        }
    }

    class PlaylistHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.title)
        private val image: ImageView = itemView.findViewById(R.id.albumArt)
        internal val actionPlay: View = itemView.findViewById(R.id.action_play)

        fun bind(playlistItem: MediaItem, glide: RequestBuilder<*>) {
            val description = playlistItem.description
            title.text = description.title
            glide.load(description.iconUri).into(image)

            // The play button is only shown if the item is playable
            actionPlay.visibility = if (playlistItem.isPlayable) View.VISIBLE else View.GONE
        }
    }

    interface OnPlaylistSelectedListener {
        fun onPlaylistSelected(holder: PlaylistHolder, playlist: MediaItem)
        fun onPlay(playlist: MediaItem)
    }
}
