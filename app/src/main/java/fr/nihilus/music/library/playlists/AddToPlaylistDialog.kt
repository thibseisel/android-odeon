/*
 * Copyright 2021 Thibault Seisel
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

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Bitmap
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.LoadRequest
import fr.nihilus.music.core.ui.base.BaseDialogFragment
import fr.nihilus.music.core.ui.base.ListAdapter
import fr.nihilus.music.databinding.PlaylistListItemBinding

/**
 * A fragment displaying an Alert Dialog prompting the user to choose to which playlists
 * he wants to add a set of tracks.
 * He may also trigger another dialog allowing him to create a new playlist from the given tracks.
 */
@AndroidEntryPoint
internal class AddToPlaylistDialog : BaseDialogFragment() {

    private lateinit var playlistAdapter: TargetPlaylistsAdapter

    private val playlistViewModel: PlaylistManagementViewModel by viewModels(
        ::requireParentFragment
    )

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        playlistAdapter = TargetPlaylistsAdapter(this)

        playlistViewModel.userPlaylists.observe(this) {
            if (it is LoadRequest.Success) {
                playlistAdapter.submitList(it.data)
            }
        }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.add_to_playlist)
            .setAdapter(playlistAdapter, dialogEventHandler)
            .setPositiveButton(R.string.action_create_playlist, dialogEventHandler)
            .setNegativeButton(fr.nihilus.music.core.ui.R.string.core_cancel, null)
            .create()
    }

    private val dialogEventHandler = DialogInterface.OnClickListener { _, position ->
        if (position >= 0) {
            // The clicked element is a playlist
            val playlist = playlistAdapter.getItem(position)
            val memberTracks = getPlaylistMembersArgument()
            playlistViewModel.addTracksToPlaylist(playlist, memberTracks)

        } else if (position == DialogInterface.BUTTON_POSITIVE) {
            // The "New playlist" action has been selected
            callNewPlaylistDialog()
        }
    }

    private fun callNewPlaylistDialog() {
        NewPlaylistDialog.open(requireParentFragment(), getPlaylistMembersArgument())
    }

    private fun getPlaylistMembersArgument(): Array<MediaItem> {
        val argument = arguments?.getParcelableArray(ARG_SELECTED_TRACKS) ?: emptyArray()
        return Array(argument.size) { argument[it] as MediaItem }
    }

    /**
     * The adapter used to display available playlists in the dialog's body.
     */
    private class TargetPlaylistsAdapter(
        fragment: Fragment
    ) : ListAdapter<MediaItem, PlaylistHolder>() {
        private val glideRequest = Glide.with(fragment).asBitmap()
            .circleCrop()
            .autoClone()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PlaylistHolder(parent)

        override fun onBindViewHolder(holder: PlaylistHolder, position: Int) {
            holder.bind(getItem(position), glideRequest)
        }
    }

    /**
     * Holds references to views representing a playlist item.
     */
    private class PlaylistHolder(
        parent: ViewGroup
    ) : ListAdapter.ViewHolder(parent, R.layout.playlist_list_item) {
        private val binding = PlaylistListItemBinding.bind(itemView)

        fun bind(playlist: MediaItem, glide: RequestBuilder<Bitmap>) {
            binding.playlistTitle.text = playlist.description.title
            glide.load(playlist.description.iconUri).into(binding.playlistIcon)
        }
    }

    companion object Factory {
        private const val ARG_SELECTED_TRACKS = "fr.nihilus.music.library.SELECTED_TRACKS"

        fun open(caller: Fragment, selectedTracksIds: List<MediaItem>) {
            val dialog = AddToPlaylistDialog().apply {
                arguments = Bundle(1).apply {
                    putParcelableArray(ARG_SELECTED_TRACKS, selectedTracksIds.toTypedArray())
                }
            }
            dialog.show(caller.childFragmentManager, null)
        }
    }
}
