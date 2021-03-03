/*
 * Copyright 2021 Thibault Seisel
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

package fr.nihilus.music.ui.settings.exclusion

import android.text.format.DateUtils
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.ui.settings.R
import fr.nihilus.music.ui.settings.databinding.ItemExcludedTrackBinding

internal class ExclusionAdapter : ListAdapter<ExcludedTrack, ExclusionAdapter.TrackHolder>(TrackDiffer()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = TrackHolder(parent)

    override fun onBindViewHolder(holder: TrackHolder, position: Int) {
        holder.bind(getItem(position))
    }

    public override fun getItem(position: Int): ExcludedTrack = super.getItem(position)

    class TrackHolder(
        parent: ViewGroup
    ) : BaseHolder<ExcludedTrack>(parent, R.layout.item_excluded_track) {
        private val binding = ItemExcludedTrackBinding.bind(itemView)

        override fun bind(data: ExcludedTrack) = with(binding) {
            trackTitle.text = data.title
            trackArtist.text = data.artistName
            exclusionTime.text = DateUtils.getRelativeTimeSpanString(
                root.context,
                data.excludeDate * 1000L,
                true
            )
        }
    }

    private class TrackDiffer : DiffUtil.ItemCallback<ExcludedTrack>() {
        override fun areItemsTheSame(
            oldItem: ExcludedTrack,
            newItem: ExcludedTrack
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: ExcludedTrack,
            newItem: ExcludedTrack
        ): Boolean = oldItem == newItem
    }
}