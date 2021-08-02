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

package fr.nihilus.music.ui.cleanup

import android.os.Bundle
import android.view.*
import androidx.appcompat.view.ActionMode
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import androidx.recyclerview.selection.*
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.core.collections.associateByLong
import fr.nihilus.music.core.ui.ConfirmDialogFragment
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.doOnApplyWindowInsets
import fr.nihilus.music.core.ui.extensions.startActionMode
import fr.nihilus.music.core.ui.view.DividerItemDecoration
import fr.nihilus.music.ui.cleanup.databinding.FragmentCleanupBinding

/**
 * Code associated with the request to confirm deleting tracks.
 */
private const val REQUEST_CONFIRM_CLEANUP = "fr.nihilus.music.request.CONFIRM_CLEANUP"

/**
 * Lists tracks that could be deleted from the device's storage to free-up space.
 */
@AndroidEntryPoint
internal class CleanupFragment : BaseFragment(R.layout.fragment_cleanup) {

    private var binding: FragmentCleanupBinding? = null

    private val viewModel by viewModels<CleanupViewModel>()
    private lateinit var adapter: CleanupAdapter
    private lateinit var selectionTracker: SelectionTracker<Long>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentCleanupBinding.bind(view)
        this.binding = binding

        val recyclerView = binding.disposableTrackList

        recyclerView.setHasFixedSize(true)
        val dividers = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        recyclerView.addItemDecoration(dividers)

        adapter = CleanupAdapter()
        recyclerView.adapter = adapter

        selectionTracker = SelectionTracker.Builder(
            "track_ids_selection",
            recyclerView,
            StableIdKeyProvider(recyclerView),
            TrackDetailLookup(recyclerView),
            StorageStrategy.createLongStorage()
        ).build().also {
            adapter.selection = it
            it.addObserver(HasSelectionObserver(it.selection))
        }

        configureViewOffsetForSystemBars(binding)

        binding.deleteTracksButton.setOnClickListener {
            askCleanupConfirmation(selectionTracker.selection)
        }

        viewModel.tracks.observe(viewLifecycleOwner) { disposableTracksRequest ->
            when (disposableTracksRequest) {
                is LoadRequest.Success -> {
                    adapter.submitList(disposableTracksRequest.data)
                }
            }
        }

        ConfirmDialogFragment.registerForResult(this, REQUEST_CONFIRM_CLEANUP) { result ->
            if (result == ConfirmDialogFragment.ActionButton.POSITIVE) {
                val tracksById = adapter.currentList.associateByLong { it.trackId }
                val selectedTracks = selectionTracker.selection.mapNotNull { trackId ->
                    tracksById[trackId]
                }

                viewModel.deleteTracks(selectedTracks)
                selectionTracker.clearSelection()
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun configureViewOffsetForSystemBars(bindings: FragmentCleanupBinding) {
        bindings.disposableTrackList.doOnApplyWindowInsets { view, insets, padding, _ ->
            val systemWindowInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(bottom = padding.bottom + systemWindowInsets.bottom)
        }

        bindings.deleteTracksButton.doOnApplyWindowInsets { view, insets, _, margin ->
            val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
            val tappableWindowInsets = insets.getInsets(WindowInsetsCompat.Type.tappableElement())
            layoutParams.bottomMargin = margin.bottom + tappableWindowInsets.bottom
        }
    }

    private fun askCleanupConfirmation(deletedTracks: Selection<Long>) {
        val selected = deletedTracks.size()
        ConfirmDialogFragment.open(
            this,
            REQUEST_CONFIRM_CLEANUP,
            title = resources.getQuantityString(R.plurals.cleanup_confirmation_title, selected, selected),
            message = getString(R.string.cleanup_confirmation_message),
            positiveButton = R.string.core_action_delete,
            negativeButton = R.string.core_cancel
        )
    }

    private fun setFabVisibility(visible: Boolean) {
        val fab = binding!!.deleteTracksButton
        if (visible) {
            fab.show()
        } else {
            fab.hide()
        }
    }

    /**
     * Provides the detail of items in the selectable list from their ViewHolder.
     */
    private class TrackDetailLookup(
        private val view: RecyclerView
    ) : ItemDetailsLookup<Long>() {

        override fun getItemDetails(e: MotionEvent): ItemDetails<Long>? {
            return view.findChildViewUnder(e.x, e.y)
                ?.let { view.getChildViewHolder(it) as? CleanupAdapter.ViewHolder }
                ?.itemDetails
        }
    }

    private inner class HasSelectionObserver(
        private val liveSelection: Selection<Long>
    ) : SelectionTracker.SelectionObserver<Long>(),
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

        override fun onItemStateChanged(key: Long, selected: Boolean) {
            if (!liveSelection.isEmpty) {
                updateActionModeText()
            }
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
                //mode.subtitle = formatToHumanReadableByteCount(freedBytes)
            }
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false

        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean = true

        override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean = false

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            selectionTracker.clearSelection()
        }
    }
}