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

package fr.nihilus.music.library.cleanup

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.text.format.DateUtils
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.CompoundButton
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.R
import fr.nihilus.music.extensions.inflate
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.ui.MediaItemDiffer

/**
 * Payload indicating that the selected state of an item has changed.
 * The item should be partially updated to refresh its selection state.
 */
private const val SELECTED_STATE_CHANGED = 1

class CleanupAdapter : ListAdapter<Checkable<MediaItem>, CleanupAdapter.ViewHolder>(
    CheckableDiffer(MediaItemDiffer)
) {
    private var items = emptyList<Checkable<MediaItem>>()

    override fun submitList(list: List<Checkable<MediaItem>>?) {
        items = list.orEmpty()
        super.submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        onBindViewHolder(holder, position, emptyList())
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: List<Any>) {
        val track = getItem(position)

        if (payloads.any { it == SELECTED_STATE_CHANGED }) {
            // Only update the selection state.
            holder.setSelected(track.isChecked)
        } else {
            // Rebind the whole item.
            holder.bind(track, CompoundButton.OnCheckedChangeListener { _, isChecked ->
                val selectedPosition = holder.adapterPosition
                val selectedItem = getItem(selectedPosition)
                selectedItem.isChecked = isChecked
                notifyItemChanged(selectedPosition, isChecked)
            })
        }
    }

    fun getSelectedItems(): List<MediaItem> = items.asSequence()
        .filter { it.isChecked }
        .map { it.item }
        .toList()

    class ViewHolder(
        parent: ViewGroup
    ) : RecyclerView.ViewHolder(parent.inflate(R.layout.item_disposable_track)) {

        private val context = parent.context
        private val selectionMark = itemView.findViewById<CheckBox>(R.id.selection_mark)
        private val title = itemView.findViewById<TextView>(R.id.title)
        private val usageDescription = itemView.findViewById<TextView>(R.id.usage_description)
        private val fileSizeCaption = itemView.findViewById<TextView>(R.id.file_size)

        fun bind(track: Checkable<MediaItem>, checkedListener: CompoundButton.OnCheckedChangeListener) {
            val item = track.item.description
            val extras = item.extras!!
            val fileSizeBytes = extras.getLong(MediaItems.EXTRA_FILE_SIZE, 0)
            val lastPlayedTime = extras.takeIf { it.containsKey(MediaItems.EXTRA_LAST_PLAYED_TIME) }
                ?.getLong(MediaItems.EXTRA_LAST_PLAYED_TIME)

            title.text = item.title
            usageDescription.text = formatElapsedTimeSince(lastPlayedTime)
            fileSizeCaption.text = formatToHumanReadableByteCount(fileSizeBytes)

            setSelected(track.isChecked)
            selectionMark.setOnCheckedChangeListener(checkedListener)
        }

        fun setSelected(isSelected: Boolean) {
            itemView.isActivated = isSelected
            selectionMark.isChecked = isSelected
        }

        private fun formatElapsedTimeSince(epochTime: Long?): String =
            if (epochTime == null) context.getString(R.string.never_played)
            else context.getString(
                R.string.last_played_description,
                DateUtils.getRelativeTimeSpanString(epochTime)
            )
    }
}