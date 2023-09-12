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

package fr.nihilus.music.ui.settings.exclusion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import fr.nihilus.music.core.compose.theme.OdeonTheme

/**
 * Display the list of tracks that have been excluded from the music library by users.
 * Tracks listed here can be allowed again by swiping them.
 */
@AndroidEntryPoint
internal class ExcludedTracksFragment : Fragment() {
    private val viewModel by viewModels<ExcludedTracksViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            OdeonTheme {
                val tracks by viewModel.tracks.collectAsStateWithLifecycle()
                ExcludedTracksScreen(
                    tracks = tracks,
                    navigateBack = { parentFragmentManager.popBackStack() },
                    restoreTrack = { viewModel.restore(it) }
                )
            }
        }
    }
}
