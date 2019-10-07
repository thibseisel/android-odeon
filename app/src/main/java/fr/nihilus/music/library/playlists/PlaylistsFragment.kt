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

package fr.nihilus.music.library.playlists

import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import fr.nihilus.music.R
import fr.nihilus.music.common.media.MediaId
import fr.nihilus.music.common.media.toMediaId
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.isVisible
import fr.nihilus.music.library.HomeFragmentDirections
import fr.nihilus.music.library.MusicLibraryViewModel
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.core.ui.ProgressTimeLatch
import kotlinx.android.synthetic.main.fragment_playlist.*

class PlaylistsFragment : BaseFragment(R.layout.fragment_playlist), BaseAdapter.OnItemSelectedListener {

    private val hostViewModel: MusicLibraryViewModel by activityViewModels { viewModelFactory }
    private val viewModel: PlaylistsViewModel by viewModels { viewModelFactory }

    private lateinit var adapter: PlaylistsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = PlaylistsAdapter(this, this)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressIndicator = view.findViewById<View>(R.id.progress_indicator)
        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            progressIndicator.isVisible = shouldShow
        }

        playlist_recycler.adapter = adapter
        playlist_recycler.setHasFixedSize(true)

        viewModel.children.observe(this) { playlistsRequest ->
            when (playlistsRequest) {
                is LoadRequest.Pending -> progressBarLatch.isRefreshing = true
                is LoadRequest.Success -> {
                    progressBarLatch.isRefreshing = false
                    adapter.submitList(playlistsRequest.data)
                    group_empty_view.isVisible = playlistsRequest.data.isEmpty()
                }
                is LoadRequest.Error -> {
                    progressBarLatch.isRefreshing = false
                    adapter.submitList(emptyList())
                    group_empty_view.isVisible = true
                }
            }
        }
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val selectedPlaylist = adapter.getItem(position)
        when (actionId) {
            R.id.action_play_item -> hostViewModel.playMedia(selectedPlaylist)
            R.id.action_browse_item -> {
                val selectedPlaylistType = selectedPlaylist.mediaId.toMediaId().type
                val isDeletablePlaylist = selectedPlaylistType == MediaId.TYPE_PLAYLISTS
                val toPlaylistTracks = HomeFragmentDirections.browsePlaylistContent(
                    selectedPlaylist.mediaId!!,
                    isDeletablePlaylist
                )

                findNavController().navigate(toPlaylistTracks)
            }
        }
    }
}
