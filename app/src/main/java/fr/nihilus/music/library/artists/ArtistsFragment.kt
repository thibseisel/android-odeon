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
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.startPostponedEnterTransitionWhenDrawn
import fr.nihilus.music.databinding.FragmentArtistsBinding
import fr.nihilus.music.library.HomeFragmentDirections
import fr.nihilus.music.library.HomeViewModel
import fr.nihilus.music.library.artists.detail.ArtistAdapter

@AndroidEntryPoint
class ArtistsFragment : BaseFragment(R.layout.fragment_artists) {
    private val viewModel: HomeViewModel by activityViewModels()

    private lateinit var adapter: ArtistAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = ArtistAdapter(this, ::onArtistSelected)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentArtistsBinding.bind(view)

        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            binding.progressIndicator.isVisible = shouldShow
        }

        binding.artistGrid.adapter = adapter
        binding.artistGrid.setHasFixedSize(true)

        viewModel.artists.observe(viewLifecycleOwner) { artistRequest ->
            when (artistRequest) {
                is LoadRequest.Pending -> progressBarLatch.isRefreshing = true
                is LoadRequest.Success -> {
                    progressBarLatch.isRefreshing = false
                    adapter.submitList(artistRequest.data)
                    binding.emptyViewGroup.isVisible = artistRequest.data.isEmpty()
                    requireParentFragment().startPostponedEnterTransitionWhenDrawn()
                }
                is LoadRequest.Error -> {
                    progressBarLatch.isRefreshing = false
                    adapter.submitList(emptyList())
                    binding.emptyViewGroup.isVisible = true
                    requireParentFragment().startPostponedEnterTransitionWhenDrawn()
                }
            }
        }
    }

    private fun onArtistSelected(position: Int) {
        val artist = adapter.getItem(position)

        // Reset transitions set by another navigation events.
        val transitionDuration = resources.getInteger(fr.nihilus.music.core.ui.R.integer.ui_motion_duration_large).toLong()
        requireParentFragment().apply {
            exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
                duration = transitionDuration
            }
            reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
                duration = transitionDuration
            }
        }

        val toArtistDetail = HomeFragmentDirections.browseArtistDetail(artist.mediaId!!)
        findNavController().navigate(toArtistDetail)
    }
}
