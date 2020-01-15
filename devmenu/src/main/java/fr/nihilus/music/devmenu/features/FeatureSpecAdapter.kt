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

import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.core.ui.extensions.inflate
import fr.nihilus.music.devmenu.R

internal class FeatureSpecAdapter(
    private val viewModel: ComposerViewModel
) : ListAdapter<FeatureFilterState, FeatureSpecAdapter.RangeInputHolder>(Differ()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = RangeInputHolder(parent)

    override fun onBindViewHolder(holder: RangeInputHolder, position: Int) {
        val featureFilter = getItem(position)
        holder.bind(featureFilter)
    }

    private fun updateFilter(feature: Feature, lowerText: String?, upperText: String?) {
        val minValue = lowerText?.toFloatOrNull()
        val maxValue = upperText?.toFloatOrNull()

        if (minValue != null && maxValue != null) {
            val filterSpec = FeatureFilterState(feature, minValue, maxValue)
            viewModel.setFilter(filterSpec)
        }
    }

    inner class RangeInputHolder(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.dev_range_featurespec)) {
        private val featureLabel: TextView = itemView.findViewById(R.id.label)
        private val lowerInput: EditText = itemView.findViewById(R.id.input_lower)
        private val upperInput: EditText = itemView.findViewById(R.id.input_upper)
        private val removeButton: ImageView = itemView.findViewById(R.id.action_remove)

        init {
            val rangeChangedListener = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    updateFilter(
                        getItem(adapterPosition).feature,
                        lowerInput.text?.toString(),
                        upperInput.text?.toString()
                    )
                }
            }

            lowerInput.addTextChangedListener(rangeChangedListener)
            upperInput.addTextChangedListener(rangeChangedListener)

            removeButton.setOnClickListener {
                val featureFilter = getItem(adapterPosition)
                viewModel.removeFilter(featureFilter)
            }
        }

        fun bind(filter: FeatureFilterState) {
            featureLabel.text = featureLabel.context.getString(filter.feature.labelResId)

            lowerInput.setText(filter.minValue.toString())
            upperInput.setText(filter.maxValue.toString())
        }
    }

    private class Differ : DiffUtil.ItemCallback<FeatureFilterState>() {

        override fun areItemsTheSame(
            oldItem: FeatureFilterState,
            newItem: FeatureFilterState
        ): Boolean = oldItem.feature == newItem.feature

        override fun areContentsTheSame(
            oldItem: FeatureFilterState,
            newItem: FeatureFilterState
        ): Boolean = oldItem.minValue == newItem.minValue && oldItem.maxValue == newItem.maxValue
    }
}