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

package fr.nihilus.music.spotify

import fr.nihilus.music.core.database.spotify.MusicalMode
import fr.nihilus.music.core.database.spotify.Pitch
import fr.nihilus.music.core.database.spotify.TrackFeature
import io.kotlintest.properties.Gen
import io.kotlintest.properties.forAll
import io.kotlintest.properties.forNone
import kotlin.random.Random
import kotlin.test.Test

class FeatureFilterTest {

    @Test
    fun `When applying a null tone filter then always match`() {
        val allTones = FeatureFilter.OnTone(null, null)

        forAll(FeatureGen) {
            allTones.matches(it)
        }
    }

    @Test
    fun `When applying a key filter then only match tracks with the same key`() {
        val singleKey = FeatureFilter.OnTone(Pitch.A, null)

        forAll(FeatureGen.filter { it.key == Pitch.A }) {
            singleKey.matches(it)
        }

        forNone(FeatureGen.filter { it.key != Pitch.A }) {
            singleKey.matches(it)
        }
    }

    @Test
    fun `When applying a mode filter then only match tracks with the same mode`() {
        val majorFilter = FeatureFilter.OnTone(null, MusicalMode.MAJOR)

        forAll(FeatureGen.filter { it.mode == MusicalMode.MAJOR }) {
            majorFilter.matches(it)
        }

        forNone(FeatureGen.filter { it.mode != MusicalMode.MAJOR }) {
            majorFilter.matches(it)
        }
    }

    @Test
    fun `When applying a tone filter then only match tracks with the same key and mode`() {
        val dMinorFilter = FeatureFilter.OnTone(Pitch.D, MusicalMode.MINOR)

        val dMinorTrackGen = FeatureGen.filter { it.key == Pitch.D && it.mode == MusicalMode.MINOR }
        forAll(dMinorTrackGen) {
            dMinorFilter.matches(it)
        }

        val otherTonesTrackGen = FeatureGen.filterNot { it.key == Pitch.D && it.mode == MusicalMode.MINOR }
        forNone(otherTonesTrackGen) {
            dMinorFilter.matches(it)
        }
    }

    @Test
    fun `When applying a filter on tempo, then only match tracks with tempo in range`() {
        val moderatoFilter = FeatureFilter.OnRange(TrackFeature::tempo, 88f, 112f)

        val moderatoTrackGen = FeatureGen.filter { it.tempo in 88f..112f }
        forAll(moderatoTrackGen) {
            moderatoFilter.matches(it)
        }

        val allegroTrackGen = FeatureGen.filter { it.tempo in 112f..160f }
        forNone(allegroTrackGen) {
            moderatoFilter.matches(it)
        }
    }
}

private object FeatureGen : Gen<TrackFeature> {
    private val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"

    override fun constants(): Iterable<TrackFeature> = emptyList()

    override fun random(): Sequence<TrackFeature> = generateSequence {
        TrackFeature(
            id = randomSpotifyId(),
            key = randomPitchKey(),
            mode = randomMusicalMode(),
            tempo = Random.nextDouble(0.0, 240.0).toFloat(),
            signature = 4,
            loudness = Random.nextDouble(-60.0, 0.0).toFloat(),
            acousticness = Random.nextDouble(0.0, 1.0).toFloat(),
            danceability = Random.nextDouble(0.0, 1.0).toFloat(),
            energy = Random.nextDouble(0.0, 1.0).toFloat(),
            instrumentalness = Random.nextDouble(0.0, 1.0).toFloat(),
            liveness = Random.nextDouble(0.0, 1.0).toFloat(),
            speechiness = Random.nextDouble(0.0, 1.0).toFloat(),
            valence = Random.nextDouble(0.0, 1.0).toFloat()
        )
    }

    private fun randomSpotifyId() = buildString(22) {
        repeat(22) {
            append(chars.random())
        }
    }

    private fun randomPitchKey(): Pitch? {
        val pitches = Pitch.values()

        // We want a null pitch with the same probability as the other values.
        // We introduce an extra ordinal which will be used for `null`.
        val ordinal = Random.nextInt(until = pitches.size + 1)
        return if (ordinal < pitches.size) pitches[ordinal] else null
    }

    private fun randomMusicalMode(): MusicalMode =
        if (Random.nextBoolean()) MusicalMode.MAJOR else MusicalMode.MINOR

}