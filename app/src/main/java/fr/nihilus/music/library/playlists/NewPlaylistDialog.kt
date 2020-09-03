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

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.nihilus.music.R
import fr.nihilus.music.core.ui.base.BaseDialogFragment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

class NewPlaylistDialog : BaseDialogFragment() {
    private val playlistViewModel: PlaylistManagementViewModel by viewModels(::requireCallerFragment)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        @SuppressLint("InflateParams")
        val inputLayout = LayoutInflater.from(context).inflate(R.layout.new_playlist_input, null)
        inputLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val titleInputView = inputLayout.findViewById<EditText>(R.id.title_input)

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.action_create_playlist)
            .setView(inputLayout)
            .setNegativeButton(R.string.core_cancel, null)
            .setPositiveButton(R.string.core_ok) { _, _ ->
                val currentPlaylistTitle = titleInputView.text?.toString()
                check(isValidTitle(currentPlaylistTitle))
                onRequestCreatePlaylist(currentPlaylistTitle)
            }
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            saveButton.isEnabled = isValidTitle(titleInputView.text)
            titleInputView.doAfterTextChanged { playlistTitle ->
                val hasValidPlaylistTitle = isValidTitle(playlistTitle)
                saveButton.isEnabled = hasValidPlaylistTitle
            }
        }

        return dialog
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        // Make the keyboard immediately visible when displaying the dialog.
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    private fun requireCallerFragment(): Fragment =
        targetFragment ?: error("NewPlaylistDialog should be instantiated with newInstance.")

    private fun onRequestCreatePlaylist(playlistTitle: String) {
        val memberTracks = getNewPlaylistMembersArgument()
        playlistViewModel.createPlaylist(playlistTitle, memberTracks)
    }

    private fun getNewPlaylistMembersArgument(): Array<MediaItem> {
        val argument = arguments?.getParcelableArray(ARG_MEMBER_TRACKS) ?: emptyArray()
        return Array(argument.size) { argument[it] as MediaItem }
    }

    @OptIn(ExperimentalContracts::class)
    private fun isValidTitle(playlistTitle: CharSequence?): Boolean {
        contract {
            returns(true) implies (playlistTitle != null)
        }
        return playlistTitle?.isNotBlank() == true
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