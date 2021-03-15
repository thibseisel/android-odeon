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

package fr.nihilus.music.library.albums

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.Hold
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.startPostponedEnterTransitionWhenDrawn
import fr.nihilus.music.databinding.FragmentAlbumsBinding
import fr.nihilus.music.library.HomeFragmentDirections
import fr.nihilus.music.library.HomeViewModel

/**
 * Display all albums in a grid of images.
 * Selecting an album opens its [detail view][AlbumDetailFragment].
 */
@AndroidEntryPoint
class AlbumsFragment : BaseFragment(R.layout.fragment_albums) {
    private val viewModel: HomeViewModel by activityViewModels()

    private var binding: FragmentAlbumsBinding? = null
    private lateinit var albumAdapter: AlbumsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAlbumsBinding.bind(view)
        this.binding = binding

        val refreshToggle = ProgressTimeLatch { progressVisible ->
            binding.progressIndicator.isVisible = progressVisible
        }

        albumAdapter = AlbumsAdapter(this, ::onAlbumSelected)
        binding.albumGrid.apply {
            adapter = albumAdapter
            setHasFixedSize(true)
        }

        viewModel.albums.observe(viewLifecycleOwner) { albumRequest ->
            when (albumRequest) {
                is LoadRequest.Pending -> refreshToggle.isRefreshing = true
                is LoadRequest.Success -> {
                    refreshToggle.isRefreshing = false
                    albumAdapter.submitList(albumRequest.data)
                    binding.emptyViewGroup.isVisible = albumRequest.data.isEmpty()
                    requireParentFragment().startPostponedEnterTransitionWhenDrawn()
                }
                is LoadRequest.Error -> {
                    refreshToggle.isRefreshing = false
                    albumAdapter.submitList(emptyList())
                    binding.emptyViewGroup.isVisible = true
                    requireParentFragment().startPostponedEnterTransitionWhenDrawn()
                }
            }
        }
    }

    private fun onAlbumSelected(position: Int) {
        val album = albumAdapter.getItem(position)
        val holder = binding!!.albumGrid.findViewHolderForAdapterPosition(position) as AlbumHolder

        val albumId = album.mediaId!!
        val toAlbumDetail = HomeFragmentDirections.browseAlbumDetail(albumId)
        val transitionExtras = FragmentNavigatorExtras(
            holder.itemView to albumId
        )

        // Reset transitions previously set when moving to search fragment,
        // and keep it displayed while animating to the next destination.
        requireParentFragment().apply {
            exitTransition = Hold().apply {
                duration = resources.getInteger(R.integer.ui_motion_duration_large).toLong()
                addTarget(R.id.fragment_home)
            }
            reenterTransition = null
        }

        findNavController().navigate(toAlbumDetail, transitionExtras)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
