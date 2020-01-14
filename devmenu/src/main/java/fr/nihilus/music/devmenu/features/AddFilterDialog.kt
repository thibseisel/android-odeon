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
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.viewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import fr.nihilus.music.core.ui.base.BaseDialogFragment
import fr.nihilus.music.core.ui.base.ListAdapter
import fr.nihilus.music.devmenu.R

internal class AddFilterDialog : BaseDialogFragment() {
    private val viewModel by viewModels<ComposerViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val adapter = FeatureAdapter()
        adapter.submitList(Feature.values().toList())

        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.dev_title_new_filter))
            .setAdapter(adapter) { _, position ->
                val pickedFeature = Feature.values()[position]
                val featureFilter = FeatureFilterState(pickedFeature, pickedFeature.minValue, pickedFeature.maxValue)
                viewModel.setFilter(featureFilter)
            }
            .setNegativeButton(R.string.core_cancel, null)
            .create()
    }

    private class FeatureAdapter : ListAdapter<Feature, FeatureAdapter.Holder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(parent)

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val feature = Feature.values()[position]
            holder.label.setText(feature.labelResId)
        }

        class Holder(parent: ViewGroup) : ViewHolder(parent, android.R.layout.simple_list_item_1) {
            val label: TextView = itemView.findViewById(android.R.id.text1)
        }
    }
}