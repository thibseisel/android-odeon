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
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.view.*
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseFragment
import fr.nihilus.music.extensions.isVisible
import fr.nihilus.music.extensions.observeK
import fr.nihilus.music.library.FRAGMENT_ID
import fr.nihilus.music.library.MusicLibraryViewModel
import fr.nihilus.music.library.NavigationController
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.ConfirmDialogFragment
import fr.nihilus.music.ui.LoadRequest
import fr.nihilus.music.ui.ProgressTimeLatch
import kotlinx.android.synthetic.main.fragment_playlist_members.*
import javax.inject.Inject

class MembersFragment : BaseFragment(), BaseAdapter.OnItemSelectedListener {

    @Inject lateinit var router: NavigationController
    private lateinit var adapter: MembersAdapter

    private val playlist: MediaItem by lazy(LazyThreadSafetyMode.NONE) {
        arguments?.getParcelable<MediaItem>(ARG_PLAYLIST) ?: error("Fragment must be initialized with newInstance")
    }

    private val hostViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(requireActivity())[MusicLibraryViewModel::class.java]
    }

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this, viewModelFactory)[MembersViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        adapter = MembersAdapter(this, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_playlist_members, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.loadTracksOfPlaylist(playlist)

        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            progress_indicator.isVisible = shouldShow
        }

        members_recycler.adapter = adapter
        members_recycler.setHasFixedSize(true)

        viewModel.children.observeK(this) { membersRequest ->
            when (membersRequest) {
                is LoadRequest.Pending -> progressBarLatch.isRefreshing = true
                is LoadRequest.Success -> {
                    progressBarLatch.isRefreshing = false
                    adapter.submitList(membersRequest.data)
                }
                is LoadRequest.Error -> {
                    progressBarLatch.isRefreshing = false
                    adapter.submitList(emptyList())
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_playlist_details, menu)
        menu.findItem(R.id.action_delete).isVisible = arguments!!.getBoolean(ARG_DELETABLE)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> {
                val dialogTitle = getString(
                    R.string.delete_playlist_dialog_title,
                    playlist.description.title
                )
                ConfirmDialogFragment.newInstance(
                    this,
                    REQUEST_DELETE_PLAYLIST,
                    title = dialogTitle,
                    positiveButton = R.string.ok,
                    negativeButton = R.string.cancel
                ).show(fragmentManager, null)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onStart() {
        super.onStart()
        hostViewModel.setToolbarTitle(playlist.description.title)
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val member = adapter.getItem(position)
        hostViewModel.playMedia(member)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_DELETE_PLAYLIST && resultCode == DialogInterface.BUTTON_POSITIVE) {
            viewModel.deletePlaylist(playlist)
            router.navigateBack()
        }
    }

    companion object Factory {
        private const val ARG_PLAYLIST = "playlist"
        private const val ARG_DELETABLE = "deletable"
        private const val REQUEST_DELETE_PLAYLIST = 66

        fun newInstance(playlist: MediaItem, deletable: Boolean) = MembersFragment().apply {
            arguments = Bundle(3).apply {
                putInt(FRAGMENT_ID, R.id.action_playlist)
                putParcelable(ARG_PLAYLIST, playlist)
                putBoolean(ARG_DELETABLE, deletable)
            }
        }
    }
}
