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

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.R
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.ui.base.BaseDialogFragment
import fr.nihilus.music.databinding.NewPlaylistInputBinding
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@AndroidEntryPoint
internal class NewPlaylistDialog : BaseDialogFragment() {
    private val playlistViewModel: PlaylistManagementViewModel by viewModels(::requireParentFragment)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val context = requireContext()

        val binding = NewPlaylistInputBinding.inflate(LayoutInflater.from(context))
        binding.root.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle(R.string.action_create_playlist)
            .setView(binding.root)
            .setNegativeButton(fr.nihilus.music.core.ui.R.string.core_cancel, null)
            .setPositiveButton(fr.nihilus.music.core.ui.R.string.core_ok) { _, _ ->
                val currentPlaylistTitle = binding.titleInput.text?.toString()
                check(isValidTitle(currentPlaylistTitle))
                onRequestCreatePlaylist(currentPlaylistTitle)
            }
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
            saveButton.isEnabled = isValidTitle(binding.titleInput.text)
            binding.titleInput.doAfterTextChanged { playlistTitle ->
                val hasValidPlaylistTitle = isValidTitle(playlistTitle)
                saveButton.isEnabled = hasValidPlaylistTitle
            }
        }

        return dialog
    }

    private fun onRequestCreatePlaylist(playlistTitle: String) {
        val memberTracks = getNewPlaylistMembersArgument()
        playlistViewModel.createPlaylist(playlistTitle, memberTracks)
    }

    private fun getNewPlaylistMembersArgument(): List<MediaId> = arguments
        ?.getStringArrayList(ARG_MEMBER_TRACKS)
        ?.map { MediaId.parse(it) }
        ?: emptyList()

    @OptIn(ExperimentalContracts::class)
    private fun isValidTitle(playlistTitle: CharSequence?): Boolean {
        contract {
            returns(true) implies (playlistTitle != null)
        }
        return playlistTitle?.isNotBlank() == true
    }

    companion object Factory {
        private const val ARG_MEMBER_TRACKS = "member_tracks_ids"

        fun open(caller: Fragment, memberTracks: List<MediaId>) {
            val dialog = NewPlaylistDialog().apply {
                arguments = Bundle(1).apply {
                    putStringArrayList(
                        ARG_MEMBER_TRACKS,
                        memberTracks.mapTo(ArrayList(memberTracks.size), MediaId::encoded)
                    )
                }
            }
            dialog.show(caller.childFragmentManager, null)
        }
    }
}
