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

package fr.nihilus.music.ui

import android.util.SparseIntArray
import android.widget.SectionIndexer
import java.text.Normalizer
import java.util.*

class AlphaSectionIndexer : SectionIndexer {
    private val items = mutableListOf<String>()
    private val positionForSection: MutableMap<String, Int> = TreeMap(SECTION_COMPARATOR)
    private val positionToSectionCache = SparseIntArray()
    private var sections: Array<String> = emptyArray()

    override fun getSections(): Array<String> = sections

    override fun getSectionForPosition(position: Int): Int {
        if (position !in items.indices) return 0

        val cachedSectionIndex = positionToSectionCache[position, -1]
        if (cachedSectionIndex >= 0) {
            return cachedSectionIndex
        }

        val itemAtPosition = items[position]
        val subSection = itemAtPosition.toNormalizedSubSection()
        val sectionIndex = if (positionForSection.containsKey(subSection)) {
            sections.binarySearch(subSection, SECTION_COMPARATOR)
        } else {
            val parentSection = subSection.substring(0, 1)
            sections.binarySearch(parentSection, SECTION_COMPARATOR)
        }

        return sectionIndex.coerceAtLeast(0).also {
            positionToSectionCache.put(position, it)
        }
    }

    override fun getPositionForSection(sectionIndex: Int): Int {
        if (sectionIndex !in sections.indices) throw IndexOutOfBoundsException()
        val section = sections[sectionIndex]
        return positionForSection[section] ?: 0
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
        positionToSectionCache.clear()
        val subSectionsPerSection = newItems.mapTo(items, String::toNormalizedSubSection)
            .asSequence()
            .withIndex()
            .groupBy { (_, subSection) -> subSection.substring(0, 1) }

        for ((section, subSections) in subSectionsPerSection) {
            if (subSections.size < 10) {
                positionForSection[section] = subSections.first().index
            } else {
                subSections
                    .groupingBy { it.value }
                    .foldTo(positionForSection, Int.MAX_VALUE) { accumulator, (index, _) ->
                        minOf(accumulator, index)
                    }
            }
        }

        sections = positionForSection.keys.toTypedArray()
    }
}

private val REGEX_COMBINE_DIACRITIC = Regex("[\\p{InCombiningDiacriticalMarks}]")

private val SECTION_COMPARATOR = Comparator { a: String, b: String ->
    when {
        a == b -> 0
        a == "#" -> -1
        b == "#" -> +1
        else -> a.compareTo(b)
    }
}

/**
 * Extract the section name from an item label.
 */
private fun String.toNormalizedSubSection(): String {
    val trimmed = this.trimStart().toUpperCase(Locale.ENGLISH)
    val withoutCommonPrefixes = when {
        trimmed.startsWith("THE ") -> trimmed.drop(4)
        trimmed.startsWith("AN ") -> trimmed.drop(3)
        trimmed.startsWith("A ") -> trimmed.drop(2)
        else -> trimmed
    }

    if (withoutCommonPrefixes.isEmpty() || !withoutCommonPrefixes.first().isLetter()) {
        return "#"
    }

    val sectionWithDiacritics = buildString(2) {
        val firstLetterIndex = withoutCommonPrefixes.indexOfFirst(Char::isLetter)
        if (firstLetterIndex != -1) {
            append(withoutCommonPrefixes[firstLetterIndex])
            val secondLetter = withoutCommonPrefixes.substring(firstLetterIndex + 1)
                .firstOrNull { it.isLetter() || it.isWhitespace() }
            if (secondLetter != null && secondLetter.isLetter()) {
                append(secondLetter)
            }
        }
    }

    return Normalizer.normalize(sectionWithDiacritics, Normalizer.Form.NFKD)
        .replace(REGEX_COMBINE_DIACRITIC, "")
}
