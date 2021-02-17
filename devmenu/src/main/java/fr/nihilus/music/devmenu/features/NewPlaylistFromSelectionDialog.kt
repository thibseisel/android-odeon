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

package fr.nihilus.music.devmenu.features

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.nihilus.music.devmenu.R

class NewPlaylistFromSelectionDialog : AppCompatDialogFragment() {
    private val viewModel by activityViewModels<ComposerViewModel>()

    private lateinit var titleInputView: EditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        @SuppressLint("InflateParams")
        val inputLayout = LayoutInflater.from(context).inflate(R.layout.dev_new_playlist_input, null)
        inputLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        titleInputView = inputLayout.findViewById(R.id.title_input)

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dev_title_playlist_from_selection)
            .setView(inputLayout)
            .setPositiveButton(R.string.core_ok) { _,  _ ->
                onRequestCreatePlaylist()
            }
            .setNegativeButton(R.string.core_cancel, null)
            .create()
    }

    private fun onRequestCreatePlaylist() {
        val title = titleInputView.text.toString()
        viewModel.saveSelectionAsPlaylist(title)
    }
}