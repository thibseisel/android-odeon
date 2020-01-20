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

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.nihilus.music.devmenu.R

internal class AddFilterDialog : AppCompatDialogFragment() {
    private val viewModel by activityViewModels<ComposerViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val allFeatures = Feature.values()
        val featureEntries = Array(allFeatures.size) { allFeatures[it].name }

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dev_title_new_filter)
            .setItems(featureEntries) { _, position ->
                val pickedFeature = Feature.values()[position]
                val featureFilter = FeatureFilterState(pickedFeature, pickedFeature.minValue, pickedFeature.maxValue)
                viewModel.setFilter(featureFilter)
            }
            .setNegativeButton(R.string.core_cancel, null)
            .create()
    }
}