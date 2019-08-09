/*
 * Copyright 2019 Thibault Seisel
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

package fr.nihilus.music.library.cleanup

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseFragment
import fr.nihilus.music.ui.LoadRequest
import kotlinx.android.synthetic.main.fragment_cleanup.*

class CleanupFragment : BaseFragment(R.layout.fragment_cleanup) {
    private val viewModel by viewModels<CleanupViewModel> { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = CleanupAdapter()
        disposable_track_list.adapter = adapter
        disposable_track_list.setHasFixedSize(true)

        viewModel.tracks.observe(this) { disposableTracksRequest ->
            when (disposableTracksRequest) {
                is LoadRequest.Success -> {
                    val checkableItems = disposableTracksRequest.data.map { Checkable(it, false) }
                    adapter.submitList(checkableItems)
                }
            }
        }

        action_delete_selected.setOnClickListener {
            viewModel.deleteTracks(adapter.getSelectedItems())
        }
    }
}