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

package fr.nihilus.music.ui.albums

import android.support.v4.media.MediaMetadataCompat
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import fr.nihilus.music.media.extensions.id
import fr.nihilus.music.media.musicIdFrom
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.holder.AlbumTrackHolder

internal class TrackAdapter(
    private val listener: BaseAdapter.OnItemSelectedListener
) : BaseAdapter<AlbumTrackHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumTrackHolder {
        return AlbumTrackHolder(parent).also { holder ->
            holder.onAttachListeners(listener)
        }
    }

    override fun getItemId(position: Int): Long {
        if (hasStableIds()) {
            val mediaId = getItem(position).mediaId
            return musicIdFrom(mediaId)?.toLong() ?: RecyclerView.NO_ID
        }

        return RecyclerView.NO_ID
    }

    /**
     * Retrieve the position of a given metadata in the adapter.
     *
     * @param metadata Metadata whose music id matches the position of the searched track.
     * @return Position in the adapter, on `-1` if not found.
     */
    fun indexOf(metadata: MediaMetadataCompat): Int {
        // Assume the passed musicId is from ALBUMS category
        val musicId = metadata.id

        for (position in 0 until itemCount) {
            val item = getItem(position)
            if (item.mediaId!!.endsWith(musicId)) {
                return position
            }
        }

        return -1
    }

}
