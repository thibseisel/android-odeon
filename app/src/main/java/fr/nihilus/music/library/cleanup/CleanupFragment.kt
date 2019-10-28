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

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.selection.*
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.R
import fr.nihilus.music.core.media.MediaItems
import fr.nihilus.music.core.ui.ConfirmDialogFragment
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.startActionMode
import fr.nihilus.music.extensions.sumByLong
import kotlinx.android.synthetic.main.fragment_cleanup.*

/**
 * Code associated with the request to confirm deleting tracks.
 */
private const val REQUEST_CONFIRM_CLEANUP = 1337

/**
 * Lists tracks that could be deleted from the device's storage to free-up space.
 */
class CleanupFragment : BaseFragment(R.layout.fragment_cleanup) {

    private val viewModel by viewModels<CleanupViewModel> { viewModelFactory }
    private lateinit var selectionTracker: SelectionTracker<MediaBrowserCompat.MediaItem>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = disposable_track_list
        recyclerView.setHasFixedSize(true)
        val dividers = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(dividers)

        val adapter = CleanupAdapter()
        recyclerView.adapter = adapter

        selectionTracker = SelectionTracker.Builder(
            "disposable_tracks_selection",
            recyclerView,
            TrackKeyProvider(adapter),
            TrackDetailLookup(recyclerView),
            StorageStrategy.createParcelableStorage(MediaBrowserCompat.MediaItem::class.java)
        ).build().also {
            adapter.selection = it
            it.addObserver(HasSelectionObserver(it.selection))
        }

        action_delete_selected.setOnClickListener {
            askCleanupConfirmation(selectionTracker.selection)
        }

        check_all_label.setOnClickListener {
            check_all_box.isChecked = !check_all_box.isChecked
        }

        check_all_box.setOnCheckedChangeListener { _, isChecked ->
            val allTracks = adapter.currentList
            selectionTracker.setItemsSelected(allTracks, isChecked)
        }

        viewModel.tracks.observe(this) { disposableTracksRequest ->
            when (disposableTracksRequest) {
                is LoadRequest.Success -> {
                    adapter.submitList(disposableTracksRequest.data)
                }
            }
        }

        if (savedInstanceState != null) {
            // Restore selected positions.
            selectionTracker.onRestoreInstanceState(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectionTracker.onSaveInstanceState(outState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CONFIRM_CLEANUP && resultCode == DialogInterface.BUTTON_POSITIVE) {
            val selectedTracks = selectionTracker.selection.toList()
            viewModel.deleteTracks(selectedTracks)
            selectionTracker.clearSelection()
        }
    }

    private fun askCleanupConfirmation(deletedTracks: Selection<MediaBrowserCompat.MediaItem>) {
        val selected = deletedTracks.size()
        val dialog = ConfirmDialogFragment.newInstance(
            this,
            REQUEST_CONFIRM_CLEANUP,
            resources.getQuantityString(R.plurals.cleanup_confirmation_title, selected, selected),
            getString(R.string.cleanup_confirmation_message),
            R.string.action_delete,
            R.string.cancel
        )

        dialog.show(parentFragmentManager, null)
    }

    private fun setFabVisibility(visible: Boolean) {
        if (visible) {
            action_delete_selected.show()
        } else {
            action_delete_selected.hide()
        }
    }

    /**
     * Associate selection keys to positions in the adapter.
     */
    private class TrackKeyProvider(
        private val adapter: CleanupAdapter
    ) : ItemKeyProvider<MediaBrowserCompat.MediaItem>(SCOPE_CACHED) {

        override fun getKey(position: Int): MediaBrowserCompat.MediaItem? {
            val items = adapter.currentList
            return items.getOrNull(position)
        }

        override fun getPosition(key: MediaBrowserCompat.MediaItem): Int {
            val currentItems = adapter.currentList
            val trackId = key.mediaId
            val indexOfKey = currentItems.indexOfFirst { it.mediaId == trackId }
            return if (indexOfKey != -1) indexOfKey else RecyclerView.NO_POSITION
        }
    }

    /**
     * Provides the detail of items in the selectable list from their ViewHolder.
     */
    private class TrackDetailLookup(
        private val view: RecyclerView
    ) : ItemDetailsLookup<MediaBrowserCompat.MediaItem>() {

        override fun getItemDetails(e: MotionEvent): ItemDetails<MediaBrowserCompat.MediaItem>? {
            return view.findChildViewUnder(e.x, e.y)
                ?.let { view.getChildViewHolder(it) as? CleanupAdapter.ViewHolder }
                ?.itemDetails
        }
    }

    private inner class HasSelectionObserver(
        private val liveSelection: Selection<MediaBrowserCompat.MediaItem>
    ) : SelectionTracker.SelectionObserver<MediaBrowserCompat.MediaItem>(),
        ActionMode.Callback {

        private var hadSelection = false
        private var actionMode: ActionMode? = null

        override fun onSelectionChanged() {
            val hasSelection = !liveSelection.isEmpty
            if (hadSelection != hasSelection) {
                setFabVisibility(hasSelection)
                toggleActionMode(hasSelection)
                hadSelection = hasSelection
            }
        }

        override fun onItemStateChanged(key: MediaBrowserCompat.MediaItem, selected: Boolean) {
            updateActionModeText()
        }

        override fun onSelectionRestored() {
            val hasSelection = !liveSelection.isEmpty
            setFabVisibility(hasSelection)
            toggleActionMode(hasSelection)
            hadSelection = hasSelection
        }

        private fun toggleActionMode(hasSelection: Boolean) {
            if (hasSelection && actionMode == null) {
                actionMode = startActionMode(this)
                updateActionModeText()
            } else if (!hasSelection) {
                actionMode?.finish()
            }
        }

        private fun updateActionModeText() {
            actionMode?.let { mode ->
                val selectedCount = liveSelection.size()
                mode.title = resources.getQuantityString(
                    R.plurals.number_of_selected_tracks,
                    selectedCount,
                    selectedCount
                )

                val freedBytes = liveSelection.sumByLong {
                    it.description.extras?.getLong(MediaItems.EXTRA_FILE_SIZE) ?: 0L
                }
                mode.subtitle = formatToHumanReadableByteCount(freedBytes)
            }
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean = true

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            selectionTracker.clearSelection()
            this@CleanupFragment.check_all_box.isChecked = false
        }
    }
}