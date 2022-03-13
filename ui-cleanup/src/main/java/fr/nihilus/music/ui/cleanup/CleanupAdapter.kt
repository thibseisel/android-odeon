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

package fr.nihilus.music.ui.cleanup

import android.text.format.DateUtils
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.ui.cleanup.databinding.ItemDisposableTrackBinding

private const val SELECTION_CHANGED_PAYLOAD = "fr.nihilus.music.ui.SELECTION_CHANGED"

/**
 * Displays tracks that could be safely deleted from the device's storage in a list.
 */
internal class CleanupAdapter(
    private val onItemSelect: (CleanupState.Track) -> Unit
) : ListAdapter<CleanupState.Track, CleanupAdapter.ViewHolder>(TrackDiffer()) {

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent) {
        onItemSelect(getItem(it))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        val track = currentList[position]
        if (SELECTION_CHANGED_PAYLOAD in payloads) {
            holder.markAsSelected(track.selected)
        } else {
            holder.bind(track)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        onBindViewHolder(holder, position, emptyList())
    }

    override fun getItemId(position: Int): Long =
        if (hasStableIds()) {
            val track = currentList[position]
            track.id.track ?: RecyclerView.NO_ID
        } else {
            RecyclerView.NO_ID
        }

    /**
     * Holds references to views used to display a track row in the list.
     * @param parent Parent View to which the root view of this holder should be attached.
     */
    internal class ViewHolder(
        parent: ViewGroup,
        onSelect: (position: Int) -> Unit,
    ) : BaseHolder<CleanupState.Track>(parent, R.layout.item_disposable_track) {

        private val context = parent.context
        private val binding = ItemDisposableTrackBinding.bind(itemView)

        init {
            itemView.setOnClickListener { onSelect(adapterPosition) }
            binding.tickMark.setOnClickListener { onSelect(adapterPosition) }
        }

        /**
         * Updates this holder's view to reflect the data in the provided [data].
         * @param data The metadata of the track to be displayed.
         */
        override fun bind(data: CleanupState.Track) {
            binding.trackTitle.text = data.title
            binding.usageDescription.text = formatElapsedTimeSince(data.lastPlayedTime)
            binding.fileSize.text = formatToHumanReadableByteCount(data.fileSizeBytes)
            markAsSelected(data.selected)
        }

        fun markAsSelected(selected: Boolean) {
            binding.tickMark.isChecked = selected
            itemView.isActivated = selected
        }

        private fun formatElapsedTimeSince(epochTime: Long?): String =
            if (epochTime == null) context.getString(R.string.never_played)
            else context.getString(
                R.string.last_played_description,
                DateUtils.getRelativeTimeSpanString(context, epochTime * 1000L, true)
            )
    }

    private class TrackDiffer : DiffUtil.ItemCallback<CleanupState.Track>() {
        override fun areItemsTheSame(
            oldItem: CleanupState.Track,
            newItem: CleanupState.Track
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: CleanupState.Track,
            newItem: CleanupState.Track
        ): Boolean = oldItem == newItem

        override fun getChangePayload(
            oldItem: CleanupState.Track,
            newItem: CleanupState.Track
        ): Any? = when {
            oldItem.selected != newItem.selected -> SELECTION_CHANGED_PAYLOAD
            else -> null
        }
    }
}
