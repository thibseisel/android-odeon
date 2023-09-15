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

package fr.nihilus.music.core.media

import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ROOT
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class MediaIdTest {

    @Test
    fun whenParsingTypeOnly_thenMediaIdOnlyHasType() {
        val mediaId = MediaId.parse(TYPE_ROOT)

        assertSoftly(mediaId) {
            type shouldBe TYPE_ROOT
            category.shouldBeNull()
            track.shouldBeNull()
        }
    }

    @Test
    fun whenParsingTypeAndCategory_thenMediaIdHasTypeAndCategory() {
        val mediaId = MediaId.parse("$TYPE_TRACKS/$CATEGORY_ALL")

        assertSoftly(mediaId) {
            type shouldBe TYPE_TRACKS
            category shouldBe CATEGORY_ALL
            track.shouldBeNull()
        }
    }

    @Test
    fun whenParsingFullMediaId_thenMediaIdHasTypeAndCategoryAndTrack() {
        val mediaId = MediaId.parse("$TYPE_TRACKS/$CATEGORY_ALL|42")

        mediaId.type shouldBe TYPE_TRACKS
        mediaId.category shouldBe CATEGORY_ALL
        mediaId.track shouldBe 42L
    }

    @Test
    fun whenParsing_encodedMediaIdShouldBeSameAsInput() {
        assertParsedIsSameAsEncoded("type")
        assertParsedIsSameAsEncoded("type/category")
        assertParsedIsSameAsEncoded("type/category|42")
    }

    private fun assertParsedIsSameAsEncoded(encoded: String) {
        val mediaId = MediaId.parse(encoded)
        mediaId.encoded shouldBe encoded
    }

    @Test
    fun whenParsingInvalidStrings_thenFailWithMalformedMediaIdException() {
        assertParsingFails(null)
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
        encoded shouldBe TYPE_ROOT
    }

    @Test
    fun whenEncodingTypeAndCategory_thenReturnsTypeAndCategorySeparatedBySlash() {
        val encoded = MediaId.encode("type", "category")
        encoded shouldBe "type/category"
    }

    @Test
    fun whenEncodingTypeCategoryAndTrack_thenReturnsWellFormedMediaId() {
        val encoded = MediaId.encode("type", "category", 42)
        encoded shouldBe "type/category|42"
    }

    @Test
    fun givenSameParts_whenEncodingAndBuildingFromParts_thenProduceSameEncodedId() {
        val encoded = MediaId.encode("type", "category", 42)
        val mediaId = MediaId.fromParts("type", "category", 42)

        mediaId.encoded shouldBe encoded
    }

    @Test
    fun whenEncodingWithTrackIdButNoCategory_thenFailWithMalformedMediaIdException() {
        shouldThrow<MalformedMediaIdException> {
            MediaId.encode("type", null, 42)
        }
    }

    private fun assertParsingFails(encoded: String?) {
        shouldThrow<MalformedMediaIdException> {
            MediaId.parse(encoded)
        }
    }
}