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

import fr.nihilus.music.devmenu.R

internal enum class Feature(
    val labelResId: Int,
    val minValue: Float,
    val maxValue: Float
) {
    TEMPO(R.string.dev_label_tempo, 0f, 500f),
    LOUDNESS(R.string.dev_label_loudness, -60f, 0f),
    ENERGY(R.string.dev_label_energy, 0f, 1f),
    DANCEABILITY(R.string.dev_label_danceability, 0f, 1f),
    INSTRUMENTALNESS(R.string.dev_label_instrumentalness, 0f, 1f),
    VALENCE(R.string.dev_label_valence, 0f, 1f)
}