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

package fr.nihilus.music.library.songs

import android.app.Dialog
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.nihilus.music.R
import fr.nihilus.music.library.HomeViewModel
import fr.nihilus.music.library.songs.DeleteTrackDialog.Factory.open

/**
 * An alert dialog that prompts the user for confirmation to delete a single track from its device.
 * Instances of this class should be created with [open].
 */
class DeleteTrackDialog : AppCompatDialogFragment() {
    private val viewModel: HomeViewModel by activityViewModels()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_dialog_title)
            .setMessage(R.string.delete_dialog_message)
            .setPositiveButton(R.string.core_action_delete) { _, _ -> onDelete() }
            .setNegativeButton(R.string.core_cancel, null)
            .create()
    }

    /**
     * Called when the user confirmed its intention to delete the track.
     */
    private fun onDelete() {
        val track = arguments?.getParcelable<MediaBrowserCompat.MediaItem>(ARG_TRACK)
            ?: error("This dialog should have been passed the track to delete as argument.")
        viewModel.deleteSongs(listOf(track))
    }

    companion object Factory {
        private const val ARG_TRACK = "fr.nihilus.music.library.TRACK"

        /**
         * @param caller Caller fragment.
         * @param track The track that may be deleted when confirmation is given.
         */
        fun open(caller: Fragment, track: MediaBrowserCompat.MediaItem) {
            val dialog = DeleteTrackDialog().apply {
                arguments = Bundle(1).apply {
                    putParcelable(ARG_TRACK, track)
                }
            }
            dialog.show(caller.childFragmentManager, null)
        }
    }
}