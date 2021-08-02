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

package fr.nihilus.music.library.songs

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.view.View
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.transition.TransitionManager
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.motion.Stagger
import fr.nihilus.music.databinding.FragmentAllTracksBinding
import fr.nihilus.music.library.HomeViewModel
import fr.nihilus.music.library.MusicLibraryViewModel
import fr.nihilus.music.library.playlists.AddToPlaylistDialog
import fr.nihilus.music.library.playlists.PlaylistActionResult
import fr.nihilus.music.library.playlists.PlaylistManagementViewModel

@AndroidEntryPoint
class AllTracksFragment : BaseFragment(R.layout.fragment_all_tracks) {

    private val hostViewModel: MusicLibraryViewModel by activityViewModels()
    private val viewModel: HomeViewModel by activityViewModels()
    private val playlistViewModel: PlaylistManagementViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAllTracksBinding.bind(view)

        val songAdapter = SongAdapter(this, ::onTrackAction)
        binding.trackList.adapter = songAdapter

        val progressBarLatch = ProgressTimeLatch { shouldShowProgress ->
            binding.progressIndicator.isVisible = shouldShowProgress
            binding.trackList.isVisible = !shouldShowProgress
        }

        val staggerTransition = Stagger()

        viewModel.tracks.observe(viewLifecycleOwner) { itemRequest ->
            when (itemRequest) {
                is LoadRequest.Pending -> progressBarLatch.isRefreshing = true
                is LoadRequest.Success -> {
                    progressBarLatch.isRefreshing = false
                    TransitionManager.beginDelayedTransition(binding.trackList, staggerTransition)
                    songAdapter.submitList(itemRequest.data)
                    binding.emptyViewGroup.isVisible = itemRequest.data.isEmpty()
                }
                is LoadRequest.Error -> {
                    progressBarLatch.isRefreshing = false
                    songAdapter.submitList(emptyList())
                    binding.emptyViewGroup.isVisible = true
                }
            }
        }

        playlistViewModel.playlistActionResult.observe(viewLifecycleOwner) { playlistEvent ->
            playlistEvent.handle { result ->
                when (result) {
                    is PlaylistActionResult.Created -> {
                        val userMessage = getString(R.string.playlist_created, result.playlistName)
                        Toast.makeText(context, userMessage, Toast.LENGTH_SHORT).show()
                    }

                    is PlaylistActionResult.Edited -> {
                        val userMessage = resources.getQuantityString(
                            R.plurals.tracks_added_to_playlist,
                            result.addedTracksCount,
                            result.addedTracksCount,
                            result.playlistName
                        )
                        Toast.makeText(context, userMessage, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    /**
     * Called when an action has been triggered on a given track.
     *
     * @param track The track to execute the action on.
     * @param action The action that should be executed on the selected track.
     */
    private fun onTrackAction(track: MediaBrowserCompat.MediaItem, action: SongAdapter.ItemAction) {
        when (action) {
            SongAdapter.ItemAction.PLAY -> {
                hostViewModel.playMedia(track)
            }

            SongAdapter.ItemAction.DELETE -> {
                DeleteTrackDialog.open(this, track)
            }

            SongAdapter.ItemAction.EXCLUDE -> {
                viewModel.excludeTrack(track)
            }

            SongAdapter.ItemAction.ADD_TO_PLAYLIST -> {
                AddToPlaylistDialog.open(this, listOf(track))
            }
        }
    }
}
