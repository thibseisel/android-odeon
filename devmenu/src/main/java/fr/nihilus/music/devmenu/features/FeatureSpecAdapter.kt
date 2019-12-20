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

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.core.ui.extensions.inflate
import fr.nihilus.music.devmenu.R
import fr.nihilus.music.devmenu.RangeSlider

internal class AudioFeatureSpec(
    val featureNameId: Int,
    val minValue: Float,
    val maxValue: Float
) {
    var lowerBound: Float = minValue
    var upperBound: Float = maxValue
}

internal class FeatureSpecAdapter : RecyclerView.Adapter<FeatureSpecAdapter.ViewHolder>() {
    private val featureSpecs = listOf(
        AudioFeatureSpec(R.string.dev_label_tempo, 0f, 240f),
        AudioFeatureSpec(R.string.dev_label_loudness, 0f, 1f),
        AudioFeatureSpec(R.string.dev_acousticness, 0f, 1f),
        AudioFeatureSpec(R.string.dev_label_energy, 0f, 1f),
        AudioFeatureSpec(R.string.dev_label_danceability, 0f, 1f),
        AudioFeatureSpec(R.string.dev_label_instrumentalness, 0f, 1f),
        AudioFeatureSpec(R.string.dev_label_valence, 0f, 1f),
        AudioFeatureSpec(R.string.dev_label_liveness, 0f, 1f)
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(parent)

    override fun getItemCount(): Int = featureSpecs.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(featureSpecs[position])
    }

    inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.dev_feature_row)) {
        private val featureLabel: TextView = itemView.findViewById(R.id.label)
        private val slider: RangeSlider = itemView.findViewById(R.id.slider)
        private val lowerLabel: TextView = itemView.findViewById(R.id.lower_label)
        private val upperLabel: TextView = itemView.findViewById(R.id.upper_label)

        init {
            slider.setOnRangeChangedListener(object : RangeSlider.OnRangeChangedListener {

                override fun onRangeChanged(slider: RangeSlider, lower: Float, upper: Float) {
                    lowerLabel.text = lower.toString()
                    upperLabel.text = upper.toString()
                }

                override fun onStartTrackingTouch(slider: RangeSlider) {}

                override fun onStopTrackingTouch(slider: RangeSlider) {
                    val feature = featureSpecs[adapterPosition]
                    feature.lowerBound = slider.lowerBound
                    feature.upperBound = slider.upperBound
                }
            })
        }

        fun bind(feature: AudioFeatureSpec) {
            featureLabel.text = featureLabel.context.getString(feature.featureNameId)

            slider.minValue = feature.minValue
            slider.maxValue = feature.maxValue
            slider.lowerBound = feature.lowerBound
            slider.upperBound = feature.upperBound

            lowerLabel.text = feature.lowerBound.toString()
            upperLabel.text = feature.upperBound.toString()
        }
    }
}