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

import com.google.common.collect.BoundType
import com.google.common.collect.Range
import com.google.common.truth.Truth.assertThat
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
    fun itShouldReturnSectionForPosition() {
        assume().that(indexer.sections).asList().containsExactly("A", "B", "C", "G", "H", "S").inOrder()

        assertThat(indexer.getPositionForSection(0)).isEqualTo(0)
        assertThat(indexer.getPositionForSection(1)).isEqualTo(1)
        assertThat(indexer.getPositionForSection(2)).isEqualTo(2)
        assertThat(indexer.getPositionForSection(3)).isEqualTo(3)
        assertThat(indexer.getPositionForSection(4)).isEqualTo(4)
        assertThat(indexer.getPositionForSection(5)).isEqualTo(5)
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

    @Test
    fun itShouldReturnPositionOfFirstItemOfSection() {
        assume().that(indexer.sections).asList().containsExactly("A", "B", "C", "H", "S").inOrder()

        // Section [0] "A" starts at [0] "Another One Bites the Dust"
        assertThat(indexer.getPositionForSection(0)).isEqualTo(0)
        // Section [1] "B" starts at [1] "Back in Black"
        assertThat(indexer.getPositionForSection(1)).isEqualTo(1)
        // Section [2] "C" starts at [4] "Come As You Are"
        assertThat(indexer.getPositionForSection(2)).isEqualTo(4)
        // Section [3] "H" starts at [5] "Hells Bells"
        assertThat(indexer.getPositionForSection(3)).isEqualTo(5)
        // Section [4] "S" starts at [7] "Something Human"
        assertThat(indexer.getPositionForSection(4)).isEqualTo(7)
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
        assertThat(indexer.getSectionForPosition(0)).isEqualTo(0)
        // [1] "10 Years Today" -> [0] "#"
        assertThat(indexer.getSectionForPosition(1)).isEqualTo(0)
    }

    @Test
    fun itsSharpSectionShouldStartAtFirstItem() {
        assertThat(indexer.getPositionForSection(0)).isEqualTo(0)
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
        assertThat(indexer.getSectionForPosition(0)).isEqualTo(0)
        assertThat(indexer.getSectionForPosition(1)).isEqualTo(1)
        assertThat(indexer.getSectionForPosition(2)).isEqualTo(1)
        assertThat(indexer.getSectionForPosition(3)).isEqualTo(2)
    }

    @Test
    fun itsSectionsMayStartByDiacriticItem() {
        assume().that(indexer.sections).asList().containsExactly("A", "C", "E").inOrder()
        assertThat(indexer.getPositionForSection(0)).isEqualTo(0)
        assertThat(indexer.getPositionForSection(1)).isEqualTo(1)
        assertThat(indexer.getPositionForSection(2)).isEqualTo(3)
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
        assertThat(indexer.getSectionForPosition(0)).isEqualTo(0)
        assertThat(indexer.getSectionForPosition(1)).isEqualTo(1)
        assertThat(indexer.getSectionForPosition(2)).isEqualTo(2)
        assertThat(indexer.getSectionForPosition(3)).isEqualTo(2)
        assertThat(indexer.getSectionForPosition(4)).isEqualTo(3)
    }

    @Test
    fun itsSectionsMayStartByPrefixedItem() {
        assume().that(indexer.sections).asList().containsExactly("#", "L", "S", "U")
        assertThat(indexer.getPositionForSection(0)).isEqualTo(0)
        assertThat(indexer.getPositionForSection(1)).isEqualTo(1)
        assertThat(indexer.getPositionForSection(2)).isEqualTo(2)
        assertThat(indexer.getPositionForSection(3)).isEqualTo(4)
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
        // [0] "Hello World" with spaces -> Section [1] "H"
        assertThat(indexer.getSectionForPosition(0)).isEqualTo(1)
    }

    @Test
    fun itShouldPutEmptyStringsInSharpSection() {
        // [4] "" -> Section [0] "#"
        assertThat(indexer.getSectionForPosition(4)).isEqualTo(0)
    }

    @Test
    fun itShouldPutItemsInCorrectSections() {
        assume().that(indexer.sections).asList().containsExactly("#", "H", "I", "T", "U").inOrder()

        // [0] "Hello World" -> Section [1] "H"
        assertThat(indexer.getSectionForPosition(0)).isEqualTo(1)
        // [1] "This" -> Section [3] "T"
        assertThat(indexer.getSectionForPosition(1)).isEqualTo(3)
        // [2] "is" -> Section [2] "I"
        assertThat(indexer.getSectionForPosition(2)).isEqualTo(2)
        // [3] "unsorted" -> Section [4] "U"
        assertThat(indexer.getSectionForPosition(3)).isEqualTo(4)
        // [4] "" -> Section [0] "#"
        assertThat(indexer.getSectionForPosition(4)).isEqualTo(0)
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
        "Thunderstruck",
        "Tribute"
    )

    @Test
    fun itShouldCreateSubsectionsForFiveAndMoreItems() {
        assertThat(indexer.sections).asList().containsExactly("SA", "SC", "SE", "SU", "SY", "T").inOrder()
    }

    @Test
    fun itMapsItemsToCorrectSubsection() {
        assume().that(indexer.sections).asList().containsExactly("SA", "SC", "SE", "SU", "SY", "T").inOrder()

        assertThat(indexer.getSectionForPosition(0)).isEqualTo(0)
        assertThat(indexer.getSectionForPosition(1)).isEqualTo(0)
        assertThat(indexer.getSectionForPosition(2)).isEqualTo(1)
        assertThat(indexer.getSectionForPosition(3)).isEqualTo(1)
        assertThat(indexer.getSectionForPosition(4)).isEqualTo(2)
        assertThat(indexer.getSectionForPosition(5)).isEqualTo(2)
        assertThat(indexer.getSectionForPosition(6)).isEqualTo(2)
        assertThat(indexer.getSectionForPosition(7)).isEqualTo(3)
        assertThat(indexer.getSectionForPosition(8)).isEqualTo(3)
        assertThat(indexer.getSectionForPosition(9)).isEqualTo(3)
        assertThat(indexer.getSectionForPosition(10)).isEqualTo(4)
    }

    @Test
    fun itShouldReturnPositionOfFirstItemOfSubsection() {
        assume().that(indexer.sections).asList().containsExactly("SA", "SC", "SE", "SU", "SY", "T").inOrder()

        assertThat(indexer.getSectionForPosition(0)).isEqualTo(0)
        assertThat(indexer.getSectionForPosition(1)).isEqualTo(2)
        assertThat(indexer.getSectionForPosition(2)).isEqualTo(4)
        assertThat(indexer.getSectionForPosition(3)).isEqualTo(7)
        assertThat(indexer.getSectionForPosition(4)).isEqualTo(10)
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
        indexer.updateItems(items)
        indexer.onChanged()
    }
}
