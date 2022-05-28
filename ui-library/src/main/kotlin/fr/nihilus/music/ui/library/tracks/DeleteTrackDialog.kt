/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.ui.library.tracks

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.parse
import fr.nihilus.music.core.ui.R as CoreUiR

/**
 * An alert dialog that prompts the user for confirmation to delete a single track from its device.
 * Instances of this class should be created with [open].
 */
internal class DeleteTrackDialog : AppCompatDialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_dialog_title)
            .setMessage(R.string.delete_dialog_message)
            .setPositiveButton(CoreUiR.string.core_action_delete) { _, _ -> onDelete() }
            .setNegativeButton(CoreUiR.string.core_cancel, null)
            .create()
    }

    /**
     * Called when the user confirmed its intention to delete the track.
     */
    private fun onDelete() {
        setFragmentResult(REQUEST_DELETE_TRACK, checkNotNull(arguments) {
            "This dialog should have been created using the open function."
        })
    }

    companion object Factory {
        private const val ARG_TRACK = "fr.nihilus.music.library.TRACK"
        private const val REQUEST_DELETE_TRACK = "fr.nihilus.music.library.REQUEST_DELETE_TRACK"

        /**
         * @param caller Caller fragment.
         * @param trackId The track that may be deleted when confirmation is given.
         */
        fun open(caller: Fragment, trackId: MediaId) {
            val dialog = DeleteTrackDialog().apply {
                arguments = Bundle(1).apply {
                    putString(ARG_TRACK, trackId.encoded)
                }
            }
            dialog.show(caller.childFragmentManager, null)
        }

        fun registerForResult(caller: Fragment, listener: (MediaId) -> Unit) {
            caller.childFragmentManager.setFragmentResultListener(
                REQUEST_DELETE_TRACK,
                caller
            ) { _, bundle ->
                val trackId = bundle.getString(ARG_TRACK).parse()
                listener(trackId)
            }
        }
    }
}
