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
import android.widget.SectionIndexer

class AlphaSectionIndexer(
    private val items: List<String>
) : DataSetObserver(), SectionIndexer {
    private var sections: Array<String> = emptyArray()

    override fun getSections(): Array<String> = sections

    override fun getSectionForPosition(position: Int): Int {
        TODO("Read the section index for that position from the data structure")
    }

    override fun getPositionForSection(sectionIndex: Int): Int {
        TODO("Read the position for the linked section from the data structure")
    }

    override fun onChanged() {
        generateSections()
    }

    override fun onInvalidated() {
        sections = emptyArray()
    }

    private fun generateSections() {
        val titlesPerSection = items.asSequence()
            .map { trimCommonPrefixes(it.trimStart().toUpperCase()) }
            .groupBy { it.section }

        sections = titlesPerSection.keys.toTypedArray()
    }
}

private fun trimCommonPrefixes(text: String) = when {
    text.startsWith("THE ") -> text.drop(4)
    text.startsWith("AN ") -> text.drop(3)
    text.startsWith("A ") -> text.drop(2)
    else -> text
}

private val String.section: String
    get() = if (!first().isLetter()) "#" else this.substring(0, 1)
