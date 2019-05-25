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
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.text.InputType
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseDialogFragment
import fr.nihilus.music.extensions.observeK

class NewPlaylistDialog : BaseDialogFragment() {

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this, viewModelFactory)[NewPlaylistViewModel::class.java]
    }

    private lateinit var titleInputView: EditText

    companion object Factory {

        private const val ARG_MEMBER_TRACKS = "member_tracks"

        /**
         * The tag associated with this dialog.
         * This may be used to identify the dialog in the fragment manager.
         */
        const val TAG = "NewPlaylistDialog"

        /**
         * A result code sent to the caller's [Fragment.onActivityResult]
         * indicating that the a playlist with the specified name already exists.
         */
        const val ERROR_ALREADY_EXISTS = -4

        /**
         * The title of the playlist whose failed to be created de to be already existing.
         * This is passed as an extra in the caller's [Fragment.onActivityResult].
         *
         * Type: `String`
         */
        const val RESULT_TAKEN_PLAYLIST_TITLE = "taken_playlist_name"

        fun newInstance(
            caller: Fragment,
            requestCode: Int,
            memberTracks: Array<MediaBrowserCompat.MediaItem>
        ) = NewPlaylistDialog().apply {
                setTargetFragment(caller, requestCode)
                arguments = Bundle(1).apply {
                    putParcelableArray(ARG_MEMBER_TRACKS, memberTracks)
                }
            }
    }

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

        viewModel.createPlaylistResult.observeK(this) { createPlaylistEvent ->
            createPlaylistEvent?.handle(this::onReceivePlaylistCreationResult)
        }
    }

    private fun onReceivePlaylistCreationResult(result: PlaylistEditionResult) {
        when (result) {
            is PlaylistEditionResult.Success -> {
                val data = Intent().apply {
                    putExtra(AddToPlaylistDialog.RESULT_PLAYLIST_TITLE, result.playlistTitle)
                    putExtra(AddToPlaylistDialog.RESULT_TRACK_COUNT, result.trackCount)
                }

                targetFragment?.onActivityResult(targetRequestCode, R.id.abc_result_success, data)
            }

            is PlaylistEditionResult.AlreadyTaken -> {
                val data = Intent().apply {
                    putExtra(RESULT_TAKEN_PLAYLIST_TITLE, result.requestedPlaylistName)
                }

                targetFragment?.onActivityResult(targetRequestCode, ERROR_ALREADY_EXISTS, data)
            }
        }
    }

    private fun onRequestCreatePlaylist() {
        val playlistTitle = titleInputView.text.toString()
        val members = arguments?.getParcelableArray(ARG_MEMBER_TRACKS)
                as? Array<MediaBrowserCompat.MediaItem> ?: emptyArray()

        viewModel.createPlaylist(playlistTitle, members)
    }
}