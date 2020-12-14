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

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.TransitionManager
import fr.nihilus.music.R
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.toMediaId
import fr.nihilus.music.core.ui.ConfirmDialogFragment
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.motion.Stagger
import fr.nihilus.music.databinding.FragmentPlaylistMembersBinding
import fr.nihilus.music.library.MusicLibraryViewModel

class MembersFragment : BaseFragment(R.layout.fragment_playlist_members) {

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
        val binding = FragmentPlaylistMembersBinding.bind(view)

        binding.toolbar.apply {
            val playlistType = args.playlistId.toMediaId().type
            menu.findItem(R.id.action_delete)?.isVisible = playlistType == MediaId.TYPE_PLAYLISTS
            setOnMenuItemClickListener(::onOptionsItemSelected)
            setNavigationOnClickListener { findNavController().navigateUp() }
        }

        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            binding.progressIndicator.isVisible = shouldShow
        }

        adapter = MembersAdapter(this, ::onTrackSelected)
        binding.membersRecycler.adapter = adapter
        binding.membersRecycler.setHasFixedSize(true)

        // Disable add animations because we'll manually animate those.
        binding.membersRecycler.itemAnimator = object : DefaultItemAnimator() {
            override fun animateAdd(holder: RecyclerView.ViewHolder?): Boolean {
                dispatchAddFinished(holder)
                dispatchAddStarting(holder)
                return false
            }
        }

        val staggerTransition = Stagger()

        viewModel.playlist.observe(viewLifecycleOwner, {
            binding.toolbar.title = it.description.title
        })

        viewModel.members.observe(viewLifecycleOwner) { membersRequest ->
            when (membersRequest) {
                is LoadRequest.Pending -> progressBarLatch.isRefreshing = true
                is LoadRequest.Success -> {
                    progressBarLatch.isRefreshing = false
                    TransitionManager.beginDelayedTransition(binding.membersRecycler, staggerTransition)
                    adapter.submitList(membersRequest.data)
                }
                is LoadRequest.Error -> {
                    progressBarLatch.isRefreshing = false
                    adapter.submitList(emptyList())
                }
            }
        }
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
                    positiveButton = R.string.core_ok,
                    negativeButton = R.string.core_cancel
                ).show(parentFragmentManager, null)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun onTrackSelected(position: Int) {
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
