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

import android.util.SparseIntArray
import android.widget.SectionIndexer
import java.text.Normalizer
import java.util.*

class AlphaSectionIndexer : SectionIndexer {
    private val items = mutableListOf<String>()
    private val positionForSection = SparseIntArray()
    private var sections: Array<String> = emptyArray()

    override fun getSections(): Array<String> = sections

    override fun getSectionForPosition(position: Int): Int {
        if (position !in items.indices) return 0

        val itemAtPosition = items[position]
        val section = itemAtPosition.toNormalizedSection()
        val sectionIndex = sections.binarySearch(section, SECTION_COMPARATOR)

        return sectionIndex.coerceAtLeast(0)
    }

    override fun getPositionForSection(sectionIndex: Int): Int {
        if (sectionIndex !in sections.indices) throw IndexOutOfBoundsException()
        val section = sections[sectionIndex]
        return positionForSection.get(sectionKey(section), 0)
    }

    /**
     * Update the set of items to be indexed.
     * This will re-generate the sections returned by [getSections].
     *
     * Strings returned by the provided Sequence are expected to be alphabetically sorted ;
     * otherwise the values returned by [getPositionForSection] are undefined.
     *
     * @param newItems A sequence returning the title of each item from the list
     * associated with this indexer.
     */
    fun update(newItems: Sequence<String>) {
        newItems.mapTo(items, String::toNormalizedSection)
        val sectionPositionMapper: Map<String, Int> = items.asSequence()
            .withIndex()
            .groupingBy { it.value }
            .foldTo(
                destination = TreeMap<String, Int>(SECTION_COMPARATOR),
                initialValue = Int.MAX_VALUE,
                operation = { accumulator, (index, _) -> minOf(accumulator, index) }
            )

        sections = sectionPositionMapper.keys.toTypedArray()
        sectionPositionMapper.forEach { (section, itemPosition) ->
            positionForSection.put(sectionKey(section), itemPosition)
        }
    }
}

private val REGEX_COMBINE_DIACRITIC = Regex("[\\p{InCombiningDiacriticalMarks}]")

/**
 * The ordering used
 */
private val SECTION_COMPARATOR = Comparator<String> { a: String, b: String ->
    when {
        a == b -> 0
        a == "#" -> -1
        b == "#" -> +1
        else -> a.compareTo(b)
    }
}

/**
 * Produce an integer key representing a given section.
 */
private fun sectionKey(section: String): Int = if (section.isEmpty()) 0 else section.first().toInt()

/**
 * Extract the section name from an item label.
 */
private fun String.toNormalizedSection(): String {
    val trimmed = this.trimStart().toUpperCase()
    val withoutCommonPrefixes = when {
        trimmed.startsWith("THE ") -> trimmed.drop(4)
        trimmed.startsWith("AN ") -> trimmed.drop(3)
        trimmed.startsWith("A ") -> trimmed.drop(2)
        else -> trimmed
    }

    if (withoutCommonPrefixes.isEmpty() || !withoutCommonPrefixes.first().isLetter()) {
        return "#"
    }

    val sectionWithDiacritics = withoutCommonPrefixes.substring(0, 1)
    return Normalizer.normalize(sectionWithDiacritics, Normalizer.Form.NFKD)
        .replace(REGEX_COMBINE_DIACRITIC, "")
}
