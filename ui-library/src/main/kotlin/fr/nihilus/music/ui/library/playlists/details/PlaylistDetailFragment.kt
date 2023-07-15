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

package fr.nihilus.music.ui.library.playlists.details

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.core.ui.ConfirmDialogFragment
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.themeColor
import fr.nihilus.music.core.ui.observe
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.ui.library.databinding.FragmentPlaylistDetailBinding
import java.util.concurrent.TimeUnit
import fr.nihilus.music.core.ui.R as CoreUiR

private const val REQUEST_DELETE_PLAYLIST = "fr.nihilus.music.request.DELETE_PLAYLIST"

@AndroidEntryPoint
internal class PlaylistDetailFragment : BaseFragment(R.layout.fragment_playlist_detail) {
    private val viewModel: PlaylistDetailsViewModel by viewModels()

    private val args by navArgs<PlaylistDetailFragmentArgs>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = CoreUiR.id.nav_host_fragment
            duration = resources.getInteger(CoreUiR.integer.ui_motion_duration_large).toLong()
            setAllContainerColors(requireContext().themeColor(com.google.android.material.R.attr.colorSurface))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentPlaylistDetailBinding.bind(view)

        // Wait for playlist tracks to be loaded before triggering enter transition.
        postponeEnterTransition(500, TimeUnit.MILLISECONDS)
        view.transitionName = args.playlistId

        binding.toolbar.apply {
            setOnMenuItemClickListener(::onToolbarMenuItemSelected)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        val deleteMenuItem = binding.toolbar.menu.findItem(R.id.action_delete)
        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            binding.progressIndicator.isVisible = shouldShow
        }

        val trackAdapter = PlaylistTracksAdapter(this) {
            viewModel.play(it)
        }

        binding.playlistTrackList.apply {
            setHasFixedSize(true)
            adapter = trackAdapter
        }

        viewModel.state.observe(viewLifecycleOwner) {
            binding.toolbar.title = it.playlistTitle
            deleteMenuItem.isVisible = it.isUserDefined
            progressBarLatch.isRefreshing = it.isLoadingTracks
            trackAdapter.submitList(it.tracks)

            if (!it.isLoadingTracks && it.tracks.isNotEmpty()) {
                startPostponedEnterTransition()
            }
        }

        ConfirmDialogFragment.registerForResult(this, REQUEST_DELETE_PLAYLIST) { button ->
            if (button == ConfirmDialogFragment.ActionButton.POSITIVE) {
                viewModel.deleteThisPlaylist()
                findNavController().popBackStack()
            }
        }
    }

    private fun onToolbarMenuItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_delete -> {
            val playlistTitle = viewModel.state.value.playlistTitle
            ConfirmDialogFragment.open(
                this,
                REQUEST_DELETE_PLAYLIST,
                title = getString(
                    R.string.delete_playlist_dialog_title,
                    playlistTitle
                ),
                positiveButton = CoreUiR.string.core_ok,
                negativeButton = CoreUiR.string.core_cancel
            )
            true
        }

        else -> false
    }
}
