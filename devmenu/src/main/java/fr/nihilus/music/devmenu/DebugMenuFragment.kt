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

package fr.nihilus.music.devmenu

import android.os.Bundle
import android.view.View
import androidx.navigation.fragment.findNavController
import fr.nihilus.music.core.ui.base.BaseFragment
import kotlinx.android.synthetic.main.dev_fragment_menu.*

/**
 * Main menu for accessing debug features.
 */
internal class DebugMenuFragment : BaseFragment(R.layout.dev_fragment_menu) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Rows will use the id of the navigation action as row id.
        val menuEntries = listOf(
            StaticAdapter.Row(R.id.show_mix_composer, R.string.dev_filter_by_features)
        )

        recycler_list.setHasFixedSize(true)
        recycler_list.adapter = StaticAdapter(menuEntries) { selectedRow ->
            findNavController().navigate(selectedRow.id)
        }
    }
}