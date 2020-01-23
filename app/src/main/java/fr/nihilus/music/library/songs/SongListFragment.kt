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

package fr.nihilus.music.library.songs

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.view.View
import android.widget.ListView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.transition.TransitionManager
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.library.HomeViewModel
import fr.nihilus.music.library.MusicLibraryViewModel
import fr.nihilus.music.library.playlists.AddToPlaylistDialog
import fr.nihilus.music.library.playlists.PlaylistActionResult
import fr.nihilus.music.library.playlists.PlaylistManagementViewModel
import fr.nihilus.music.ui.Stagger
import kotlinx.android.synthetic.main.fragment_songs.*

class SongListFragment : BaseFragment(R.layout.fragment_songs) {

    private val hostViewModel: MusicLibraryViewModel by activityViewModels()
    private val viewModel: HomeViewModel by viewModels(::requireParentFragment)
    private val playlistViewModel: PlaylistManagementViewModel by viewModels { viewModelFactory }

    private lateinit var songAdapter: SongAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        songAdapter = SongAdapter(this, ::onTrackAction)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressIndicator = view.findViewById<View>(R.id.progress_indicator)
        val songsListView = view.findViewById<ListView>(R.id.songs_listview)
        songsListView.adapter = songAdapter

        val progressBarLatch = ProgressTimeLatch { shouldShowProgress ->
            progressIndicator.isVisible = shouldShowProgress
            songsListView.isVisible = !shouldShowProgress
        }


        val staggerTransition = Stagger()

        viewModel.tracks.observe(this) { itemRequest ->
            when (itemRequest) {
                is LoadRequest.Pending -> progressBarLatch.isRefreshing = true
                is LoadRequest.Success -> {
                    progressBarLatch.isRefreshing = false
                    TransitionManager.beginDelayedTransition(songs_listview, staggerTransition)
                    songAdapter.submitList(itemRequest.data)
                    group_empty_view.isVisible = itemRequest.data.isEmpty()
                }
                is LoadRequest.Error -> {
                    progressBarLatch.isRefreshing = false
                    songAdapter.submitList(emptyList())
                    group_empty_view.isVisible = true
                }
            }
        }

        playlistViewModel.playlistActionResult.observe(this) { playlistEvent ->
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
                val dialog = DeleteTrackDialog.newInstance(track)
                dialog.show(parentFragmentManager, DeleteTrackDialog.TAG)
            }

            SongAdapter.ItemAction.ADD_TO_PLAYLIST -> {
                val dialog = AddToPlaylistDialog.newInstance(this, listOf(track))
                dialog.show(parentFragmentManager, AddToPlaylistDialog.TAG)
            }
        }
    }
}
