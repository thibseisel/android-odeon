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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import fr.nihilus.music.core.database.spotify.TrackFeature
import fr.nihilus.music.devmenu.R
import fr.nihilus.music.media.provider.Track
import kotlinx.android.synthetic.main.dev_dialog_feature_stats.*
import java.text.NumberFormat

/**
 * A floating dialog displaying statistics on selected tracks.
 */
internal class FeatureStatsDialog : AppCompatDialogFragment() {
    private val viewModel by activityViewModels<ComposerViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dev_dialog_feature_stats, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.tracks.observe(viewLifecycleOwner) { featuredTracks ->
            updateFeatureStats(featuredTracks)
        }
    }

    private fun updateFeatureStats(featuredTracks: List<Pair<Track, TrackFeature>>) {
        var minTempo = Float.MIN_VALUE
        var minEnergy = Float.MIN_VALUE
        var minDanceability = Float.MIN_VALUE
        var minValence = Float.MIN_VALUE

        var maxTempo = Float.MAX_VALUE
        var maxEnergy = Float.MAX_VALUE
        var maxDanceability = Float.MAX_VALUE
        var maxValence = Float.MAX_VALUE

        var tempoSum = 0f
        var energySum = 0f
        var danceabilitySum = 0f
        var valenceSum = 0f

        for ((_, feature) in featuredTracks) {
            tempoSum += feature.tempo
            energySum += feature.energy
            danceabilitySum += feature.danceability
            valenceSum += feature.valence

            minTempo = minOf(minTempo, feature.tempo)
            maxTempo = maxOf(maxTempo, feature.tempo)

            minEnergy = minOf(minEnergy, feature.energy)
            maxEnergy = maxOf(maxEnergy, feature.energy)

            minDanceability = minOf(minDanceability, feature.danceability)
            maxDanceability = maxOf(maxDanceability, feature.danceability)

            minValence = minOf(minValence, feature.valence)
            maxValence = maxOf(maxValence, feature.valence)
        }

        val numberOfTracks = featuredTracks.size
        val percentFormatter = NumberFormat.getPercentInstance().apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 2
        }

        val decimalFormatter = NumberFormat.getNumberInstance().apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }

        min_tempo.text = decimalFormatter.format(minTempo)
        avg_tempo.text = decimalFormatter.format(tempoSum / numberOfTracks)
        max_tempo.text = decimalFormatter.format(maxTempo)

        min_energy.text = percentFormatter.format(minEnergy)
        avg_energy.text = percentFormatter.format(energySum / numberOfTracks)
        max_energy.text = percentFormatter.format(maxEnergy)

        min_danceability.text = percentFormatter.format(maxDanceability)
        avg_danceability.text = percentFormatter.format(danceabilitySum / numberOfTracks)
        max_danceability.text = percentFormatter.format(maxDanceability)

        min_valence.text = percentFormatter.format(maxValence)
        avg_valence.text = percentFormatter.format(valenceSum / numberOfTracks)
        max_valence.text = percentFormatter.format(maxValence)
    }
}