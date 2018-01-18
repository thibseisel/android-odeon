/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.service

import android.os.Bundle
import android.provider.MediaStore
import org.junit.Assert.*
import org.junit.Test

class SearchParamsTest {

    @Test
    fun whenNoQuery_isAnyIsTrue() {
        var search = SearchParams(null, null)
        assertTrue(search.isAny)

        search = SearchParams("", null)
        assertTrue(search.isAny)
    }

    @Test
    fun whenIsAny_anythingElseIsFalse() {
        val search = SearchParams(null, null)
        assertFalse(search.isUnstructured)
        assertFalse(search.isArtistFocus)
        assertFalse(search.isAlbumFocus)
        assertFalse(search.isSongFocus)
    }

    @Test
    fun whenHasQuery_isAnyIsFalse() {
        val search = SearchParams("a song", null)
        assertFalse(search.isAny)
    }

    @Test
    fun whenHasQueryButNoExtras_isUnstructured() {
        val search = SearchParams("a song", null)
        assertTrue(search.isUnstructured)
    }

    @Test
    fun whenHasArtistFocusExtra_isArtistFocused() {
        val search = SearchParams("Some artist", Bundle(2).apply {
            putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)
            putString(MediaStore.EXTRA_MEDIA_ARTIST, "Some artist")
        })

        assertTrue(search.isArtistFocus)
        assertEquals("Some artist", search.artist)
    }

    @Test
    fun whenHasAlbumFocusExtra_isAlbumFocused() {
        val search = SearchParams("Some album", Bundle(3).apply {
            putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE)
            putString(MediaStore.EXTRA_MEDIA_ALBUM, "Some album")
            putString(MediaStore.EXTRA_MEDIA_ARTIST, "Some artist")
        })

        assertTrue(search.isAlbumFocus)
        assertEquals("Some album", search.album)
        assertEquals("Some artist", search.artist)
    }

    @Test
    fun whenHasSongFocusExtra_isSongFocused() {
        val search = SearchParams("Some song", Bundle(4).apply {
            putString(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Media.ENTRY_CONTENT_TYPE)
            putString(MediaStore.EXTRA_MEDIA_TITLE, "Some song")
            putString(MediaStore.EXTRA_MEDIA_ALBUM, "Some album")
            putString(MediaStore.EXTRA_MEDIA_ARTIST, "Some artist")
        })

        assertTrue(search.isSongFocus)
        assertEquals("Some song", search.song)
        assertEquals("Some album", search.album)
        assertEquals("Some artist", search.artist)
    }

    @Test
    fun whenHasFocusExtra_shouldBeSpecified() {
        arrayOf(
            MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE,
            MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE,
            MediaStore.Audio.Media.ENTRY_CONTENT_TYPE
        ).map { mediaFocus ->
            SearchParams("Some query", Bundle(1).apply {
                putString(MediaStore.EXTRA_MEDIA_FOCUS, mediaFocus)
            })
        }.forEach { search ->
                assertFalse(search.isAny)
                assertFalse(search.isUnstructured)
            }
    }
}