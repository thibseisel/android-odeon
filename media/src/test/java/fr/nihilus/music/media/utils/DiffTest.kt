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

package fr.nihilus.music.media.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class DiffTest {

    @Test
    fun givenTwoIdenticalLists_whenDiffingThem_thenReturnAnEmptyDiff() {
        assertThatDiffIsEmptyFor(emptyList(), emptyList())
        assertThatDiffIsEmptyFor(listOf("Hello", "World"), listOf("Hello", "World"))
    }

    @Test
    fun givenTwoListsWithSameContentButDifferentOrder_whenDiffingThem_thenReturnAnEmptyDiff() {
        assertThatDiffIsEmptyFor(listOf("Hello", "World"), listOf("World", "Hello"))
        assertThatDiffIsEmptyFor(listOf("Foo", "Bar", "Baz"), listOf("Baz", "Foo", "Bar"))
    }

    private fun <T : Any> assertThatDiffIsEmptyFor(original: List<T>, modified: List<T>) {
        val (added, removed) = diffList(original, modified)
        assertThat(added).isEmpty()
        assertThat(removed).isEmpty()
    }

    @Test
    fun givenAddedElements_whenDiffingLists_thenListAddedItems() {
        assertThatElementsHaveBeenAdded(emptyList(), listOf("Foo"), listOf("Foo"))
        assertThatElementsHaveBeenAdded(listOf("Hello"), listOf("Hello", "World"), listOf("World"))
        assertThatElementsHaveBeenAdded(listOf("Bar"), listOf("Foo", "Bar", "Baz"), listOf("Foo", "Baz"))
    }

    private fun <T : Any> assertThatElementsHaveBeenAdded(
        original: List<T>,
        modified: List<T>,
        expectedAdditions: List<T>
    ) {
        val (added, _) = diffList(original, modified)
        assertThat(added).containsExactlyElementsIn(expectedAdditions)
    }

    @Test
    fun givenRemovedElements_whenDiffingLists_thenListRemovedElements() {
        assertThatElementsHaveBeenRemoved(listOf("Hello"), emptyList(), listOf("Hello"))
        assertThatElementsHaveBeenRemoved(listOf("Foo", "Bar", "Baz"), listOf("Bar"), listOf("Foo", "Baz"))
        assertThatElementsHaveBeenRemoved(listOf("Hello", "World"), emptyList(), listOf("Hello", "World"))
    }

    private fun <T : Any> assertThatElementsHaveBeenRemoved(
        original: List<T>,
        modified: List<T>,
        expectedRemovals: List<T>
    ) {
        val (_, deleted) = diffList(original, modified)
        assertThat(deleted).containsExactlyElementsIn(expectedRemovals)
    }

    @Test
    fun givenDuplicateAddedElements_whenDiffingLists_thenListThemBoth() {
        val original = listOf("Bar")
        val modified = listOf("Foo", "Foo", "Bar", "Baz")
        val (added, _) = diffList(original, modified)
        assertThat(added).containsAtLeast("Foo", "Foo")
    }

    @Test
    fun givenDuplicateRemovedElements_henDiffingLists_thenListThemBoth() {
        val original = listOf("Foo", "Foo", "Bar", "Baz")
        val modified = listOf("Bar")
        val (_, removed) = diffList(original, modified)
        assertThat(removed).containsAtLeast("Foo", "Foo")
    }

    private data class Person(val name: String, val age: Int)
    private val ageEqualizer = {a: Person, b: Person -> a.age == b.age }

    @Test
    fun givenAgeEqualizer_whenDiffingLists_thenListAddedPersonsComparingTheirAge() {
        val thibault = Person("Thibault", 24)
        val jack = Person("Jack", 24)
        val jane = Person("Jane", 26)
        val scarlett = Person("Scarlett", 18)

        val original = listOf(thibault)
        val modified = listOf(jack, jane, scarlett)

        val (added, _) = diffList(original, modified, ageEqualizer)
        assertThat(added).containsExactly(jane, scarlett)
    }

    @Test
    fun givenAgeEqualizer_whenDiffingLists_thenListRemovedPersonsComparingTheirAge() {
        val thibault = Person("Thibault", 24)
        val jack = Person("Jack", 24)
        val jane = Person("Jane", 26)
        val scarlett = Person("Scarlett", 18)

        val original = listOf(thibault, jane, scarlett)
        val modified = listOf(jack)

        val (_, removed) = diffList(original, modified, ageEqualizer)
        assertThat(removed).containsExactly(jane, scarlett)
    }
}