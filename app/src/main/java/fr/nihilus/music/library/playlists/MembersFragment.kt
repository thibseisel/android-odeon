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
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseFragment
import fr.nihilus.music.extensions.isVisible
import fr.nihilus.music.library.MusicLibraryViewModel
import fr.nihilus.music.ui.BaseAdapter
import fr.nihilus.music.ui.ConfirmDialogFragment
import fr.nihilus.music.ui.LoadRequest
import fr.nihilus.music.ui.ProgressTimeLatch
import kotlinx.android.synthetic.main.fragment_playlist_members.*

class MembersFragment : BaseFragment(), BaseAdapter.OnItemSelectedListener {

    private val hostViewModel: MusicLibraryViewModel by activityViewModels { viewModelFactory }
    private val viewModel: MembersViewModel by viewModels { viewModelFactory }

    private val args by navArgs<MembersFragmentArgs>()
    private lateinit var adapter: MembersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        adapter = MembersAdapter(this, this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_playlist_members, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.apply {
            title = args.playlist.description.title
            setNavigationOnClickListener { findNavController().navigateUp() }

            inflateMenu(R.menu.menu_playlist_details)
            menu.findItem(R.id.action_delete)?.isVisible = args.isDeletable
            setOnMenuItemClickListener(::onOptionsItemSelected)
        }

        viewModel.loadTracksOfPlaylist(args.playlist)

        val progressIndicator = view.findViewById<View>(R.id.progress_indicator)
        val progressBarLatch = ProgressTimeLatch { shouldShow ->
            progressIndicator.isVisible = shouldShow
        }

        members_recycler.adapter = adapter
        members_recycler.setHasFixedSize(true)

        viewModel.children.observe(this) { membersRequest ->
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> {
                val dialogTitle = getString(
                    R.string.delete_playlist_dialog_title,
                    args.playlist.description.title
                )
                ConfirmDialogFragment.newInstance(
                    this,
                    REQUEST_DELETE_PLAYLIST,
                    title = dialogTitle,
                    positiveButton = R.string.ok,
                    negativeButton = R.string.cancel
                ).show(childFragmentManager, null)
                return true
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onItemSelected(position: Int, actionId: Int) {
        val member = adapter.getItem(position)
        hostViewModel.playMedia(member)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_DELETE_PLAYLIST && resultCode == DialogInterface.BUTTON_POSITIVE) {
            viewModel.deletePlaylist(args.playlist)
            findNavController().popBackStack()
        }
    }

    private companion object {
        private const val REQUEST_DELETE_PLAYLIST = 66
    }
}
