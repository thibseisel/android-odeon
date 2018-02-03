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

import android.app.Dialog
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import android.text.InputType
import android.widget.EditText
import fr.nihilus.music.R
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.command.NewPlaylistCommand

class NewPlaylistDialog : AppCompatDialogFragment() {

    private lateinit var browserModel: BrowserViewModel
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

        fun newInstance(caller: Fragment, requestCode: Int, memberTracks: LongArray) =
            NewPlaylistDialog().apply {
                setTargetFragment(caller, requestCode)
                arguments = Bundle(1).apply {
                    putLongArray(ARG_MEMBER_TRACKS, memberTracks)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        browserModel = ViewModelProviders.of(activity!!)[BrowserViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        titleInputView = EditText(context).apply {
            inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            hint = getString(R.string.hint_playlist_title)
        }

        return AlertDialog.Builder(context!!)
            .setTitle(R.string.action_create_playlist)
            .setView(titleInputView)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.ok) { _, _ -> onRequestCreatePlaylist() }
            .create()
    }

    private fun onRequestCreatePlaylist() {
        val playlistTitle = titleInputView.text.toString()
        val memberTrackIds = arguments?.getLongArray(ARG_MEMBER_TRACKS) ?: LongArray(0)

        val params = Bundle(2).apply {
            putString(NewPlaylistCommand.PARAM_TITLE, playlistTitle)
            putLongArray(NewPlaylistCommand.PARAM_TRACK_IDS, memberTrackIds)
        }

        browserModel.postCommand(NewPlaylistCommand.CMD_NAME, params) { resultCode, _ ->
            when (resultCode) {
                R.id.result_success -> {
                    val data = Intent().apply {
                        putExtra(AddToPlaylistDialog.RESULT_PLAYLIST_TITLE, playlistTitle)
                        putExtra(AddToPlaylistDialog.RESULT_TRACK_COUNT, memberTrackIds.size)
                    }

                    targetFragment?.onActivityResult(targetRequestCode, R.id.result_success, data)
                }

                R.id.error_playlist_already_exists -> {
                    val data = Intent().apply {
                        putExtra(RESULT_TAKEN_PLAYLIST_TITLE, playlistTitle)
                    }

                    targetFragment?.onActivityResult(targetRequestCode,
                        R.id.error_playlist_already_exists, data)
                }
            }
        }
    }
}