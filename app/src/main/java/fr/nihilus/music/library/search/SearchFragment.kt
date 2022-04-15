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

package fr.nihilus.music.library.search

import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.R
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.parse
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.startPostponedEnterTransitionWhenDrawn
import fr.nihilus.music.databinding.FragmentSearchBinding
import fr.nihilus.music.library.playlists.AddToPlaylistDialog
import fr.nihilus.music.library.songs.DeleteTrackDialog
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class SearchFragment : BaseFragment(R.layout.fragment_search) {
    private val viewModel by viewModels<SearchViewModel>()
    private val keyboard by lazy(LazyThreadSafetyMode.NONE) {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    private val backToTopObserver = BackToTopObserver()
    private var binding: FragmentSearchBinding? = null
    private lateinit var resultsAdapter: SearchResultsAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSearchBinding.bind(view)
        this.binding = binding

        postponeEnterTransition(1000, TimeUnit.MILLISECONDS)
        setupHomeToSearchTransition()

        val recyclerView = binding.searchResultGrid
        recyclerView.setHasFixedSize(true)

        resultsAdapter = SearchResultsAdapter(this) { item, adapterPosition, action ->
            val holder = checkNotNull(recyclerView.findViewHolderForAdapterPosition(adapterPosition))
            onSuggestionSelected(item, holder, action)
        }
        recyclerView.adapter = resultsAdapter

        val gridSpanCount = minOf(
            resources.getInteger(R.integer.album_grid_span_count),
            resources.getInteger(R.integer.artist_grid_span_count)
        )
        recyclerView.layoutManager = GridLayoutManager(requireContext(), gridSpanCount).apply {
            spanSizeLookup = SearchSpanSizer(gridSpanCount, resultsAdapter)
            isUsingSpansToEstimateScrollbarDimensions = true
        }

        with(binding.searchToolbar) {
            setNavigationOnClickListener { onNavigateUp() }
            setOnMenuItemClickListener(::onOptionsItemSelected)
        }

        binding.searchInput.doAfterTextChanged { text ->
            viewModel.search(text?.toString() ?: "")
        }

        binding.searchInput.setOnEditorActionListener { v, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    viewModel.search(v.text?.toString() ?: "")
                    true
                }

                else -> false
            }
        }

        viewModel.searchResults.observe(viewLifecycleOwner) { searchResults ->
            resultsAdapter.submitList(searchResults)
            startPostponedEnterTransitionWhenDrawn()
        }

        if (savedInstanceState == null) {
            showKeyboard(binding.searchInput)
        }
    }

    private fun setupHomeToSearchTransition() {
        val transitionDuration = resources.getInteger(R.integer.ui_motion_duration_large).toLong()
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = transitionDuration
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = transitionDuration
        }
    }

    private fun setupHoldTransition() {
        val transitionDuration = resources.getInteger(R.integer.ui_motion_duration_large).toLong()
        exitTransition = Hold().apply {
            duration = transitionDuration
            addTarget(requireView())
        }
        reenterTransition = null
    }

    private fun setupSharedAxisTransition() {
        val sharedAxisDuration = resources.getInteger(R.integer.ui_motion_duration_large).toLong()
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = sharedAxisDuration
        }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = sharedAxisDuration
        }
    }

    override fun onStart() {
        super.onStart()
        resultsAdapter.registerAdapterDataObserver(backToTopObserver)
    }

    override fun onStop() {
        resultsAdapter.unregisterAdapterDataObserver(backToTopObserver)
        super.onStop()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_clear -> {
            val input = binding!!.searchInput
            input.text = null
            input.requestFocus()
            resultsAdapter.submitList(emptyList())
            true
        }

        else -> false
    }

    private fun onNavigateUp() {
        hideKeyboard(binding!!.searchInput)
        setupHomeToSearchTransition()
        findNavController().navigateUp()
    }

    private fun showKeyboard(view: View) {
        view.requestFocus()
        keyboard.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideKeyboard(view: View) {
        keyboard.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun onSuggestionSelected(
        item: MediaBrowserCompat.MediaItem,
        holder: RecyclerView.ViewHolder,
        action: SearchResultsAdapter.ItemAction
    ) {
        when (action) {
            SearchResultsAdapter.ItemAction.PRIMARY -> when {
                item.isBrowsable -> browseMedia(item, holder)
                item.isPlayable -> {
                    viewModel.play(item)
                    onNavigateUp()
                }
            }

            SearchResultsAdapter.ItemAction.ADD_TO_PLAYLIST -> {
                AddToPlaylistDialog.open(this, listOf(item))
            }

            SearchResultsAdapter.ItemAction.EXCLUDE -> {
                viewModel.exclude(item)
            }

            SearchResultsAdapter.ItemAction.DELETE -> {
                DeleteTrackDialog.open(this, item)
            }
        }
    }

    private fun browseMedia(item: MediaBrowserCompat.MediaItem, holder: RecyclerView.ViewHolder) {
        hideKeyboard(binding!!.searchInput)
        val navController = findNavController()
        val (type, _, _) = item.mediaId.parse()

        when (type) {
            MediaId.TYPE_ALBUMS -> {
                val albumId = item.mediaId!!
                val toAlbumDetail = SearchFragmentDirections.browseAlbumDetail(albumId)

                setupHoldTransition()

                val transitionExtras = FragmentNavigatorExtras(holder.itemView to albumId)
                navController.navigate(toAlbumDetail, transitionExtras)
            }

            MediaId.TYPE_ARTISTS -> {
                val artistId = item.mediaId!!
                val toArtistDetail = SearchFragmentDirections.browseArtistDetail(artistId)
                setupSharedAxisTransition()
                navController.navigate(toArtistDetail)
            }

            MediaId.TYPE_PLAYLISTS -> {
                val playlistId = item.mediaId!!
                val toPlaylistContent = SearchFragmentDirections.browsePlaylistContent(playlistId)

                setupHoldTransition()

                val transitionExtras = FragmentNavigatorExtras(holder.itemView to playlistId)
                navController.navigate(toPlaylistContent, transitionExtras)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private inner class BackToTopObserver : RecyclerView.AdapterDataObserver() {

        override fun onItemRangeChanged(positionStart: Int, itemCount: Int) = onChanged()
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) = onChanged()
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = onChanged()
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) = onChanged()
        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) = onChanged()

        override fun onChanged() {
            binding?.searchResultGrid?.scrollToPosition(0)
        }
    }

    /**
     * Determines how many grid spans each item should take.
     */
    private class SearchSpanSizer(
        private val spanCount: Int,
        private val adapter: SearchResultsAdapter
    ) : GridLayoutManager.SpanSizeLookup() {

        init {
            isSpanIndexCacheEnabled = true
            isSpanGroupIndexCacheEnabled = true
        }

        override fun getSpanSize(position: Int): Int {
            return when (val viewType = adapter.getItemViewType(position)) {
                R.id.view_type_album,
                R.id.view_type_artist -> 1
                R.id.view_type_track,
                R.id.view_type_playlist,
                R.id.view_type_header -> spanCount
                else -> error("Unexpected view type for position $position: $viewType")
            }
        }
    }
}