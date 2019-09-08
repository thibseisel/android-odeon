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

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseDialogFragment

class NewPlaylistDialog : BaseDialogFragment() {

    private val playlistViewModel: PlaylistManagementViewModel by viewModels(::requireCallerFragment)
    private lateinit var titleInputView: EditText

    @SuppressLint("InflateParams")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        val inputLayout = LayoutInflater.from(context).inflate(R.layout.new_playlist_input, null)
        inputLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        titleInputView = inputLayout.findViewById(R.id.title_input)

        return AlertDialog.Builder(context)
            .setTitle(R.string.action_create_playlist)
            .setView(inputLayout)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ -> onRequestCreatePlaylist() }
            .create()
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Make the keyboard immediately visible when displaying the dialog.
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    private fun requireCallerFragment(): Fragment =
        targetFragment ?: error("NewPlaylistDialog should be instantiated with newInstance.")

    private fun onRequestCreatePlaylist() {
        val playlistTitle = titleInputView.text.toString()
        val memberTracks = getNewPlaylistMembersArgument()
        playlistViewModel.createPlaylist(playlistTitle, memberTracks)
    }

    private fun getNewPlaylistMembersArgument(): Array<MediaItem> {
        val argument = arguments?.getParcelableArray(ARG_MEMBER_TRACKS) ?: emptyArray()
        return Array(argument.size) { argument[it] as MediaItem }
    }

    companion object Factory {

        private const val ARG_MEMBER_TRACKS = "member_tracks"

        /**
         * The tag associated with this dialog.
         * This may be used to identify the dialog in the fragment manager.
         */
        const val TAG = "NewPlaylistDialog"

        fun newInstance(
            caller: Fragment,
            memberTracks: Array<MediaItem>
        ) = NewPlaylistDialog().apply {
            setTargetFragment(caller, 0)
            arguments = Bundle(1).apply {
                putParcelableArray(ARG_MEMBER_TRACKS, memberTracks)
            }
        }
    }
}