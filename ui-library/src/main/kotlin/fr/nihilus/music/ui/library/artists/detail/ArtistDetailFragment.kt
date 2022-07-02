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
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.startPostponedEnterTransitionWhenDrawn
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.ui.library.albums.AlbumHolder
import fr.nihilus.music.ui.library.databinding.FragmentArtistDetailBinding
import java.util.concurrent.TimeUnit
import fr.nihilus.music.core.ui.R as CoreUiR

@AndroidEntryPoint
internal class ArtistDetailFragment : BaseFragment(R.layout.fragment_artist_detail) {
    private val viewModel: ArtistDetailViewModel by viewModels()
    private lateinit var childrenAdapter: ArtistDetailAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentArtistDetailBinding.bind(view)

        setupGridToDetailTransition()

        // Wait for albums to be displayed before returning from album detail screen.
        postponeEnterTransition(1000, TimeUnit.MILLISECONDS)

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        childrenAdapter = ArtistDetailAdapter(this, object : ArtistDetailAdapter.SelectionListener {

            override fun onAlbumSelected(position: Int) {
                val holder =
                    binding.artistChildren.findViewHolderForAdapterPosition(position) as? AlbumHolder
                        ?: return
                val album = childrenAdapter.getItem(position)
                onAlbumSelected(holder, album)
            }

            override fun onTrackSelected(position: Int) {
                val track = childrenAdapter.getItem(position)
                onTrackSelected(track)
            }
        })

        val spanCount = resources.getInteger(R.integer.artist_grid_span_count)
        val manager = GridLayoutManager(context, spanCount)
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val viewType = childrenAdapter.getItemViewType(position)
                return if (viewType == R.id.view_type_album) 1 else spanCount
            }
        }

        binding.artistChildren.apply {
            adapter = childrenAdapter
            layoutManager = manager
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

        viewModel.artist.observe(viewLifecycleOwner) {
            binding.toolbar.title = it.description.title
        }

        viewModel.children.observe(viewLifecycleOwner) { childrenRequest ->
            when (childrenRequest) {
                is LoadRequest.Pending -> progressBarLatch.isRefreshing = true
                is LoadRequest.Success -> {
                    progressBarLatch.isRefreshing = false
                    this.childrenAdapter.submitList(childrenRequest.data)
                    startPostponedEnterTransitionWhenDrawn()
                }
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

    private fun onAlbumSelected(holder: AlbumHolder, album: MediaItem) {
        val albumId = album.mediaId!!
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

    private fun onTrackSelected(track: MediaItem) {
        viewModel.playMedia(track)
    }
}
