/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.ui.library.playlists.members

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ListAdapter
import com.bumptech.glide.Glide
import fr.nihilus.music.core.ui.base.MediaItemDiffer
import fr.nihilus.music.core.ui.R as CoreUiR

internal class MembersAdapter(
    fragment: Fragment,
    private val onTrackSelected: (position: Int) -> Unit
) : ListAdapter<MediaItem, MembersHolder>(MediaItemDiffer) {

    // Note: Glide is attached to the context of the activity to workaround a bug in
    // MaterialContainerTransform not capturing images in return transition.
    private val glideRequest = Glide.with(fragment.requireActivity()).asBitmap()
        .error(CoreUiR.drawable.ic_audiotrack_24dp)
        .autoClone()

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): MembersHolder = MembersHolder(parent, glideRequest, onTrackSelected)

    override fun onBindViewHolder(holder: MembersHolder, position: Int) {
        holder.bind(getItem(position))
    }

    public override fun getItem(position: Int): MediaItem = super.getItem(position)
}
