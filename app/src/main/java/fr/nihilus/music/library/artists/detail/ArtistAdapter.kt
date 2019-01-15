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

package fr.nihilus.music.library.artists.detail

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.library.artists.ArtistHolder
import fr.nihilus.music.media.musicIdFrom
import fr.nihilus.music.ui.BaseAdapter

internal class ArtistAdapter(
    fragment: androidx.fragment.app.Fragment,
    private val listener: BaseAdapter.OnItemSelectedListener
) : BaseAdapter<ArtistHolder>() {

    private val glide = GlideApp.with(fragment).asBitmap()
        .fallback(R.drawable.ic_person_24dp)
        .centerCrop()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistHolder {
        return ArtistHolder(parent, glide).also { holder ->
            holder.onAttachListeners(listener)
        }
    }

    override fun getItemId(position: Int): Long {
        if (hasStableIds()) {
            val mediaId = getItem(position).mediaId
            return musicIdFrom(mediaId)?.toLong() ?: androidx.recyclerview.widget.RecyclerView.NO_ID
        }
        return androidx.recyclerview.widget.RecyclerView.NO_ID
    }

}
