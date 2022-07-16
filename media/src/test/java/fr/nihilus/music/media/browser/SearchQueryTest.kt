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

package fr.nihilus.music.media.browser

import android.os.Bundle
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class SearchQueryTest {

    @Test
    fun `When query is null, then parse as an Empty query`() {
        val result = SearchQuery.from(null, null)
        result.shouldBeTypeOf<SearchQuery.Empty>()
    }

    @Test
    fun `When query is blank, then parse as an Empty query`() {
        val result = SearchQuery.from("   ", null)
        result.shouldBeTypeOf<SearchQuery.Empty>()
    }

    @Test
    fun `When query has no options, then parse it as an Unspecified query`() {
        val result = SearchQuery.from("muse", null)
        result.shouldBeTypeOf<SearchQuery.Unspecified>()
        result.userQuery shouldBe "muse"
    }

    @Test
    fun `When query has artist focus, then parse it as an Artist query`() {
        val result = SearchQuery.from("muse", artistOptions("Muse"))
        result.shouldBeTypeOf<SearchQuery.Artist>()
        result.name shouldBe "Muse"
    }

    @Test
    fun `When query has album focus, then parse it as an Album query`() {
        val result = SearchQuery.from(
            "simulation theory",
            albumOptions("Muse", "Simulation Theory")
        )

        result.shouldBeTypeOf<SearchQuery.Album>()
        assertSoftly(result) {
            it.artist shouldBe "Muse"
            it.title shouldBe "Simulation Theory"
        }
    }

    @Test
    fun `When query has track focus, then parse it as a Track query`() {
        val result = SearchQuery.from(
            "algorithm",
            trackOptions("Muse", "Simulation Theory", "Algorithm")
        )

        result.shouldBeTypeOf<SearchQuery.Song>()
        assertSoftly(result) {
            it.artist shouldBe "Muse"
            it.album shouldBe "Simulation Theory"
            it.title shouldBe "Algorithm"
        }
    }

    @Test
    fun `When query has unsupported focus, then parse it as Unspecified query`() {
        val options = Bundle(2).apply {
            putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE)
            putString(MediaStore.EXTRA_MEDIA_GENRE, "Rock")
        }

        val result = SearchQuery.from("rock", options)
        result.shouldBeTypeOf<SearchQuery.Unspecified>()
        result.userQuery shouldBe "rock"
    }

    @Suppress("SameParameterValue")
    private fun artistOptions(name: String?) = Bundle(2).apply {
        putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)
        putString(MediaStore.EXTRA_MEDIA_ARTIST, name)
    }

    @Suppress("SameParameterValue")
    private fun albumOptions(artist: String?, title: String?) = Bundle(3).apply {
        putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE)
        putString(MediaStore.EXTRA_MEDIA_ARTIST, artist)
        putString(MediaStore.EXTRA_MEDIA_ALBUM, title)
    }

    @Suppress("SameParameterValue")
    private fun trackOptions(artist: String?, album: String?, title: String?) = Bundle(4).apply {
        putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Media.ENTRY_CONTENT_TYPE)
        putString(MediaStore.EXTRA_MEDIA_ARTIST, artist)
        putString(MediaStore.EXTRA_MEDIA_ALBUM, album)
        putString(MediaStore.EXTRA_MEDIA_TITLE, title)
    }
}
