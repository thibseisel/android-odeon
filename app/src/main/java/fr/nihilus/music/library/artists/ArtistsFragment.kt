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

package fr.nihilus.music.library.artists

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.startPostponedEnterTransitionWhenDrawn
import fr.nihilus.music.core.ui.observe
import fr.nihilus.music.databinding.FragmentArtistsBinding
import fr.nihilus.music.library.HomeFragmentDirections
import fr.nihilus.music.library.artists.detail.ArtistDetailFragment
import fr.nihilus.music.core.ui.R as CoreUiR

/**
 * Displays all artists in a grid of images.
 * Selecting an artist opens [its detail view][ArtistDetailFragment].
 */
@AndroidEntryPoint
class ArtistsFragment : BaseFragment(R.layout.fragment_artists) {
    private val viewModel: ArtistsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentArtistsBinding.bind(view)

        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            binding.progressIndicator.isVisible = shouldShow
        }

        val adapter = ArtistAdapter(this, ::browseArtist)
        binding.artistGrid.adapter = adapter
        binding.artistGrid.setHasFixedSize(true)

        viewModel.state.observe(viewLifecycleOwner) {
            progressBarLatch.isRefreshing = it.isLoadingArtists && it.artists.isEmpty()
            adapter.submitList(it.artists)
            binding.emptyViewGroup.isVisible = !it.isLoadingArtists && it.artists.isEmpty()
            if (it.isLoadingArtists) {
                requireParentFragment().startPostponedEnterTransitionWhenDrawn()
            }
        }
    }

    private fun browseArtist(artist: ArtistUiState) {
        // Reset transitions set by another navigation events.
        val transitionDuration = resources
            .getInteger(CoreUiR.integer.ui_motion_duration_large)
            .toLong()
        requireParentFragment().apply {
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
                duration = transitionDuration
            }
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
                duration = transitionDuration
            }
        }

        val toArtistDetail = HomeFragmentDirections.browseArtistDetail(artist.id.encoded)
        findNavController().navigate(toArtistDetail)
    }
}
