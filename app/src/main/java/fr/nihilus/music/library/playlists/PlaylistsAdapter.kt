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

package fr.nihilus.music.library.playlists

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.media.musicIdFrom
import fr.nihilus.music.ui.BaseAdapter
import java.util.*

/**
 * Display playlist media items as a grid of floating cards.
 */
internal class PlaylistsAdapter(
    fragment: androidx.fragment.app.Fragment,
    private val listener: BaseAdapter.OnItemSelectedListener
) : BaseAdapter<PlaylistHolder>() {

    private val playlists = ArrayList<MediaItem>()
    private val glideRequest = GlideApp.with(fragment).asBitmap()
        .fallback(R.drawable.ic_playlist_24dp)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistHolder {
        return PlaylistHolder(parent, glideRequest).also { holder ->
            holder.onAttachListeners(listener)
        }
    }

    override fun getItemId(position: Int): Long {
        return if (hasStableIds()) {
            val mediaId = playlists[position].mediaId
            musicIdFrom(mediaId)?.toLong() ?: androidx.recyclerview.widget.RecyclerView.NO_ID
        } else androidx.recyclerview.widget.RecyclerView.NO_ID
    }

}
