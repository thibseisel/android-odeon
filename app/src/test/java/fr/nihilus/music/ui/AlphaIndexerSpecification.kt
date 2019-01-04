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

package fr.nihilus.music.ui

import com.google.common.collect.BoundType
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.truth.TruthJUnit.assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.robolectric.RobolectricTestRunner

@RunWith(Suite::class)
@Suite.SuiteClasses(
    WithNoItem::class,
    WithOneItemPerSection::class,
    WithSameFirstLetter::class,
    WithNonLetterItems::class,
    WithDiacriticsItems::class,
    WithLeadingCommonEnglishPrefixes::class,
    WithUnexpectedItems::class,
    WithLotsOfItemsPerSection::class
)
class AlphaIndexerSpecification

@RunWith(RobolectricTestRunner::class)
class WithNoItem {

    private lateinit var indexer: AlphaSectionIndexer

    @Before
    fun whenInitialized() {
        indexer = AlphaSectionIndexer()
    }

    @Test
    fun itShouldHaveNoSections() {
        assertThat(indexer.sections).isEmpty()
    }

    @Test
    fun itShouldAlwaysReturnSectionZero() {
        assertThat(indexer.getSectionForPosition(0)).isEqualTo(0)
        assertThat(indexer.getSectionForPosition(2)).isEqualTo(0)
        assertThat(indexer.getSectionForPosition(5)).isEqualTo(0)
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun itShouldFailWhenRequestingAnyPositionForSection() {
        indexer.getPositionForSection(0)
    }
}

@RunWith(RobolectricTestRunner::class)
class WithOneItemPerSection : ItemBasedIndexerScenario() {
    override val items = listOf(
        "Another One Bites the Dust",
        "Black Betty",
        "Come As You Are",
        "Get Lucky",
        "Hysteria",
        "Supermassive Black Hole"
    )

    @Test
    fun itShouldHaveOneSectionPerItem() {
        assertThat(indexer.sections).asList().containsExactly("A", "B", "C", "G", "H", "S").inOrder()
    }

    @Test
    fun itShouldMapPositionToCorrespondingSection() {
        assume().that(indexer.sections).asList().containsExactly("A", "B", "C", "G", "H", "S").inOrder()

        // [0] "Another One Bites the Dust" -> Section [0] "A"
        assertItemInSection(0, 0)
        // [1] "Black Betty" -> Section [1] "B"
        assertItemInSection(1, 1)
        // [2] "Come As You Are" -> Section [2] "C"
        assertItemInSection(2, 2)
        // [3] "Get Lucky" -> Section [3] "G"
        assertItemInSection(3, 3)
        // [4] "Hysteria" -> Section [4] "H"
        assertItemInSection(4, 4)
        // [5] "Supermassive Black Hole" -> Section [5] "S"
        assertItemInSection(5, 5)
    }

    @Test
    fun itShouldReturnSectionForPosition() {
        assume().that(indexer.sections).asList().containsExactly("A", "B", "C", "G", "H", "S").inOrder()

        assertSectionStartsAtPosition(0, 0)
        assertSectionStartsAtPosition(1, 1)
        assertSectionStartsAtPosition(2, 2)
        assertSectionStartsAtPosition(3, 3)
        assertSectionStartsAtPosition(4, 4)
        assertSectionStartsAtPosition(5, 5)
    }
}

@RunWith(RobolectricTestRunner::class)
class WithSameFirstLetter : ItemBasedIndexerScenario() {
    override val items = listOf(
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

    @Test
    fun itShouldNotHaveDuplicateSections() {
        assertThat(indexer.sections).asList().containsExactly("A", "B", "C", "H", "S").inOrder()
    }

    @Test
    fun itShouldGroupItemsStartingWithSameLetterUnderSameSection() {
        assume().that(indexer.sections).asList().containsExactly("A", "B", "C", "H", "S").inOrder()

        // [0] "Another One Bites the Dust" -> Section [0] "A"
        assertItemInSection(0, 0)
        // [1] "Back in Black" -> Section [1] "B"
        assertItemInSection(1, 1)
        // [2] "Black Betty" -> Section [1] "B"
        assertItemInSection(2, 1)
        // [3] "Beat It" -> Section [1] "B"
        assertItemInSection(3, 1)
        // [4] "Come As You Are" -> Section [2] "C"
        assertItemInSection(4, 2)
        // [5] "Hell's Bells" -> Section [3] "H"
        assertItemInSection(5, 3)
        // [6] "Hysteria" -> Section [3] "H"
        assertItemInSection(6, 3)
        // [7] "Something Human" -> Section [4] "S"
        assertItemInSection(5, 3)
        // [8] "Supermassive Black Hole" -> Section [4] "S"
        assertItemInSection(5, 3)
    }

    @Test
    fun itShouldReturnPositionOfFirstItemOfSection() {
        assume().that(indexer.sections).asList().containsExactly("A", "B", "C", "H", "S").inOrder()

        // Section [0] "A" starts at [0] "Another One Bites the Dust"
        assertSectionStartsAtPosition(0, 0)
        // Section [1] "B" starts at [1] "Back in Black"
        assertSectionStartsAtPosition(1, 1)
        // Section [2] "C" starts at [4] "Come As You Are"
        assertSectionStartsAtPosition(2, 4)
        // Section [3] "H" starts at [5] "Hells Bells"
        assertSectionStartsAtPosition(3, 5)
        // Section [4] "S" starts at [7] "Something Human"
        assertSectionStartsAtPosition(4, 7)
    }
}

@RunWith(RobolectricTestRunner::class)
class WithNonLetterItems : ItemBasedIndexerScenario() {
    override val items = listOf(
        "[F]",
        "10 Years Today",
        "Another One Bites The Dust",
        "Get Lucky"
    )

    @Test
    fun itShouldHaveLeadingSharpSection() {
        assertThat(indexer.sections).isNotEmpty()
        assertThat(indexer.sections.first()).isEqualTo("#")
    }

    @Test
    fun itShouldAlsoHaveLetterSectionsForOtherItems() {
        assertThat(indexer.sections).asList().containsExactly("#", "A", "G").inOrder()
    }

    @Test
    fun itShouldPutNonLetterItemsInSharpSection() {
        // [0] "[F]" -> Section [0] "#"
        assertItemInSection(0, 0)
        // [1] "10 Years Today" -> [0] "#"
        assertItemInSection(1, 0)
    }

    @Test
    fun itsSharpSectionShouldStartAtFirstItem() {
        assertSectionStartsAtPosition(0, 0)
    }
}

@RunWith(RobolectricTestRunner::class)
class WithDiacriticsItems : ItemBasedIndexerScenario() {
    override val items = listOf(
        "À la Claire Fontaine",
        "Ça (c'est vraiment toi)",
        "Californication",
        "Évier Métal"
    )

    @Test
    fun itShouldIgnoreDiacriticsInSectionNames() {
        assertThat(indexer.sections).asList().containsExactly("A", "C", "E").inOrder()
    }

    @Test
    fun itShouldPutDiacriticItemsInNonDiacriticSections() {
        assume().that(indexer.sections).asList().containsExactly("A", "C", "E").inOrder()
        assertItemInSection(0, 0)
        assertItemInSection(1, 1)
        assertItemInSection(2, 1)
        assertItemInSection(3, 2)
    }

    @Test
    fun itsSectionsMayStartByDiacriticItem() {
        assume().that(indexer.sections).asList().containsExactly("A", "C", "E").inOrder()
        assertSectionStartsAtPosition(0, 0)
        assertSectionStartsAtPosition(1, 1)
        assertSectionStartsAtPosition(2, 3)
    }
}

@RunWith(RobolectricTestRunner::class)
class WithLeadingCommonEnglishPrefixes : ItemBasedIndexerScenario() {
    override val items = listOf(
        "The 2nd Law: Isolated System",
        "A Little Peace Of Heaven",
        "The Sky Is A Neighborhood",
        "Supermassive Black Hole",
        "An Unexpected Journey"
    )

    @Test
    fun itShouldCreateSectionFomUnprefixedItems() {
        assertThat(indexer.sections).asList().containsExactly("#", "L", "S", "U")
    }

    @Test
    fun itShouldPutPrefixedItemsInUnprefixedSections() {
        assume().that(indexer.sections).asList().containsExactly("#", "L", "S", "U")
        assertItemInSection(0, 0)
        assertItemInSection(1, 1)
        assertItemInSection(2, 2)
        assertItemInSection(3, 2)
        assertItemInSection(4, 3)
    }

    @Test
    fun itsSectionsMayStartByPrefixedItem() {
        assume().that(indexer.sections).asList().containsExactly("#", "L", "S", "U")
        assertSectionStartsAtPosition(0, 0)
        assertSectionStartsAtPosition(1, 1)
        assertSectionStartsAtPosition(2, 2)
        assertSectionStartsAtPosition(3, 4)
    }
}

@RunWith(RobolectricTestRunner::class)
class WithUnexpectedItems : ItemBasedIndexerScenario() {
    override val items = listOf(
        "  \nHello World!",
        "This",
        "is",
        "unsorted",
        ""
    )

    @Test
    fun itShouldHaveCorrectAndSortedSections() {
        assertThat(indexer.sections).asList().containsExactly("#", "H", "I", "T", "U").inOrder()
    }

    @Test
    fun itShouldIgnoreLeadingWhitespaceCharacters() {
        assume().that(indexer.sections).asList().containsExactly("#", "H", "I", "T", "U").inOrder()

        // [0] "Hello World" with spaces -> Section [1] "H"
        assertItemInSection(0, 1)
    }

    @Test
    fun itShouldPutEmptyStringsInSharpSection() {
        assume().that(indexer.sections).asList().containsExactly("#", "H", "I", "T", "U").inOrder()

        // [4] "" -> Section [0] "#"
        assertItemInSection(4, 0)
    }

    @Test
    fun itShouldPutItemsInCorrectSections() {
        assume().that(indexer.sections).asList().containsExactly("#", "H", "I", "T", "U").inOrder()

        // [0] "Hello World" -> Section [1] "H"
        assertItemInSection(0, 1)
        // [1] "This" -> Section [3] "T"
        assertItemInSection(1, 3)
        // [2] "is" -> Section [2] "I"
        assertItemInSection(2, 2)
        // [3] "unsorted" -> Section [4] "U"
        assertItemInSection(3, 4)
        // [4] "" -> Section [0] "#"
        assertItemInSection(4, 0)
    }

    @Test
    fun itsSectionsShouldStartAtExistingPositions() {
        val itemIndices = Range.range(0, BoundType.CLOSED, items.size, BoundType.OPEN)
        for (sectionPosition in indexer.sections.indices) {
            assertThat(indexer.getPositionForSection(sectionPosition)).isIn(itemIndices)
        }
    }
}

@RunWith(RobolectricTestRunner::class)
class WithLotsOfItemsPerSection : ItemBasedIndexerScenario() {
    override val items = listOf(
        "Saint Cecilia",
        "Say Goodnight",
        "Scream",
        "Scream Aim Fire",
        "Sean",
        "See The Light",
        "Session",
        "Supermassive Black Hole",
        "Supremacy",
        "Survival",
        "Symphony Of Destruction",
        "T-Shirt",
        "Teenagers",
        "These Days",
        "Through Glass",
        "Throwdown",
        "Thunderstruck",
        "Time Is Running Out",
        "T.N.T",
        "Tribute"
    )

    @Test
    fun itShouldCreateSubsectionsWhenTenOrMoreItemsPerSection() {
        assertThat(indexer.sections).asList().containsExactly("SA", "SC", "SE", "SU", "SY", "T").inOrder()
    }

    @Test
    fun itMapsItemsToCorrectSubsection() {
        assume().that(indexer.sections).asList().containsExactly("SA", "SC", "SE", "SU", "SY", "T").inOrder()

        assertItemInSection(0, 0)
        assertItemInSection(1, 0)
        assertItemInSection(2, 1)
        assertItemInSection(3, 1)
        assertItemInSection(4, 2)
        assertItemInSection(5, 2)
        assertItemInSection(6, 2)
        assertItemInSection(7, 3)
        assertItemInSection(8, 3)
        assertItemInSection(9, 3)
        assertItemInSection(10, 4)
    }

    @Test
    fun itShouldReturnPositionOfFirstItemOfSubsection() {
        assume().that(indexer.sections).asList().containsExactly("SA", "SC", "SE", "SU", "SY", "T").inOrder()

        assertSectionStartsAtPosition(0, 0)
        assertSectionStartsAtPosition(1, 2)
        assertSectionStartsAtPosition(2, 4)
        assertSectionStartsAtPosition(3, 7)
        assertSectionStartsAtPosition(4, 10)
    }
}

/**
 * Describes a testing scenario where given items are loaded into the indexer.
 */
abstract class ItemBasedIndexerScenario {
    /** The indexer under test, initialized with items. */
    protected lateinit var indexer: AlphaSectionIndexer
    /** The list of items that should be added into the [indexer]. */
    protected abstract val items: List<String>

    /**
     * Initializes the indexer with the item provided by [items].
     */
    @Before
    fun setUp() {
        indexer = AlphaSectionIndexer()
        indexer.update(items.asSequence())
    }

    /**
     * Assert that the indexer under test returns the [expectedItemPosition] for the given [sectionIndex].
     * This is a helper function to test [AlphaSectionIndexer.getPositionForSection],
     * printing a cleaner error message when the assertion fails.
     */
    protected fun assertSectionStartsAtPosition(sectionIndex: Int, expectedItemPosition: Int) {
        val actualItemPosition = indexer.getPositionForSection(sectionIndex)
        assertWithMessage(
            "Section '%s' should start at item [%s] '%s', but started at item [%s] '%s'",
            indexer.sections[sectionIndex],
            expectedItemPosition, items[expectedItemPosition],
            actualItemPosition, items.getOrNull(actualItemPosition) ?: "<out of bound item>"
        ).that(actualItemPosition).isEqualTo(expectedItemPosition)
    }

    /**
     * Assert that the item at [itemPosition] is sorted in the section at [expectedSectionIndex].
     * This is a helper function to test [AlphaSectionIndexer.getSectionForPosition],
     * printing a cleaner error message when the assertion fails.
     */
    protected fun assertItemInSection(itemPosition: Int, expectedSectionIndex: Int) {
        val sections = indexer.sections
        val actualSectionIndex = indexer.getSectionForPosition(itemPosition)
        assertWithMessage(
            "Item [%s] '%s' should be in section '%s', but was in '%s'",
            itemPosition,
            items[itemPosition],
            sections[expectedSectionIndex],
            sections.getOrNull(actualSectionIndex) ?: "<out of bound section>"
        ).that(actualSectionIndex).isEqualTo(expectedSectionIndex)
    }
}
