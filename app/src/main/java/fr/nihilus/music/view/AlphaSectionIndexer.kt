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

import android.database.DataSetObserver
import android.util.SparseIntArray
import android.widget.SectionIndexer

class AlphaSectionIndexer : DataSetObserver(), SectionIndexer {
    private val items = mutableListOf<String>()
    private val positionForSection = SparseIntArray()
    private var sections: Array<String> = emptyArray()

    override fun getSections(): Array<String> = sections

    override fun getSectionForPosition(position: Int): Int {
        if (position !in items.indices) return 0

        val itemAtPosition = items[position]
        val section = itemAtPosition.stripCommonPrefixes().section

        val sectionIndex = sections.binarySearch(section, Comparator { a, b ->
            when {
                a == "#" -> -1
                b == "#" -> +1
                else -> a.compareTo(b)
            }
        })

        return sectionIndex.coerceAtLeast(0)
    }

    override fun getPositionForSection(sectionIndex: Int): Int {
        if (sectionIndex !in sections.indices) throw IndexOutOfBoundsException()
        val section = sections[sectionIndex]
        return positionForSection.get(sectionKey(section), 0)
    }

    override fun onChanged() {
        generateSections()
    }

    override fun onInvalidated() {
        sections = emptyArray()
    }

    fun updateItems(newTitles: List<String>) {
        items.clear()
        items += newTitles
    }

    private fun generateSections() {
        val sectionPositionMapper = items.asSequence()
            .map(String::stripCommonPrefixes)
            .withIndex()
            .groupingBy { (_, title) -> title.section }
            .fold(Int.MAX_VALUE) { accumulator, (index, _) -> minOf(accumulator, index) }

        sections = sectionPositionMapper.keys.toTypedArray()

        sectionPositionMapper.forEach { (section, itemPosition) ->
            positionForSection.put(sectionKey(section), itemPosition)
        }
    }
}

private fun String.stripCommonPrefixes(): String {
    with (this.trimStart().toUpperCase()) {
        return when {
            startsWith("THE ") -> drop(4)
            startsWith("AN ") -> drop(3)
            startsWith("A ") -> drop(2)
            else -> this
        }
    }
}

private fun sectionKey(section: String): Int {
    return if (section.isEmpty()) 0 else section.first().toInt()
}

private val String.section: String
    get() = if (firstOrNull()?.isLetter() != true) "#" else substring(0, 1)