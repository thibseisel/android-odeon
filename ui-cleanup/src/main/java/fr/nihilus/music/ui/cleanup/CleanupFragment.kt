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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ActionMode
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.core.ui.ConfirmDialogFragment
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.doOnApplyWindowInsets
import fr.nihilus.music.core.ui.extensions.startActionMode
import fr.nihilus.music.core.ui.observe
import fr.nihilus.music.core.ui.view.DividerItemDecoration
import fr.nihilus.music.media.tracks.DeleteTracksResult
import fr.nihilus.music.ui.cleanup.databinding.FragmentCleanupBinding
import fr.nihilus.music.core.ui.R as CoreUiR

/**
 * Code associated with the request to confirm deleting tracks.
 */
private const val REQUEST_CONFIRM_CLEANUP = "fr.nihilus.music.request.CONFIRM_CLEANUP"

/**
 * Lists tracks that could be deleted from the device's storage to free-up space.
 */
@AndroidEntryPoint
internal class CleanupFragment : BaseFragment(R.layout.fragment_cleanup) {
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentCleanupBinding.bind(view)
        configureViewOffsetForSystemBars(binding)

        val trackAdapter = CleanupAdapter { viewModel.toggleSelection(it.id) }
        binding.disposableTrackList.apply {
            adapter = trackAdapter
            setHasFixedSize(true)
            addItemDecoration(
                DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
            )
        }


        binding.deleteTracksButton.setOnClickListener {
            val selectedCount = viewModel.state.value.selectedCount
            askCleanupConfirmation(selectedCount)
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            trackAdapter.submitList(state.tracks)

            if (state.selectedCount > 0) {
                binding.deleteTracksButton.show()
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
                binding.deleteTracksButton.hide()
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

        ConfirmDialogFragment.registerForResult(this, REQUEST_CONFIRM_CLEANUP) { result ->
            if (result == ConfirmDialogFragment.ActionButton.POSITIVE) {
                viewModel.deleteSelected()
            }
        }
    }

    private fun configureViewOffsetForSystemBars(bindings: FragmentCleanupBinding) {
        bindings.disposableTrackList.doOnApplyWindowInsets { view, insets, padding, _ ->
            val systemWindowInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = padding.left + systemWindowInsets.left,
                right = padding.right + systemWindowInsets.right,
                bottom = padding.bottom + systemWindowInsets.bottom
            )
        }

        bindings.deleteTracksButton.doOnApplyWindowInsets { view, insets, _, margin ->
            val layoutParams = view.layoutParams as ViewGroup.MarginLayoutParams
            val tappableInsets = insets.getInsets(WindowInsetsCompat.Type.tappableElement())
            layoutParams.leftMargin = margin.left + tappableInsets.left
            layoutParams.rightMargin = margin.right + tappableInsets.right
            layoutParams.bottomMargin = margin.bottom + tappableInsets.bottom
        }
    }

    private fun askCleanupConfirmation(selectedCount: Int) {
        ConfirmDialogFragment.open(
            this,
            REQUEST_CONFIRM_CLEANUP,
            title = resources.getQuantityString(
                R.plurals.cleanup_confirmation_title,
                selectedCount,
                selectedCount
            ),
            message = getString(R.string.cleanup_confirmation_message),
            positiveButton = CoreUiR.string.core_action_delete,
            negativeButton = CoreUiR.string.core_cancel
        )
    }
}
