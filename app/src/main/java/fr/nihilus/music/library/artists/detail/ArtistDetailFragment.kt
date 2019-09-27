/*
 * Copyright 2019 Thibault Seisel
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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.afterMeasure
import fr.nihilus.music.core.ui.extensions.isVisible
import fr.nihilus.music.library.MusicLibraryViewModel
import fr.nihilus.music.library.albums.AlbumHolder
import fr.nihilus.music.ui.BaseAdapter
import kotlinx.android.synthetic.main.fragment_artist_detail.*

class ArtistDetailFragment : BaseFragment(R.layout.fragment_artist_detail), BaseAdapter.OnItemSelectedListener {
    private lateinit var childrenAdapter: ArtistDetailAdapter

    private val args: ArtistDetailFragmentArgs by navArgs()

    private val hostViewModel: MusicLibraryViewModel by activityViewModels()
    private val viewModel: ArtistDetailViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        childrenAdapter = ArtistDetailAdapter(this, this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        postponeEnterTransition()

        with (toolbar) {
            title = args.pickedArtist.description.title
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        viewModel.loadChildrenOfArtist(args.pickedArtist)

        val spanCount = resources.getInteger(R.integer.artist_grid_span_count)
        val manager = GridLayoutManager(context, spanCount)
        manager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val viewType = childrenAdapter.getItemViewType(position)
                return if (viewType == R.id.view_type_album) 1 else spanCount
            }
        }

        with(artist_detail_recycler) {
            adapter = childrenAdapter
            layoutManager = manager
            setHasFixedSize(true)
            afterMeasure { startPostponedEnterTransition() }
        }

        val progressIndicator = view.findViewById<View>(R.id.progress_indicator)
        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            progressIndicator.isVisible = shouldShow
        }

        viewModel.children.observe(this) { childrenRequest ->
            when  (childrenRequest) {
                is LoadRequest.Pending -> progressBarLatch.isRefreshing = true
                is LoadRequest.Success -> {
                    progressBarLatch.isRefreshing = false
                    childrenAdapter.submitList(childrenRequest.data)
                }
                is LoadRequest.Error -> {
                    progressBarLatch.isRefreshing = false
                    childrenAdapter.submitList(emptyList())
                }
            }
        }
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val selectedItem = childrenAdapter.getItem(position)
        val holder = artist_detail_recycler.findViewHolderForAdapterPosition(position) ?: return
        when (holder.itemViewType) {
            R.id.view_type_track -> onTrackSelected(selectedItem)
            R.id.view_type_album -> onAlbumSelected(holder as AlbumHolder, selectedItem)
        }
    }

    private fun onAlbumSelected(holder: AlbumHolder, album: MediaItem) {
        val toAlbumDetail =
            ArtistDetailFragmentDirections.browseArtistAlbum(album, holder.colorPalette)
        val transitionExtras = FragmentNavigatorExtras(
            holder.transitionView to album.mediaId!!
        )

        findNavController().navigate(toAlbumDetail, transitionExtras)
    }

    private fun onTrackSelected(track: MediaItem) {
        hostViewModel.playMedia(track)
    }
}
