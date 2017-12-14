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
import android.view.ViewGroup
import com.bumptech.glide.RequestBuilder
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.holder.PlaylistHolder
import fr.nihilus.music.utils.MediaID
import java.util.*

/**
 * Display playlist media items as a grid of floating cards.
 */
internal class PlaylistsAdapter(
        fragment: Fragment,
        private val listener: BaseAdapter.OnItemSelectedListener
) : BaseAdapter<PlaylistHolder>() {

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

}
