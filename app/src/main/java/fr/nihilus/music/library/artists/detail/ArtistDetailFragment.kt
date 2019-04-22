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

import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.ViewModelProviders
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseFragment
import fr.nihilus.music.extensions.isVisible
import fr.nihilus.music.extensions.observeK
import fr.nihilus.music.library.FRAGMENT_ID
import fr.nihilus.music.library.MusicLibraryViewModel
import fr.nihilus.music.library.albums.AlbumDetailActivity
import fr.nihilus.music.library.albums.AlbumHolder
import fr.nihilus.music.library.albums.AlbumPalette
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.LoadRequest
import fr.nihilus.music.ui.ProgressTimeLatch
import kotlinx.android.synthetic.main.fragment_artist_detail.*
import javax.inject.Inject

class ArtistDetailFragment : BaseFragment(), BaseAdapter.OnItemSelectedListener {

    @Inject lateinit var defaultAlbumPalette: AlbumPalette

    private lateinit var adapter: ArtistDetailAdapter

    private val pickedArtist: MediaItem by lazy(LazyThreadSafetyMode.NONE) {
        arguments?.getParcelable<MediaItem>(KEY_ARTIST) ?: error("Callers must specify the artist to display.")
    }

    private val hostViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(requireActivity())[MusicLibraryViewModel::class.java]
    }

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this, viewModelFactory)[ArtistDetailViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = ArtistDetailAdapter(this, defaultAlbumPalette, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_artist_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadChildrenOfArtist(pickedArtist)

        val spanCount = resources.getInteger(R.integer.artist_grid_span_count)
        val manager = androidx.recyclerview.widget.GridLayoutManager(context, spanCount)
        artist_detail_recycler.adapter = adapter
        artist_detail_recycler.layoutManager = manager
        artist_detail_recycler.setHasFixedSize(true)

        manager.spanSizeLookup = object : androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                val viewType = adapter.getItemViewType(position)
                return if (viewType == R.id.view_type_album) 1 else spanCount
            }
        }

        val progressIndicator = view.findViewById<View>(R.id.progress_indicator)
        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            progressIndicator.isVisible = shouldShow
        }

        viewModel.children.observeK(this) { childrenRequest ->
            when  (childrenRequest) {
                is LoadRequest.Pending -> progressBarLatch.isRefreshing = true
                is LoadRequest.Success -> {
                    progressBarLatch.isRefreshing = false
                    adapter.submitList(childrenRequest.data)
                }
                is LoadRequest.Error -> {
                    progressBarLatch.isRefreshing = false
                    adapter.submitList(emptyList())
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        hostViewModel.setToolbarTitle(pickedArtist.description.title)
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val selectedItem = adapter.getItem(position)
        val holder = artist_detail_recycler.findViewHolderForAdapterPosition(position) ?: return
        when (holder.itemViewType) {
            R.id.view_type_track -> onTrackSelected(selectedItem)
            R.id.view_type_album -> onAlbumSelected(holder as AlbumHolder, selectedItem)
        }
    }

    private fun onAlbumSelected(holder: AlbumHolder, album: MediaItem) {
        val options = ActivityOptionsCompat.makeSceneTransitionAnimation(
            activity!!, holder.transitionView, AlbumDetailActivity.ALBUM_ART_TRANSITION_NAME
        )

        val albumDetailIntent = Intent(context, AlbumDetailActivity::class.java).apply {
            putExtra(AlbumDetailActivity.ARG_PICKED_ALBUM, album)
            putExtra(AlbumDetailActivity.ARG_PALETTE, holder.colorPalette)
        }

        startActivity(albumDetailIntent, options.toBundle())
    }

    private fun onTrackSelected(track: MediaItem) {
        hostViewModel.playMedia(track)
    }

    companion object Factory {

        private const val KEY_ARTIST = "artist"

        fun newInstance(artist: MediaItem) = ArtistDetailFragment().apply {
            arguments = Bundle(2).apply {
                putInt(FRAGMENT_ID, R.id.action_artists)
                putParcelable(KEY_ARTIST, artist)
            }
        }
    }
}
