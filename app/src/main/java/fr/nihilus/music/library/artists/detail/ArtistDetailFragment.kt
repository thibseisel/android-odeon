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

package fr.nihilus.music.library.artists.detail

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.afterMeasure
import fr.nihilus.music.databinding.FragmentAlbumDetailBinding
import fr.nihilus.music.databinding.FragmentArtistDetailBinding
import fr.nihilus.music.library.MusicLibraryViewModel
import fr.nihilus.music.library.albums.AlbumHolder

class ArtistDetailFragment : BaseFragment(R.layout.fragment_artist_detail) {

    private lateinit var childrenAdapter: ArtistDetailAdapter
    private val args: ArtistDetailFragmentArgs by navArgs()

    private val hostViewModel: MusicLibraryViewModel by activityViewModels()
    private val viewModel: ArtistDetailViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setArtist(args.artistId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentArtistDetailBinding.bind(view)

        postponeEnterTransition()

        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }

        childrenAdapter = ArtistDetailAdapter(this, object : ArtistDetailAdapter.SelectionListener {

            override fun onAlbumSelected(position: Int) {
                val holder = binding.artistDetailRecycler.findViewHolderForAdapterPosition(position) as? AlbumHolder ?: return
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

        binding.artistDetailRecycler.apply {
            adapter = childrenAdapter
            layoutManager = manager
            setHasFixedSize(true)
            afterMeasure { startPostponedEnterTransition() }
            itemAnimator = object : DefaultItemAnimator() {
                override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
                    dispatchAddFinished(holder)
                    dispatchAddStarting(holder)
                    return false
                }
            }
        }

        val progressIndicator = view.findViewById<View>(R.id.progress_indicator)
        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            progressIndicator.isVisible = shouldShow
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
                }
                is LoadRequest.Error -> {
                    progressBarLatch.isRefreshing = false
                    this.childrenAdapter.submitList(emptyList())
                }
            }
        }
    }

    private fun onAlbumSelected(holder: AlbumHolder, album: MediaItem) {
        val albumId = album.mediaId!!
        val toAlbumDetail = ArtistDetailFragmentDirections.browseArtistAlbum(albumId)
        val transitionExtras = FragmentNavigatorExtras(
            holder.transitionView to albumId
        )

        findNavController().navigate(toAlbumDetail, transitionExtras)
    }

    private fun onTrackSelected(track: MediaItem) {
        hostViewModel.playMedia(track)
    }
}
