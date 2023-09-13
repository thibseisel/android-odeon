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

package fr.nihilus.music.ui.cleanup

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ActionMode
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.core.compose.theme.OdeonTheme
import fr.nihilus.music.core.ui.extensions.startActionMode
import fr.nihilus.music.core.ui.observe
import fr.nihilus.music.media.tracks.DeleteTracksResult

/**
 * Lists tracks that could be deleted from the device's storage to free-up space.
 */
@AndroidEntryPoint
internal class CleanupFragment : Fragment() {
    private val viewModel by viewModels<CleanupViewModel>()

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionGranted ->
        if (permissionGranted) {
            viewModel.deleteSelected()
        }
    }

    private val deleteMediaPopup = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.clearSelection()
        }
    }

    private var actionMode: ActionMode? = null

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            actionMode = mode
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = false

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false

        override fun onDestroyActionMode(mode: ActionMode?) {
            actionMode = null
            viewModel.clearSelection()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val state by viewModel.state.collectAsState()

            OdeonTheme {
                var requiresDeleteConsent by remember { mutableStateOf(false) }

                CleanupScreen(
                    tracks = state.tracks,
                    selectedCount = state.selectedCount,
                    toggleTrack = { track -> viewModel.toggleSelection(track.id) },
                    deleteSelection = { requiresDeleteConsent = true },
                )

                if (requiresDeleteConsent) {
                    ConfirmDeleteDialog(
                        deletedTrackCount = state.selectedCount,
                        accept = { viewModel.deleteSelected() },
                        cancel = { requiresDeleteConsent = false }
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.state.observe(viewLifecycleOwner) { state ->
            if (state.selectedCount > 0) {
                val actionMode = actionMode ?: startActionMode(actionModeCallback)
                actionMode?.apply {
                    title = resources.getQuantityString(
                        R.plurals.number_of_selected_tracks,
                        state.selectedCount,
                        state.selectedCount,
                    )
                    subtitle = formatToHumanReadableByteCount(state.selectedFreedBytes)
                }
            } else {
                actionMode?.finish()
            }

            if (state.result != null) {
                when (state.result) {
                    is DeleteTracksResult.Deleted -> {}
                    is DeleteTracksResult.RequiresPermission -> {
                        requestPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    }

                    is DeleteTracksResult.RequiresUserConsent -> {
                        deleteMediaPopup.launch(
                            IntentSenderRequest.Builder(state.result.intent).build()
                        )
                    }
                }
                viewModel.acknowledgeResult()
            }
        }
    }
}
