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

package fr.nihilus.music.media

import com.google.common.truth.Truth.assertThat
import fr.nihilus.music.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.media.MediaId.Builder.TYPE_ROOT
import fr.nihilus.music.media.MediaId.Builder.TYPE_TRACKS
import org.junit.Test

class MediaIdTest {

    @Test
    fun whenParsingTypeOnly_thenMediaIdOnlyHasType() {
        val mediaId = MediaId.parse(TYPE_ROOT)

        assertThat(mediaId.type).isEqualTo(TYPE_ROOT)
        assertThat(mediaId.category).isNull()
        assertThat(mediaId.track).isNull()
    }

    @Test
    fun whenParsingTypeAndCategory_thenMediaIdHasTypeAndCategory() {
        val mediaId = MediaId.parse("$TYPE_TRACKS/$CATEGORY_ALL")

        assertThat(mediaId.type).isEqualTo(TYPE_TRACKS)
        assertThat(mediaId.category).isEqualTo(CATEGORY_ALL)
        assertThat(mediaId.track).isNull()
    }

    @Test
    fun whenParsingFullMediaId_thenMediaIdHasTypeAndCategoryAndTrack() {
        val mediaId = MediaId.parse("$TYPE_TRACKS/$CATEGORY_ALL|42")

        assertThat(mediaId.type).isEqualTo(TYPE_TRACKS)
        assertThat(mediaId.category).isEqualTo(CATEGORY_ALL)
        assertThat(mediaId.track).isEqualTo(42L)
    }

    @Test
    fun whenParsing_encodedMediaIdShouldBeSameAsInput() {
        assertParsedIsSameAsEncoded("type")
        assertParsedIsSameAsEncoded("type/category")
        assertParsedIsSameAsEncoded("type/category|42")
    }

    private fun assertParsedIsSameAsEncoded(encoded: String) {
        val mediaId = MediaId.parse(encoded)
        assertThat(mediaId.encoded).isEqualTo(encoded)
    }

    @Test(expected = IllegalArgumentException::class)
    fun whenParsingNull_thenFailWithIllegalArgumentException() {
        MediaId.parse(null)
    }

    @Test
    fun whenParsingInvalidStrings_thenFailWithInvalidMediaException() {
        assertParsingFails("")
        assertParsingFails("12e86f5")
        assertParsingFails("type/")
        assertParsingFails("/category")
        assertParsingFails("type/my category!")
        assertParsingFails("type/category|")
        assertParsingFails("type/|42")
        assertParsingFails("type/category|ae539f7")
        assertParsingFails("type/category1/category2")
        assertParsingFails("type/category/42")
        assertParsingFails("type/category|42|16")
    }

    @Test
    fun whenEncodingTypeOnly_thenReturnsTypeOnly() {
        val encoded = MediaId.encode(TYPE_ROOT)
        assertThat(encoded).isEqualTo(TYPE_ROOT)
    }

    @Test
    fun whenEncodingTypeAndCategory_thenReturnsTypeAndCategorySeparatedBySlash() {
        val encoded = MediaId.encode("type", "category")
        assertThat(encoded).isEqualTo("type/category")
    }

    @Test
    fun whenEncodingTypeCategoryAndTrack_thenReturnsWellFormedMediaId() {
        val encoded = MediaId.encode("type", "category", 42)
        assertThat(encoded).isEqualTo("type/category|42")
    }

    @Test
    fun givenSameParts_whenEncodingAndBuildingFromParts_thenProduceSameEncodedId() {
        val encoded = MediaId.encode("type", "category", 42)
        val mediaId = MediaId.fromParts("type", "category", 42)

        assertThat(mediaId.encoded).isEqualTo(encoded)
    }

    @Test
    fun whenEncodingWithTrackIdButNoCategory_thenFailWithInvalidMediaException() {
        assertThrows<InvalidMediaException> {
            MediaId.encode("type", null, 42)
        }
    }

    private fun assertParsingFails(encoded: String) {
        assertThrows<InvalidMediaException> {
            MediaId.parse(encoded)
        }
    }
}