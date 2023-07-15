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

package fr.nihilus.music.ui.library.artists.detail

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.startPostponedEnterTransitionWhenDrawn
import fr.nihilus.music.core.ui.observe
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.ui.library.databinding.FragmentArtistDetailBinding
import java.util.concurrent.TimeUnit
import fr.nihilus.music.core.ui.R as CoreUiR

@AndroidEntryPoint
internal class ArtistDetailFragment : BaseFragment(R.layout.fragment_artist_detail) {
    private val viewModel: ArtistDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentArtistDetailBinding.bind(view)

        setupGridToDetailTransition()

        // Wait for albums to be displayed before returning from album detail screen.
        postponeEnterTransition(1000, TimeUnit.MILLISECONDS)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        val albumAdapter = ArtistAlbumsAdapter(this, ::navigateToAlbumDetail)
        val tracksAdapter = ArtistTracksAdapter(this) { track ->
            viewModel.play(track)
        }

        val childrenAdapter = ConcatAdapter(
            ConcatAdapter.Config.Builder()
                .setIsolateViewTypes(false)
                .setStableIdMode(ConcatAdapter.Config.StableIdMode.ISOLATED_STABLE_IDS)
                .build(),
            albumAdapter,
            tracksAdapter
        )
        binding.artistChildren.apply {
            adapter = childrenAdapter
            layoutManager = createGridLayoutManager(childrenAdapter)
            setHasFixedSize(true)
            itemAnimator = object : DefaultItemAnimator() {
                override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
                    dispatchAddFinished(holder)
                    dispatchAddStarting(holder)
                    return false
                }
            }
        }

        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            binding.progressIndicator.isVisible = shouldShow
        }

        viewModel.state.observe(viewLifecycleOwner) {
            binding.toolbar.title = it.name
            progressBarLatch.isRefreshing = it.isLoading
            albumAdapter.submitList(it.albums)
            tracksAdapter.submitList(it.tracks)

            if (!it.isLoading && it.albums.isNotEmpty()) {
                startPostponedEnterTransitionWhenDrawn()
            }
        }
    }

    private fun createGridLayoutManager(
        adapter: RecyclerView.Adapter<*>
    ): RecyclerView.LayoutManager = GridLayoutManager(
        context,
        resources.getInteger(R.integer.artist_grid_span_count)
    ).apply {
        spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val viewType = adapter.getItemViewType(position)
                return if (viewType == R.id.view_type_album) 1 else spanCount
            }
        }
    }

    private fun setupGridToDetailTransition() {
        val sharedAxisDuration =
            resources.getInteger(CoreUiR.integer.ui_motion_duration_large).toLong()
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = sharedAxisDuration
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = sharedAxisDuration
        }
    }

    private fun navigateToAlbumDetail(
        album: ArtistAlbumUiState,
        holder: ArtistAlbumsAdapter.ViewHolder
    ) {
        val albumId = album.id.encoded
        val toAlbumDetail = ArtistDetailFragmentDirections.browseArtistAlbum(albumId)
        val transitionExtras = FragmentNavigatorExtras(
            holder.itemView to albumId
        )

        // Keep this fragment displayed while animating to the next destination.
        exitTransition = Hold().apply {
            duration = resources.getInteger(CoreUiR.integer.ui_motion_duration_large).toLong()
            addTarget(R.id.fragment_artist_detail)
        }

        findNavController().navigate(toAlbumDetail, transitionExtras)
    }
}
