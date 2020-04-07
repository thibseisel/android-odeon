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

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.devmenu.R
import kotlinx.android.synthetic.main.fragment_mix_composer.*

internal class MixComposerFragment : BaseFragment(R.layout.fragment_mix_composer) {
    private val viewModel by activityViewModels<ComposerViewModel> { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = FeatureSpecAdapter(viewModel)
        feature_criteria.adapter = adapter

        search_button.setOnClickListener {
            val toMatchingTracks = MixComposerFragmentDirections.showMatchingTracks()
            findNavController().navigate(toMatchingTracks)
        }

        viewModel.tracks.observe(viewLifecycleOwner) { tracks ->
            label_track_count.text = getString(R.string.dev_matching_tracks_count, tracks.size)
        }

        viewModel.filters.observe(viewLifecycleOwner) { filters ->
            adapter.submitList(filters)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_composer, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_add_filter -> {
            val toNewFilterDialog = MixComposerFragmentDirections.addNewFilter()
            findNavController().navigate(toNewFilterDialog)
            true
        }

        else -> super.onOptionsItemSelected(item)
    }
}