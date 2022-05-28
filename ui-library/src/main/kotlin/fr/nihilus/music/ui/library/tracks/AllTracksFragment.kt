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

import android.Manifest
import android.app.Activity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.transition.TransitionManager
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.ui.library.R
import fr.nihilus.music.core.ui.ProgressTimeLatch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.motion.Stagger
import fr.nihilus.music.core.ui.observe
import fr.nihilus.music.ui.library.databinding.FragmentAllTracksBinding
import fr.nihilus.music.ui.library.playlists.AddToPlaylistDialog

/**
 * Lists all audio tracks available to the application.
 */
@AndroidEntryPoint
internal class AllTracksFragment : BaseFragment(R.layout.fragment_all_tracks) {
    private val viewModel: TracksViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.consumeEvent()
        val permissionEvent =
            viewModel.state.value.pendingEvent as? TrackEvent.RequiresStoragePermission
        if (granted && permissionEvent != null) {
            viewModel.deleteTrack(permissionEvent.trackId)
        }
    }

    private val deleteMediaPopup = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            notifyTrackDeleted()
            viewModel.consumeEvent()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentAllTracksBinding.bind(view)

        DeleteTrackDialog.registerForResult(this) { targetTrackId ->
            viewModel.deleteTrack(targetTrackId)
        }

        val tracksAdapter = TrackListAdapter(
            fragment = this,
            addToPlaylist = { AddToPlaylistDialog.open(this, listOf(it.id)) },
            exclude = { viewModel.excludeTrack(it.id) },
            delete = { DeleteTrackDialog.open(this, trackId = it.id) },
            play = { viewModel.playTrack(it.id) }
        ).also {
            binding.trackList.adapter = it
        }

        val progressBarLatch = ProgressTimeLatch { shouldShowProgress ->
            binding.progressIndicator.isVisible = shouldShowProgress
            binding.trackList.isVisible = !shouldShowProgress
        }

        val staggerTransition = Stagger()

        viewModel.state.observe(viewLifecycleOwner) { (tracks, loading, pendingEvent) ->
            progressBarLatch.isRefreshing = loading && tracks.isEmpty()
            binding.emptyViewGroup.isVisible = !loading && tracks.isEmpty()
            TransitionManager.beginDelayedTransition(binding.trackList, staggerTransition)
            tracksAdapter.submitList(tracks)

            if (pendingEvent != null) {
                handleEvent(pendingEvent)
            }
        }
    }

    private fun handleEvent(event: TrackEvent) = when (event) {
        is TrackEvent.TrackSuccessfullyDeleted -> {
            notifyTrackDeleted()
            viewModel.consumeEvent()
        }
        is TrackEvent.RequiresStoragePermission -> {
            requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        is TrackEvent.RequiresUserConsent -> {
            deleteMediaPopup.launch(
                IntentSenderRequest.Builder(event.intent).build()
            )
        }
    }

    private fun notifyTrackDeleted() {
        val message = resources.getQuantityString(
            R.plurals.deleted_songs_confirmation,
            1,
        )
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
