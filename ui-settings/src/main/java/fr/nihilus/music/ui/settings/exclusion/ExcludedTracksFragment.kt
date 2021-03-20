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

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.view.DividerItemDecoration
import fr.nihilus.music.ui.settings.R
import fr.nihilus.music.ui.settings.databinding.ExcludedTracksFragmentBinding

/**
 * Display the list of tracks that have been excluded from the music library by users.
 * Tracks listed here can be allowed again by swiping them.
 */
@AndroidEntryPoint
internal class ExcludedTracksFragment : BaseFragment(R.layout.excluded_tracks_fragment) {

    private val viewModel by viewModels<ExcludedTracksViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = ExcludedTracksFragmentBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        val adapter = ExclusionAdapter()
        binding.trackList.adapter = adapter
        binding.trackList.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )

        ItemTouchHelper(SlideCallback { swipedTrackPosition ->
            val swipedTrack = adapter.getItem(swipedTrackPosition)
            viewModel.restore(swipedTrack)
        }).attachToRecyclerView(binding.trackList)

        viewModel.tracks.observe(viewLifecycleOwner) { trackList ->
            adapter.submitList(trackList)
        }
    }

    private class SlideCallback(
        private val onSwiped: (swipedPosition: Int) -> Unit
    ) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START or ItemTouchHelper.END) {

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = false

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            onSwiped(viewHolder.adapterPosition)
        }
    }
}
