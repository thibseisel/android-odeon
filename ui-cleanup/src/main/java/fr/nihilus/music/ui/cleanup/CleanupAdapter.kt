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
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.core.ui.base.BaseHolder
import fr.nihilus.music.media.usage.DisposableTrack
import fr.nihilus.music.ui.cleanup.databinding.ItemDisposableTrackBinding

/**
 * Displays tracks that could be safely deleted from the device's storage in a list.
 */
internal class CleanupAdapter : RecyclerView.Adapter<CleanupAdapter.ViewHolder>() {
    private val asyncDiffer = AsyncListDiffer(this, DisposableDiffer())

    val currentList: List<DisposableTrack>
        get() = asyncDiffer.currentList

    var selection: SelectionTracker<Long>? = null

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        val track = currentList[position]
        val isSelectedPosition = selection?.isSelected(track.trackId) ?: false

        // Reflect the selection state on the item.
        holder.markAsSelected(isSelectedPosition)

        // If the item has not been notified for a change to its selection state,
        // then (re)bind its item data.
        if (SelectionTracker.SELECTION_CHANGED_MARKER !in payloads) {
            holder.bind(track)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        onBindViewHolder(holder, position, emptyList())
    }

    override fun getItemCount(): Int = currentList.size

    override fun getItemId(position: Int): Long =
        if (hasStableIds()) {
            val track = currentList[position]
            track.trackId
        } else {
            RecyclerView.NO_ID
        }

    /**
     * Submits a new list to be diffed, and displayed.
     *
     * If a list is already being displayed, a diff will be computed on a background thread,
     * which will dispatch Adapter.notifyItem events on the main thread.
     *
     * @param list The new list to be displayed.
     */
    fun submitList(list: List<DisposableTrack>) {
        asyncDiffer.submitList(list)
    }

    /**
     * Holds references to views used to display a track row in the list.
     * @param parent Parent View to which the root view of this holder should be attached.
     */
    inner class ViewHolder(
        parent: ViewGroup
    ) : BaseHolder<DisposableTrack>(parent, R.layout.item_disposable_track) {

        private val context = parent.context
        private val binding = ItemDisposableTrackBinding.bind(itemView)

        /**
         * The detail of this track item.
         * This is used by the selection library to determine the behavior of individual items.
         */
        val itemDetails = TrackDetails(this)

        /**
         * Updates this holder's view to reflect the data in the provided [data].
         * @param data The metadata of the track to be displayed.
         */
        override fun bind(data: DisposableTrack) {
            binding.trackTitle.text = data.title
            binding.usageDescription.text = formatElapsedTimeSince(data.lastPlayedTime)
            binding.fileSize.text = formatToHumanReadableByteCount(data.fileSizeBytes)
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

    /**
     * Provides information about a specific row in the track list.
     *
     * @param holder The ViewHolder associated with the item at this position.
     */
    class TrackDetails(private val holder: ViewHolder) : ItemDetailsLookup.ItemDetails<Long>() {

        override fun getPosition(): Int = holder.adapterPosition

        override fun getSelectionKey(): Long = holder.itemId

        override fun inSelectionHotspot(e: MotionEvent): Boolean = true
    }

    private class DisposableDiffer : DiffUtil.ItemCallback<DisposableTrack>() {

        override fun areItemsTheSame(
            oldItem: DisposableTrack,
            newItem: DisposableTrack
        ): Boolean = oldItem.trackId == newItem.trackId

        override fun areContentsTheSame(
            oldItem: DisposableTrack,
            newItem: DisposableTrack
        ): Boolean = oldItem == newItem

    }
}