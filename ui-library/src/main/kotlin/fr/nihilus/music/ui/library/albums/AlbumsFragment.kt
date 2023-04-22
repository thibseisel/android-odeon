/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.ui.library.albums

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.Hold
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.startPostponedEnterTransitionWhenDrawn
import fr.nihilus.music.core.ui.observe
import fr.nihilus.music.ui.library.HomeFragmentDirections
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.ui.library.databinding.FragmentAlbumsBinding
import fr.nihilus.music.core.ui.R as CoreUiR

/**
 * Display all albums in a grid of images.
 * Selecting an album opens its [detail view][AlbumDetailFragment].
 */
@AndroidEntryPoint
internal class AlbumsFragment : BaseFragment(R.layout.fragment_albums) {
    private val viewModel: AlbumGridViewModel by viewModels()

    private var binding: FragmentAlbumsBinding? = null
    private lateinit var albumAdapter: AlbumsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAlbumsBinding.bind(view)
        this.binding = binding

        val refreshToggle = ProgressTimeLatch { progressVisible ->
            binding.progressIndicator.isVisible = progressVisible
        }

        albumAdapter = AlbumsAdapter(this, ::browseAlbumAt)
        binding.albumGrid.apply {
            adapter = albumAdapter
            setHasFixedSize(true)
        }

        viewModel.state.observe(viewLifecycleOwner) {
            refreshToggle.isRefreshing = it.isLoadingAlbums && it.albums.isEmpty()
            albumAdapter.submitList(it.albums)
            binding.emptyViewGroup.isVisible = !it.isLoadingAlbums && it.albums.isEmpty()
            if (!it.isLoadingAlbums) {
                requireParentFragment().startPostponedEnterTransitionWhenDrawn()
            }
        }
    }

    private fun browseAlbumAt(position: Int) {
        val album = albumAdapter.getItem(position)
        val holder =
            binding!!.albumGrid.findViewHolderForAdapterPosition(position) as AlbumsAdapter.ViewHolder

        val toAlbumDetail = HomeFragmentDirections.browseAlbumDetail(album.id.encoded)
        val transitionExtras = FragmentNavigatorExtras(
            holder.itemView to album.id.encoded
        )

        // Reset transitions previously set when moving to search fragment,
        // and keep it displayed while animating to the next destination.
        requireParentFragment().apply {
            exitTransition = Hold().apply {
                duration = resources
                    .getInteger(CoreUiR.integer.ui_motion_duration_large)
                    .toLong()
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
