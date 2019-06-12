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

import android.app.Dialog
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseDialogFragment

class NewPlaylistDialog : BaseDialogFragment() {

    private val playlistViewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(targetFragment ?: this, viewModelFactory)[PlaylistManagementViewModel::class.java]
    }

    private lateinit var titleInputView: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inputPadding = resources.getDimensionPixelSize(R.dimen.playlist_name_input_padding)
        titleInputView = EditText(context).apply {
            inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            hint = getString(R.string.hint_playlist_title)
            setPadding(inputPadding, inputPadding, inputPadding, inputPadding)
        }

        return AlertDialog.Builder(context!!)
            .setTitle(R.string.action_create_playlist)
            .setView(titleInputView)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ -> onRequestCreatePlaylist() }
            .create()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Make the keyboard immediately visible when displaying the dialog.
        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    private fun onRequestCreatePlaylist() {
        val playlistTitle = titleInputView.text.toString()
        val members = arguments?.getParcelableArray(ARG_MEMBER_TRACKS) as? Array<MediaItem> ?: emptyArray()
        playlistViewModel.createPlaylist(playlistTitle, members)
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