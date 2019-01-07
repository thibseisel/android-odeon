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

import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseFragment
import fr.nihilus.music.dagger.ActivityScoped
import fr.nihilus.music.extensions.isVisible
import fr.nihilus.music.extensions.observeK
import fr.nihilus.music.library.FRAGMENT_ID
import fr.nihilus.music.library.NavigationController
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.LoadRequest
import fr.nihilus.music.ui.ProgressTimeLatch
import kotlinx.android.synthetic.main.fragment_playlist.*
import javax.inject.Inject

@ActivityScoped
class PlaylistsFragment : BaseFragment(), BaseAdapter.OnItemSelectedListener {

    @Inject lateinit var router: NavigationController
    private lateinit var adapter: PlaylistsAdapter

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this, viewModelFactory)[PlaylistsViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        adapter = PlaylistsAdapter(this, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_playlist, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            progress_indicator.isVisible = shouldShow
        }

        playlist_recycler.adapter = adapter
        playlist_recycler.setHasFixedSize(true)

        viewModel.playlists.observeK(this) { playlistsRequest ->
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

    override fun onStart() {
        super.onStart()
        activity!!.setTitle(R.string.action_playlists)
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val selectedPlaylist = adapter.getItem(position)
        when (actionId) {
            R.id.action_browse_item -> router.navigateToPlaylistDetails(selectedPlaylist)
            R.id.action_play_item -> viewModel.playAllOfPlaylist(selectedPlaylist)
        }
    }

    companion object Factory {

        fun newInstance() = PlaylistsFragment().apply {
            arguments = Bundle(1).apply {
                putInt(FRAGMENT_ID, R.id.action_playlist)
            }
        }
    }
}
