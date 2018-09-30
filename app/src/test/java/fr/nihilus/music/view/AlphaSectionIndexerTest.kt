/*
 * Copyright 2018 Thibault Seisel
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

package fr.nihilus.music.view

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.After
import org.junit.Before
import org.junit.Test

class AlphaSectionIndexerTest {

    private val items = mutableListOf<String>()
    private lateinit var indexer: AlphaSectionIndexer

    @Before
    fun setUp() {
        indexer = AlphaSectionIndexer(items)
    }

    @After
    fun tearDown() {
        items.clear()
    }

    @Test
    fun initially_itShouldHaveNoSections() {
        assertThat(indexer.sections).isEmpty()
    }

    @Test
    fun withNoItems_itShouldHaveNoSections() {
        // When notified that the item set has changed
        indexer.onChanged()

        // If there are no items
        // It should have no sections.
        assertThat(indexer.sections).isEmpty()
    }

    @Test
    fun withSimpleItemSet_itShouldHaveOneSectionPerLetter() {
        // Given a set of items each starting with a different letter
        items.addAll(oneSectionPerItem)
        indexer.onChanged()

        // There should be as many sections as items
        val sections = indexer.sections
        assertThat(sections).hasLength(6)

        // Sections should be generated from the first letter of each item
        assertThat(sections).asList().containsExactly("A", "B", "C", "G", "H", "S").inOrder()
    }

    @Test
    fun whenSameFirstLetter_itShouldHaveOneSectionPerLetter() {
        // Given items where some share the same first letter
        items.addAll(sameFirstLetter)
        indexer.onChanged()

        // There should be one section per letter
        val sections = indexer.sections
        assertThat(sections).hasLength(5)

        // Sections should not feature duplicates
        assertThat(sections).asList().containsExactly("A", "B", "C", "H", "S").inOrder()
    }

    @Test
    fun whenNotStartingByAZ_itShouldFallInFirstSection() {
        items.addAll(itemsWithLeadingNonLetterCharacter)
        indexer.onChanged()

        // The first section should be dedicated to non-letter items.
        val sections = indexer.sections
        assertThat(sections).hasLength(3)
        assertWithMessage("The first section should be the one dedicated to non-letter items")
            .that(sections.first()).isEqualTo("#")

        // Sections should be in order.
        assertThat(sections).asList().containsExactly("#", "A", "G").inOrder()
    }

    @Test
    fun shouldIgnoreCommonEnglishArticles() {
        items.addAll(itemsWithLeadingCommonArticles)
        indexer.onChanged()

        val sections = indexer.sections
        assertThat(sections).asList().containsExactly("#", "L", "S", "U").inOrder()
    }

    @Test
    fun whenInvalidated_itShouldHaveNoSections() {
        items.addAll(sameFirstLetter)
        indexer.onChanged()

        // When items are invalidated
        indexer.onInvalidated()
        assertThat(indexer.sections).isEmpty()
    }

    @Test
    fun withSimpleItems_eachHasItsOwnSection() {
        items.addAll(oneSectionPerItem)
        indexer.onChanged()

        // [0] "Another One Bites the Dust" -> Section [0] "A"
        assertThat(indexer.getSectionForPosition(0)).isEqualTo(0)
        // [1] "Black Betty" -> Section [1] "B"
        assertThat(indexer.getSectionForPosition(1)).isEqualTo(1)
        // [2] "Come As You Are" -> Section [2] "C"
        assertThat(indexer.getSectionForPosition(2)).isEqualTo(2)
        // [3] "Get Lucky" -> Section [3] "G"
        assertThat(indexer.getSectionForPosition(3)).isEqualTo(3)
        // [4] "Hysteria" -> Section [4] "H"
        assertThat(indexer.getSectionForPosition(4)).isEqualTo(4)
        // [5] "Supermassive Black Hole" -> Section [5] "S"
        assertThat(indexer.getSectionForPosition(5)).isEqualTo(5)
    }

    @Test
    fun withSameFirstLetterItems_fallInTheSameSection() {
        items.addAll(sameFirstLetter)
        indexer.onChanged()

        // [0] "Another One Bites the Dust" -> Section [0] "A"
        assertThat(indexer.getSectionForPosition(0)).isEqualTo(0)
        // [1] "Back in Black" -> Section [1] "B"
        assertThat(indexer.getSectionForPosition(1)).isEqualTo(1)
        // [2] "Black Betty" -> Section [1] "B"
        assertThat(indexer.getSectionForPosition(2)).isEqualTo(1)
        // [3] "Beat It" -> Section [1] "B"
        assertThat(indexer.getSectionForPosition(3)).isEqualTo(1)
        // [4] "Come As You Are" -> Section [2] "C"
        assertThat(indexer.getSectionForPosition(4)).isEqualTo(2)
        // [5] "Hell's Bells" -> Section [3] "H"
        assertThat(indexer.getSectionForPosition(5)).isEqualTo(3)
        // [6] "Hysteria" -> Section [3] "H"
        assertThat(indexer.getSectionForPosition(6)).isEqualTo(3)
        // [7] "Something Human" -> Section [4] "S"
        assertThat(indexer.getSectionForPosition(5)).isEqualTo(3)
        // [8] "Supermassive Black Hole" -> Section [4] "S"
        assertThat(indexer.getSectionForPosition(5)).isEqualTo(3)
    }
}

private val oneSectionPerItem = arrayOf(
    "Another One Bites the Dust",
    "Black Betty",
    "Come As You Are",
    "Get Lucky",
    "Hysteria",
    "Supermassive Black Hole"
)

private val sameFirstLetter = arrayOf(
    "Another One Bites the Dust",
    "Back in Black",
    "Black Betty",
    "Beat It",
    "Come As You Are",
    "Hell's Bells",
    "Hysteria",
    "Something Human",
    "Supermassive Black Hole"
)

private val itemsWithLeadingNonLetterCharacter = arrayOf(
    "[F]",
    "10 Years Today",
    "Another One Bites The Dust",
    "Get Lucky"
)

private val itemsWithLeadingCommonArticles = arrayOf(
    "The 2nd Law: Isolated System",
    "A Little Peace Of Heaven",
    "The Sky Is A Neighborhood",
    "Supermassive Black Hole",
    "An Unexpected Journey"
)