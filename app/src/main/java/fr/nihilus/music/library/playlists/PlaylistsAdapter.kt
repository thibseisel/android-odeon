/*
 * Copyright 2020 Thibault Seisel
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

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import fr.nihilus.music.R
import fr.nihilus.music.ui.BaseAdapter

/**
 * Display playlist media items as a grid of floating cards.
 */
internal class PlaylistsAdapter(
    fragment: Fragment,
    private val listener: OnItemSelectedListener
) : BaseAdapter<PlaylistHolder>() {

    private val glideRequest = Glide.with(fragment).asBitmap()
        .fallback(R.drawable.ic_playlist_24dp)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistHolder {
        return PlaylistHolder(parent, glideRequest).also { holder ->
            holder.onAttachListeners(listener)
        }
    }
}