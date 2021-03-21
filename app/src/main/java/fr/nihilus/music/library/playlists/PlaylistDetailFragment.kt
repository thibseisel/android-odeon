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

package fr.nihilus.music.library.playlists

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.transition.MaterialContainerTransform
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.R
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.parse
import fr.nihilus.music.core.ui.ConfirmDialogFragment
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.themeColor
import fr.nihilus.music.databinding.FragmentPlaylistDetailBinding
import fr.nihilus.music.library.MusicLibraryViewModel
import java.util.concurrent.TimeUnit

private const val REQUEST_DELETE_PLAYLIST = "fr.nihilus.music.request.DELETE_PLAYLIST"

@AndroidEntryPoint
class PlaylistDetailFragment : BaseFragment(R.layout.fragment_playlist_detail) {

    private val hostViewModel: MusicLibraryViewModel by activityViewModels()
    private val viewModel: MembersViewModel by viewModels()

    private val args by navArgs<PlaylistDetailFragmentArgs>()
    private lateinit var adapter: MembersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setPlaylist(args.playlistId)

        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = resources.getInteger(R.integer.ui_motion_duration_large).toLong()
            setAllContainerColors(requireContext().themeColor(R.attr.colorSurface))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentPlaylistDetailBinding.bind(view)

        // Wait for playlist tracks to be loaded before triggering enter transition.
        postponeEnterTransition(500, TimeUnit.MILLISECONDS)
        view.transitionName = args.playlistId

        binding.toolbar.apply {
            val playlistType = args.playlistId.parse().type
            menu.findItem(R.id.action_delete)?.isVisible = playlistType == MediaId.TYPE_PLAYLISTS
            setOnMenuItemClickListener(::onOptionsItemSelected)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            binding.progressIndicator.isVisible = shouldShow
        }

        adapter = MembersAdapter(this, ::onTrackSelected)
        binding.playlistTrackList.adapter = adapter
        binding.playlistTrackList.setHasFixedSize(true)

        viewModel.playlist.observe(viewLifecycleOwner) {
            binding.toolbar.title = it.description.title
        }

        viewModel.members.observe(viewLifecycleOwner) { membersRequest ->
            when (membersRequest) {
                is LoadRequest.Pending -> progressBarLatch.isRefreshing = true
                is LoadRequest.Success -> {
                    progressBarLatch.isRefreshing = false
                    adapter.submitList(membersRequest.data)
                    startPostponedEnterTransition()
                }
                is LoadRequest.Error -> {
                    progressBarLatch.isRefreshing = false
                    adapter.submitList(emptyList())
                    startPostponedEnterTransition()
                }
            }
        }

        ConfirmDialogFragment.registerForResult(this, REQUEST_DELETE_PLAYLIST) { button ->
            if (button == ConfirmDialogFragment.ActionButton.POSITIVE) {
                viewModel.deletePlaylist(args.playlistId)
                findNavController().popBackStack()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> {
                val playlistTitle = viewModel.playlist.value?.description?.title
                ConfirmDialogFragment.open(
                    this,
                    REQUEST_DELETE_PLAYLIST,
                    title = getString(
                        R.string.delete_playlist_dialog_title,
                        playlistTitle
                    ),
                    positiveButton = R.string.core_ok,
                    negativeButton = R.string.core_cancel
                )
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun onTrackSelected(position: Int) {
        val member = adapter.getItem(position)
        hostViewModel.playMedia(member)
    }
}
