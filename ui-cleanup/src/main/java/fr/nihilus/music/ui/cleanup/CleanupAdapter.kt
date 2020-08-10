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

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.text.format.DateUtils
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.core.media.MediaItems
import fr.nihilus.music.core.ui.base.MediaItemDiffer
import fr.nihilus.music.core.ui.extensions.inflate

/**
 * Displays tracks that could be safely deleted from the device's storage in a list.
 */
internal class CleanupAdapter : RecyclerView.Adapter<CleanupAdapter.ViewHolder>() {
    private val asyncDiffer = AsyncListDiffer(this, MediaItemDiffer)

    val currentList: List<MediaItem>
        get() = asyncDiffer.currentList

    var selection: SelectionTracker<MediaItem>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        val track = currentList[position]
        val isSelectedPosition = selection?.isSelected(track) ?: false

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

    /**
     * Submits a new list to be diffed, and displayed.
     *
     * If a list is already being displayed, a diff will be computed on a background thread,
     * which will dispatch Adapter.notifyItem events on the main thread.
     *
     * @param list The new list to be displayed.
     */
    fun submitList(list: List<MediaItem>) {
        asyncDiffer.submitList(list)
    }

    /**
     * Holds references to views used to display a track row in the list.
     * @param parent Parent View to which the root view of this holder should be attached.
     */
    inner class ViewHolder(
        parent: ViewGroup
    ) : RecyclerView.ViewHolder(parent.inflate(R.layout.item_disposable_track)) {

        private val context = parent.context
        private val tickMark = itemView.findViewById<CheckBox>(R.id.selection_mark)
        private val title = itemView.findViewById<TextView>(R.id.title)
        private val usageDescription = itemView.findViewById<TextView>(R.id.usage_description)
        private val fileSizeCaption = itemView.findViewById<TextView>(R.id.file_size)

        /**
         * The detail of this track item.
         * This is used by the selection library to determine the behavior of individual items.
         */
        val itemDetails = TrackDetails(this) { position ->
            currentList.getOrNull(position)
        }

        /**
         * Updates this holder's view to reflect the data in the provided [track].
         * @param track The metadata of the track to be displayed.
         */
        fun bind(track: MediaItem) {
            val item = track.description
            val extras = item.extras!!

            val fileSizeBytes = extras.getLong(MediaItems.EXTRA_FILE_SIZE, 0)
            val lastPlayedTime = extras.takeIf { it.containsKey(MediaItems.EXTRA_LAST_PLAYED_TIME) }
                ?.getLong(MediaItems.EXTRA_LAST_PLAYED_TIME)

            title.text = item.title
            usageDescription.text = formatElapsedTimeSince(lastPlayedTime)
            fileSizeCaption.text = formatToHumanReadableByteCount(fileSizeBytes)
        }

        fun markAsSelected(selected: Boolean) {
            tickMark.isChecked = selected
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
     * @param itemProvider A function that returns a track given its adapter position.
     */
    class TrackDetails(
        private val holder: ViewHolder,
        private val itemProvider: (position: Int) -> MediaItem?
    ) : ItemDetailsLookup.ItemDetails<MediaItem>() {

        override fun getPosition(): Int = holder.adapterPosition

        override fun getSelectionKey(): MediaItem? {
            val itemPosition = holder.adapterPosition
            return itemProvider(itemPosition)
        }

        override fun inSelectionHotspot(e: MotionEvent): Boolean = true
    }
}