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

package fr.nihilus.music.ui.artists

import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.holder.ArtistHolder
import fr.nihilus.music.utils.MediaID

internal class ArtistAdapter(
    fragment: Fragment,
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
            val mediaId = items[position].mediaId
            return MediaID.extractMusicID(mediaId)?.toLong() ?: RecyclerView.NO_ID
        }
        return RecyclerView.NO_ID
    }

}
