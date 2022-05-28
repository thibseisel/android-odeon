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

package fr.nihilus.music.library.playlists

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.Hold
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.startPostponedEnterTransitionWhenDrawn
import fr.nihilus.music.core.ui.observe
import fr.nihilus.music.databinding.FragmentPlaylistBinding
import fr.nihilus.music.library.HomeFragmentDirections
import fr.nihilus.music.core.ui.R as CoreUiR

@AndroidEntryPoint
class PlaylistsFragment : BaseFragment(R.layout.fragment_playlist) {
    private val viewModel: PlaylistsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentPlaylistBinding.bind(view)

        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            binding.progressIndicator.isVisible = shouldShow
        }

        val adapter = PlaylistsAdapter(this, ::browsePlaylist)
        binding.playlistList.adapter = adapter
        binding.playlistList.setHasFixedSize(true)

        viewModel.state.observe(viewLifecycleOwner) {
            progressBarLatch.isRefreshing = it.isLoadingPlaylists && it.playlists.isEmpty()
            adapter.submitList(it.playlists)
            binding.emptyViewGroup.isVisible = !it.isLoadingPlaylists && it.playlists.isEmpty()
            if (it.isLoadingPlaylists) {
                requireParentFragment().startPostponedEnterTransitionWhenDrawn()
            }
        }
    }

    private fun browsePlaylist(playlist: PlaylistUiState, holder: PlaylistsAdapter.ViewHolder) {
        requireParentFragment().apply {
            exitTransition = Hold().apply {
                duration = resources.getInteger(CoreUiR.integer.ui_motion_duration_large).toLong()
                addTarget(R.id.fragment_home)
            }
            reenterTransition = null
        }

        findNavController().navigate(
            HomeFragmentDirections.browsePlaylistContent(playlist.id.encoded),
            FragmentNavigatorExtras(holder.itemView to playlist.id.encoded)
        )
    }

}
