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

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.view.MotionEvent
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.recyclerview.selection.ItemDetailsLookup
import androidx.recyclerview.selection.ItemKeyProvider
import androidx.recyclerview.selection.SelectionTracker
import androidx.recyclerview.selection.StorageStrategy
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseFragment
import fr.nihilus.music.ui.LoadRequest
import kotlinx.android.synthetic.main.fragment_cleanup.*

/**
 * Lists tracks that could be deleted from the device's storage to free-up space.
 */
class CleanupFragment : BaseFragment(R.layout.fragment_cleanup) {

    private val viewModel by viewModels<CleanupViewModel> { viewModelFactory }
    private lateinit var selectionTracker: SelectionTracker<MediaBrowserCompat.MediaItem>

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = CleanupAdapter()

        val recyclerView = disposable_track_list
        recyclerView.adapter = adapter
        recyclerView.setHasFixedSize(true)

        selectionTracker = SelectionTracker.Builder(
            "disposable_tracks_selection",
            recyclerView,
            TrackKeyProvider(adapter),
            TrackDetailLookup(recyclerView),
            StorageStrategy.createParcelableStorage(MediaBrowserCompat.MediaItem::class.java)
        ).build()

        adapter.selection = selectionTracker

        action_delete_selected.setOnClickListener {
            val selectedTracks = selectionTracker.selection.toList()
            viewModel.deleteTracks(selectedTracks)
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
}