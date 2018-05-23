/*
 * Copyright 2018 Thibault Seisel
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

package fr.nihilus.music.ui.playlist

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import dagger.android.support.AndroidSupportInjection
import fr.nihilus.music.Constants
import fr.nihilus.music.R
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.client.NavigationController
import fr.nihilus.music.di.ActivityScoped
import fr.nihilus.music.media.CATEGORY_PLAYLISTS
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.recyclerfragment.RecyclerFragment
import javax.inject.Inject

@ActivityScoped
class PlaylistsFragment : RecyclerFragment(), BaseAdapter.OnItemSelectedListener {

    @Inject lateinit var router: NavigationController
    private lateinit var adapter: PlaylistsAdapter
    private lateinit var viewModel: BrowserViewModel

    override fun onAttach(context: Context?) {
        AndroidSupportInjection.inject(this)
        super.onAttach(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        adapter = PlaylistsAdapter(this, this)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(activity!!).get(BrowserViewModel::class.java)
        viewModel.subscribeTo(CATEGORY_PLAYLISTS).observe(this, Observer {
            adapter.submitList(it.orEmpty())
            setRecyclerShown(true)
        })

        recyclerView.setHasFixedSize(true)
        setAdapter(adapter)

        if (savedInstanceState == null) {
            setRecyclerShown(false)
        }
    }

    override fun onStart() {
        super.onStart()
        activity!!.setTitle(R.string.action_playlists)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_playlist, container, false)

    override fun onItemSelected(position: Int, actionId: Int) {
        val selectedPlaylist = adapter.getItem(position)
        when (actionId) {
            R.id.action_browse_item -> router.navigateToPlaylistDetails(selectedPlaylist)
            R.id.action_play_item -> onPlay(selectedPlaylist)
        }
    }

    private fun onPlay(playlist: MediaItem) {
        val mediaId = playlist.mediaId ?: throw AssertionError("Playlists should have a MediaId")
        viewModel.post { controller ->
            controller.transportControls.playFromMediaId(mediaId, null)
        }
    }

    companion object Factory {

        fun newInstance() = PlaylistsFragment().apply {
            arguments = Bundle(1).apply {
                putInt(Constants.FRAGMENT_ID, R.id.action_playlist)
            }
        }
    }
}
