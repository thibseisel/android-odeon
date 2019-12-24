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

package fr.nihilus.music.spotify.service

import io.kotlintest.shouldBe
import kotlin.test.Test

class SpotifyQueryTest {

    @Test
    fun `Given a simple track query, then encode as quoted lowercase`() {
        val singleWordQuery = SpotifyQuery.Track(title = "Algorithm")
        singleWordQuery.toString() shouldBe "\"algorithm\""

        val multiWordQuery = SpotifyQuery.Track(title = "Knights of Cydonia")
        multiWordQuery.toString() shouldBe """"knights of cydonia""""
    }

    @Test
    fun `Given a track query with artist, then encode with artist fragment`() {
        val query = SpotifyQuery.Track(title = "Algorithm", artist = "Muse")
        query.toString() shouldBe """"algorithm" artist:"muse""""
    }

    @Test
    fun `Given a track query with album, then encode with album fragment`() {
        val query = SpotifyQuery.Track(title = "Algorithm", album = "Simulation Theory")
        query.toString() shouldBe """"algorithm" album:"simulation theory""""
    }

    @Test
    fun `Given a complete track query, then encode both artist and album`() {
        val query = SpotifyQuery.Track("Algorithm", "Muse", "Simulation Theory")
        query.toString() shouldBe """"algorithm" artist:"muse" album:"simulation theory""""
    }

    @Test
    fun `Given an artist query, then encode as quoted lowercase`() {
        val singleWordQuery = SpotifyQuery.Artist(name = "Muse")
        singleWordQuery.toString() shouldBe "\"muse\""

        val multiWordQuery = SpotifyQuery.Artist(name = "Guns N' Roses")
        multiWordQuery.toString() shouldBe """"guns n' roses""""
    }

    @Test
    fun `Given a simple album query, then encode as quoted lowercase`() {
        val singleWordQuery = SpotifyQuery.Album(title = "Drones")
        singleWordQuery.toString() shouldBe "\"drones\""

        val multiWordQuery = SpotifyQuery.Album(title = "Simulation Theory")
        multiWordQuery.toString() shouldBe "\"simulation theory\""
    }

    @Test
    fun `Given an album query with artist, then encode as quoted with artist fragment`() {
        val query = SpotifyQuery.Album(title = "Simulation Theory", artist = "Muse")
        query.toString() shouldBe """"simulation theory" artist:"muse""""
    }
}