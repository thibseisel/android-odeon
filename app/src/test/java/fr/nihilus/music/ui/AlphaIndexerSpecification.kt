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

import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.ints.shouldBeInRange
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    WithNoItem::class,
    WithOneItemPerSection::class,
    WithSameFirstLetter::class,
    WithNonLetterItems::class,
    WithDiacriticsItems::class,
    WithLeadingCommonEnglishPrefixes::class,
    WithUnexpectedItems::class,
    WithLotsOfItemsPerSection::class,
    WithLotsOfItemsPerSectionAndSpecialChars::class
)
class AlphaIndexerSpecification

class WithNoItem {

    private lateinit var indexer: AlphaSectionIndexer

    @Before
    fun whenInitialized() {
        indexer = AlphaSectionIndexer()
    }

    @Test
    fun itShouldHaveNoSections() {
        indexer.sections.shouldBeEmpty()
    }

    @Test
    fun itShouldAlwaysReturnSectionZero() {
        indexer.getSectionForPosition(0) shouldBe 0
        indexer.getSectionForPosition(2) shouldBe 0
        indexer.getSectionForPosition(5) shouldBe 0
    }

    @Test(expected = IndexOutOfBoundsException::class)
    fun itShouldFailWhenRequestingAnyPositionForSection() {
        indexer.getPositionForSection(0)
    }
}

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
        indexer.sections.shouldContainExactly("A", "B", "C", "G", "H", "S")
    }

    @Test
    fun itShouldMapPositionToCorrespondingSection() {
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
        assertSectionStartsAtPosition(0, 0)
        assertSectionStartsAtPosition(1, 1)
        assertSectionStartsAtPosition(2, 2)
        assertSectionStartsAtPosition(3, 3)
        assertSectionStartsAtPosition(4, 4)
        assertSectionStartsAtPosition(5, 5)
    }
}

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
        indexer.sections.shouldContainExactly("A", "B", "C", "H", "S")
    }

    @Test
    fun itShouldGroupItemsStartingWithSameLetterUnderSameSection() {
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

class WithNonLetterItems : ItemBasedIndexerScenario() {
    override val items = listOf(
        "[F]",
        "10 Years Today",
        "Another One Bites The Dust",
        "Get Lucky"
    )

    @Test
    fun itShouldHaveLeadingSharpSection() {
        indexer.sections.shouldNotBeEmpty()
        indexer.sections.first() shouldBe "#"
    }

    @Test
    fun itShouldAlsoHaveLetterSectionsForOtherItems() {
        indexer.sections.shouldContainExactly("#", "A", "G")
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

class WithDiacriticsItems : ItemBasedIndexerScenario() {
    override val items = listOf(
        "À la Claire Fontaine",
        "Ça (c'est vraiment toi)",
        "Californication",
        "Évier Métal"
    )

    @Test
    fun itShouldIgnoreDiacriticsInSectionNames() {
        indexer.sections.shouldContainExactly("A", "C", "E")
    }

    @Test
    fun itShouldPutDiacriticItemsInNonDiacriticSections() {
        assertItemInSection(0, 0)
        assertItemInSection(1, 1)
        assertItemInSection(2, 1)
        assertItemInSection(3, 2)
    }

    @Test
    fun itsSectionsMayStartByDiacriticItem() {
        assertSectionStartsAtPosition(0, 0)
        assertSectionStartsAtPosition(1, 1)
        assertSectionStartsAtPosition(2, 3)
    }
}

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
        indexer.sections.shouldContainExactly("#", "L", "S", "U")
    }

    @Test
    fun itShouldPutPrefixedItemsInUnprefixedSections() {
        assertItemInSection(0, 0)
        assertItemInSection(1, 1)
        assertItemInSection(2, 2)
        assertItemInSection(3, 2)
        assertItemInSection(4, 3)
    }

    @Test
    fun itsSectionsMayStartByPrefixedItem() {
        assertSectionStartsAtPosition(0, 0)
        assertSectionStartsAtPosition(1, 1)
        assertSectionStartsAtPosition(2, 2)
        assertSectionStartsAtPosition(3, 4)
    }
}

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
        indexer.sections.shouldContainExactly("#", "H", "I", "T", "U")
    }

    @Test
    fun itShouldIgnoreLeadingWhitespaceCharacters() {
        // [0] "Hello World" with spaces -> Section [1] "H"
        assertItemInSection(0, 1)
    }

    @Test
    fun itShouldPutEmptyStringsInSharpSection() {
        // [4] "" -> Section [0] "#"
        assertItemInSection(4, 0)
    }

    @Test
    fun itShouldPutItemsInCorrectSections() {
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
        for (sectionPosition in indexer.sections.indices) {
            indexer.getPositionForSection(sectionPosition).shouldBeInRange(items.indices)
        }
    }
}

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
        indexer.sections.shouldContainExactly("SA", "SC", "SE", "SU", "SY", "T")
    }

    @Test
    fun itMapsItemsToCorrectSubsection() {
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
        assertSectionStartsAtPosition(0, 0)
        assertSectionStartsAtPosition(1, 2)
        assertSectionStartsAtPosition(2, 4)
        assertSectionStartsAtPosition(3, 7)
        assertSectionStartsAtPosition(4, 10)
    }
}

class WithLotsOfItemsPerSectionAndSpecialChars : ItemBasedIndexerScenario() {

    override val items = listOf(
        "I Only Lie When I Love You",
        "Ich Tu Dir Weh",
        "Ich Will",
        "If You Have Ghosts",
        "If you Want Blood",
        "I'm a Lady",
        "I'm Going to Hello For This",
        "Immortalized",
        "In Loving Memory",
        "Indestructible",
        "Inside the Fire"
    )

    @Test
    fun itShouldIgnoreSpecialCharsWhenCreatingSubsections() {
        indexer.sections.shouldContainExactly("I", "IC", "IF", "IM", "IN")
    }

    @Test
    fun itMapsToCorrectSubsection() {
        assertItemInSection(0, 0)
        assertItemInSection(1, 1)
        assertItemInSection(2, 1)
        assertItemInSection(3, 2)
        assertItemInSection(4, 2)
        assertItemInSection(5, 3)
        assertItemInSection(6, 3)
        assertItemInSection(7, 3)
        assertItemInSection(8, 4)
        assertItemInSection(9, 4)
        assertItemInSection(10, 4)
    }

    @Test
    fun itShouldReturnPositionOfFirstItemOfSubsection() {
        assertSectionStartsAtPosition(0, 0)
        assertSectionStartsAtPosition(1, 1)
        assertSectionStartsAtPosition(2, 3)
        assertSectionStartsAtPosition(3, 5)
        assertSectionStartsAtPosition(4, 8)
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
        val actualSection = indexer.sections[sectionIndex]
        val expectedItem = items[expectedItemPosition]
        val actualItem = items.getOrNull(actualItemPosition) ?: "<out of bound item>"

        withClue("Section '$actualSection' should start at item [$expectedItemPosition] '$expectedItem', but started at item [$actualItemPosition] '$actualItem'") {
            actualItemPosition shouldBe expectedItemPosition
        }
    }

    /**
     * Assert that the item at [itemPosition] is sorted in the section at [expectedSectionIndex].
     * This is a helper function to test [AlphaSectionIndexer.getSectionForPosition],
     * printing a cleaner error message when the assertion fails.
     */
    protected fun assertItemInSection(itemPosition: Int, expectedSectionIndex: Int) {
        val sections = indexer.sections
        val actualSectionIndex = indexer.getSectionForPosition(itemPosition)

        val itemTitle = items[itemPosition]
        val expectedSection = sections[expectedSectionIndex]
        val actualSection = sections.getOrNull(actualSectionIndex) ?: "<out of bound section>"

        withClue("Item [$itemPosition] '$itemTitle' should be in section '$expectedSection', but was in '$actualSection'") {
            actualSectionIndex shouldBe expectedSectionIndex
        }
    }
}
