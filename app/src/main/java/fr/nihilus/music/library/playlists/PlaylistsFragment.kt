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
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.Hold
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.startPostponedEnterTransitionWhenDrawn
import fr.nihilus.music.databinding.FragmentPlaylistBinding
import fr.nihilus.music.library.HomeFragmentDirections
import fr.nihilus.music.library.HomeViewModel

class PlaylistsFragment : BaseFragment(R.layout.fragment_playlist) {

    private val viewModel: HomeViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentPlaylistBinding.bind(view)

        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            binding.progressIndicator.isVisible = shouldShow
        }

        lateinit var adapter: PlaylistsAdapter
        val onPlaylistSelected = { position: Int ->
            val selectedPlaylist = adapter.getItem(position)
            val playlistId = selectedPlaylist.mediaId!!
            val toPlaylistTracks = HomeFragmentDirections.browsePlaylistContent(playlistId)

            val playlistHolder = binding.playlistList.findViewHolderForAdapterPosition(position)!!
            val transitionExtras = FragmentNavigatorExtras(
                playlistHolder.itemView to playlistId
            )

            requireParentFragment().apply {
                exitTransition = Hold().apply {
                    duration = resources.getInteger(R.integer.ui_motion_duration_large).toLong()
                    addTarget(R.id.fragment_home)
                }
                reenterTransition = null
            }

            findNavController().navigate(toPlaylistTracks, transitionExtras)
        }

        adapter = PlaylistsAdapter(this, onPlaylistSelected)
        binding.playlistList.adapter = adapter
        binding.playlistList.setHasFixedSize(true)

        viewModel.playlists.observe(viewLifecycleOwner) { playlistsRequest ->
            when (playlistsRequest) {
                is LoadRequest.Pending -> progressBarLatch.isRefreshing = true
                is LoadRequest.Success -> {
                    progressBarLatch.isRefreshing = false
                    adapter.submitList(playlistsRequest.data)
                    binding.emptyViewGroup.isVisible = playlistsRequest.data.isEmpty()
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

}
