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

package fr.nihilus.music.ui.library.search

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.transition.Hold
import com.google.android.material.transition.MaterialSharedAxis
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.startPostponedEnterTransitionWhenDrawn
import fr.nihilus.music.core.ui.observe
import fr.nihilus.music.media.tracks.DeleteTracksResult
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.ui.library.databinding.FragmentSearchBinding
import fr.nihilus.music.ui.library.playlists.AddToPlaylistDialog
import fr.nihilus.music.ui.library.tracks.DeleteTrackDialog
import java.util.concurrent.TimeUnit
import fr.nihilus.music.core.ui.R as CoreUiR

@AndroidEntryPoint
internal class SearchFragment : BaseFragment(R.layout.fragment_search) {
    private val viewModel by viewModels<SearchViewModel>()
    private val keyboard by lazy(LazyThreadSafetyMode.NONE) {
        requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    }

    private val backToTopObserver = BackToTopObserver()
    private var binding: FragmentSearchBinding? = null
    private lateinit var resultsAdapter: SearchResultsAdapter

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val confirmation = viewModel.deleteEvent.value?.data
        if (granted && confirmation != null) {
            viewModel.delete(confirmation.trackId)
        }
    }

    private val deleteMediaPopup = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            showDeleteTrackConfirmation()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentSearchBinding.bind(view)
        this.binding = binding

        DeleteTrackDialog.registerForResult(this) { trackId ->
            viewModel.delete(trackId)
        }

        postponeEnterTransition(1000, TimeUnit.MILLISECONDS)
        setupHomeToSearchTransition()

        val recyclerView = binding.searchResultGrid
        recyclerView.setHasFixedSize(true)

        resultsAdapter = SearchResultsAdapter(
            this,
            onPlay = {
                viewModel.play(it.id)
                navigateUp()
            },
            onBrowse = { media, position ->
                val holder = checkNotNull(recyclerView.findViewHolderForAdapterPosition(position))
                browseMedia(media, holder)
            },
            onAddToPlaylist = { AddToPlaylistDialog.open(this, listOf(it.id)) },
            onExclude = { viewModel.exclude(it.id) },
            onDelete = { DeleteTrackDialog.open(this, it.id) },
        )
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
            setNavigationOnClickListener { navigateUp() }
            setOnMenuItemClickListener {
                if (it.itemId == R.id.action_clear) {
                    clearSearchInput()
                    true
                } else false
            }
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

        viewModel.state.observe(viewLifecycleOwner) {
            resultsAdapter.submitList(it.results)

            if (it.results.isNotEmpty()) {
                startPostponedEnterTransitionWhenDrawn()
            }
        }

        viewModel.deleteEvent.observe(viewLifecycleOwner) { deleteEvent ->
            deleteEvent.handle {
                when (it.result) {
                    is DeleteTracksResult.Deleted -> {
                        showDeleteTrackConfirmation()
                    }
                    is DeleteTracksResult.RequiresPermission -> {
                        requestPermission.launch(it.result.permission)
                    }
                    is DeleteTracksResult.RequiresUserConsent -> {
                        deleteMediaPopup.launch(
                            IntentSenderRequest.Builder(it.result.intent).build()
                        )
                    }
                }
            }
        }

        if (savedInstanceState == null) {
            showKeyboard(binding.searchInput)
        }
    }

    private fun showDeleteTrackConfirmation() {
        Toast.makeText(
            context,
            resources.getQuantityString(R.plurals.deleted_songs_confirmation, 1),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun setupHomeToSearchTransition() {
        val transitionDuration =
            resources.getInteger(CoreUiR.integer.ui_motion_duration_large)
                .toLong()
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.Z, true).apply {
            duration = transitionDuration
        }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.Z, false).apply {
            duration = transitionDuration
        }
    }

    private fun setupHoldTransition() {
        val transitionDuration =
            resources.getInteger(CoreUiR.integer.ui_motion_duration_large)
                .toLong()
        exitTransition = Hold().apply {
            duration = transitionDuration
            addTarget(requireView())
        }
        reenterTransition = null
    }

    private fun setupSharedAxisTransition() {
        val sharedAxisDuration =
            resources.getInteger(CoreUiR.integer.ui_motion_duration_large)
                .toLong()
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

    private fun clearSearchInput() {
        val input = binding!!.searchInput
        input.text = null
        input.requestFocus()
        resultsAdapter.submitList(emptyList())
    }

    private fun navigateUp() {
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

    private fun browseMedia(media: SearchResult.Browsable, holder: RecyclerView.ViewHolder) {
        hideKeyboard(binding!!.searchInput)
        val navController = findNavController()
        val mediaId = media.id.toString()

        when (media.id.type) {
            MediaId.TYPE_ALBUMS -> {
                val toAlbumDetail = SearchFragmentDirections.browseAlbumDetail(mediaId)
                setupHoldTransition()

                val transitionExtras = FragmentNavigatorExtras(holder.itemView to mediaId)
                navController.navigate(toAlbumDetail, transitionExtras)
            }

            MediaId.TYPE_ARTISTS -> {
                val toArtistDetail = SearchFragmentDirections.browseArtistDetail(mediaId)
                setupSharedAxisTransition()
                navController.navigate(toArtistDetail)
            }

            MediaId.TYPE_PLAYLISTS -> {
                val toPlaylistContent = SearchFragmentDirections.browsePlaylistContent(mediaId)

                setupHoldTransition()

                val transitionExtras = FragmentNavigatorExtras(holder.itemView to mediaId)
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
        override fun onItemRangeChanged(positionStart: Int, itemCount: Int, payload: Any?) =
            onChanged()

        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) = onChanged()
        override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) =
            onChanged()

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
