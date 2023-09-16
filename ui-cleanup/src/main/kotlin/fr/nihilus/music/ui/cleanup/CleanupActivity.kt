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

package fr.nihilus.music.ui.cleanup

import android.Manifest
import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.core.compose.theme.OdeonTheme
import fr.nihilus.music.core.ui.observe
import fr.nihilus.music.media.tracks.DeleteTracksResult

/**
 * Lists tracks that could be deleted to free up device space.
 *
 * This feature is exposed as an Activity to avoid sharing display of player controls, which becomes
 * secondary in the context of this feature.
 */
@AndroidEntryPoint
class CleanupActivity : AppCompatActivity() {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.state.observe(this) { state ->
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

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            OdeonTheme {
                var requiresDeleteConsent by rememberSaveable { mutableStateOf(false) }
                CleanupScreen(
                    tracks = state.tracks,
                    selectedCount = state.selectedCount,
                    selectedFreedBytes = state.selectedFreedBytes,
                    toggleTrack = { track -> viewModel.toggleSelection(track.id) },
                    clearSelection = { viewModel.clearSelection() },
                    deleteSelection = { requiresDeleteConsent = true },
                )

                if (requiresDeleteConsent) {
                    ConfirmDeleteDialog(
                        deletedTrackCount = state.selectedCount,
                        cancel = { requiresDeleteConsent = false },
                        accept = {
                            viewModel.deleteSelected()
                            requiresDeleteConsent = false
                        },
                    )
                }
            }
        }
    }
}
