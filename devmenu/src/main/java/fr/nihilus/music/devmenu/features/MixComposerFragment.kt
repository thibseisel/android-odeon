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

package fr.nihilus.music.devmenu.features

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import fr.nihilus.music.devmenu.R
import kotlinx.android.synthetic.main.fragment_mix_composer.*

internal class MixComposerFragment : Fragment(R.layout.fragment_mix_composer) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = FeatureSpecAdapter()
        val dividers = DividerItemDecoration(requireContext(), DividerItemDecoration.HORIZONTAL)
        feature_criteria.adapter = adapter
        feature_criteria.addItemDecoration(dividers)
    }
}