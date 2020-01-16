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
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import fr.nihilus.music.core.database.spotify.MusicalMode
import fr.nihilus.music.core.database.spotify.Pitch
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.extensions.inflate
import fr.nihilus.music.devmenu.R
import fr.nihilus.music.spotify.manager.FeaturedTrack
import kotlinx.android.synthetic.main.fragment_featured_tracks.*
import java.text.NumberFormat

internal class FeaturedTracksFragment : BaseFragment(R.layout.fragment_featured_tracks) {
    private val viewModel by activityViewModels<ComposerViewModel>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = FeaturedTrackAdapter()
        track_list.adapter = adapter

        viewModel.tracks.observe(this) { tracks ->
            adapter.submitList(tracks.sortedBy { it.track.title })
        }
    }
}

internal class FeaturedTrackAdapter : ListAdapter<FeaturedTrack, FeaturedTrackAdapter.Holder>(Differ()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder = Holder(parent)

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val track = getItem(position)
        holder.bind(track)
    }

    internal class Holder(parent: ViewGroup) : RecyclerView.ViewHolder(parent.inflate(R.layout.dev_featured_track_row)) {
        private val title: TextView = itemView.findViewById(R.id.track_title)
        private val remoteId: TextView = itemView.findViewById(R.id.remote_track_id)
        private val tone: TextView = itemView.findViewById(R.id.tone_indicator)

        private val tempo: TextView = itemView.findViewById(R.id.tempo_cartridge)
        private val signature: TextView = itemView.findViewById(R.id.signature_cartridge)
        private val loudness: TextView = itemView.findViewById(R.id.loudness_cartridge)
        private val energy: TextView = itemView.findViewById(R.id.value_energy)
        private val valence: TextView = itemView.findViewById(R.id.value_valence)
        private val danceability: TextView = itemView.findViewById(R.id.value_danceability)
        private val acousticness: TextView = itemView.findViewById(R.id.value_acousticness)
        private val instrumentalness: TextView = itemView.findViewById(R.id.value_instrumentalness)
        private val liveness: TextView = itemView.findViewById(R.id.value_liveness)
        private val speechiness: TextView = itemView.findViewById(R.id.value_speechiness)

        private val percentFormatter = NumberFormat.getPercentInstance().apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 2
        }

        fun bind(track: FeaturedTrack) {
            val context = itemView.context

            title.text = track.track.title

            val features = track.features
            remoteId.text = track.features.id

            tone.text = toneString(features.key, features.mode)
            tempo.text = context.getString(R.string.dev_format_tempo, features.tempo)
            signature.text = context.getString(R.string.dev_format_signature, features.signature)
            loudness.text = context.getString(R.string.dev_format_decibels, features.loudness)
            energy.text = percentFormatter.format(features.energy)
            valence.text = percentFormatter.format(features.valence)
            danceability.text = percentFormatter.format(features.danceability)
            acousticness.text = percentFormatter.format(features.acousticness)
            instrumentalness.text = percentFormatter.format(features.instrumentalness)
            liveness.text = percentFormatter.format(features.liveness)
            speechiness.text = percentFormatter.format(features.speechiness)
        }

        private fun toneString(key: Pitch?, mode: MusicalMode): String {
            val keys = itemView.resources.getStringArray(R.array.dev_key_entries)
            return buildString {
                append(key?.ordinal?.let { keys[it] } ?: "?")
                append(when (mode) {
                    MusicalMode.MINOR -> 'm'
                    MusicalMode.MAJOR -> 'M'
                })
            }
        }
    }

    private class Differ : DiffUtil.ItemCallback<FeaturedTrack>() {

        override fun areItemsTheSame(
            oldItem: FeaturedTrack,
            newItem: FeaturedTrack
        ): Boolean = oldItem.track.id == newItem.track.id

        override fun areContentsTheSame(
            oldItem: FeaturedTrack,
            newItem: FeaturedTrack
        ): Boolean = oldItem.features == newItem.features
    }
}