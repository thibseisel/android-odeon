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

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.ConfirmDialogFragment
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.library.MusicLibraryViewModel
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.Stagger
import kotlinx.android.synthetic.main.fragment_playlist_members.*

class MembersFragment : BaseFragment(R.layout.fragment_playlist_members), BaseAdapter.OnItemSelectedListener {

    private val hostViewModel: MusicLibraryViewModel by activityViewModels { viewModelFactory }
    private val viewModel: MembersViewModel by viewModels { viewModelFactory }

    private val args by navArgs<MembersFragmentArgs>()
    private lateinit var adapter: MembersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.setPlaylist(args.playlistId)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.apply {
            inflateMenu(R.menu.menu_playlist_details)
            menu.findItem(R.id.action_delete)?.isVisible = args.isDeletable
            setOnMenuItemClickListener(::onOptionsItemSelected)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        val progressIndicator = view.findViewById<View>(R.id.progress_indicator)
        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            progressIndicator.isVisible = shouldShow
        }

        adapter = MembersAdapter(this, this)
        members_recycler.adapter = adapter
        members_recycler.setHasFixedSize(true)

        // Disable add animations because we'll manually animate those.
        members_recycler.itemAnimator = object : DefaultItemAnimator() {
            override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
                dispatchAddFinished(holder)
                dispatchAddStarting(holder)
                return false
            }
        }

        val staggerTransition = Stagger()

        viewModel.playlist.observe(this, ::onPlaylistDetailLoaded)

        viewModel.members.observe(this) { membersRequest ->
            when (membersRequest) {
                is LoadRequest.Pending -> progressBarLatch.isRefreshing = true
                is LoadRequest.Success -> {
                    progressBarLatch.isRefreshing = false
                    TransitionManager.beginDelayedTransition(members_recycler, staggerTransition)
                    adapter.submitList(membersRequest.data)
                }
                is LoadRequest.Error -> {
                    progressBarLatch.isRefreshing = false
                    adapter.submitList(emptyList())
                }
            }
        }
    }

    private fun onPlaylistDetailLoaded(playlist: MediaBrowserCompat.MediaItem) {
        toolbar.title = playlist.description.title
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> {
                val dialogTitle = getString(
                    R.string.delete_playlist_dialog_title,
                    viewModel.playlist.value?.description?.title
                )

                ConfirmDialogFragment.newInstance(
                    this,
                    REQUEST_DELETE_PLAYLIST,
                    title = dialogTitle,
                    positiveButton = R.string.ok,
                    negativeButton = R.string.cancel
                ).show(requireFragmentManager(), null)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onItemSelected(position: Int) {
        val member = adapter.getItem(position)
        hostViewModel.playMedia(member)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_DELETE_PLAYLIST && resultCode == DialogInterface.BUTTON_POSITIVE) {
            viewModel.deletePlaylist(args.playlistId)
            findNavController().popBackStack()
        }
    }

    private companion object {
        private const val REQUEST_DELETE_PLAYLIST = 66
    }
}
