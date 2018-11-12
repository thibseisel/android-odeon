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

package fr.nihilus.music.utils

import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.truth.TruthJUnit.assume
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(WithEmptyQueue::class, WithFullQueue::class)
class ConflatedQueueTest

class WithEmptyQueue {
    private lateinit var queue: ConflatedQueue<String>

    @Before
    fun setUp() {
        queue = ConflatedQueue()
    }

    @Test
    fun queueShouldBeEmpty() {
        assertThat(queue).isEmpty()
        assertThat(queue.size).isEqualTo(0)
    }

    @Test
    fun whenAddingAnElement_itShouldNotBeEmpty() {
        val changed = queue.add("Foo")
        assertWithMessage("Queue should have changed as a result of calling Queue.add")
            .that(changed).isTrue()
        assertThat(queue).isNotEmpty()
        assertThat(queue.size).isEqualTo(1)
    }

    @Test(expected = NullPointerException::class)
    fun whenAddingNull_itShouldFailWithNPE() {
        queue.add(null)
    }

    @Test
    fun whenOfferingAnElement_itShouldNotBeEmpty() {
        val offered = queue.offer("Hello")

        assertWithMessage("Queue.offer(e) should return true when an element is added")
            .that(offered).isTrue()
        assertThat(queue).isNotEmpty()
        assertThat(queue.size).isEqualTo(1)
    }

    @Test(expected = NullPointerException::class)
    fun whenOfferingNull_itShouldFailWithNPE() {
        queue.offer(null)
    }

    @Test
    fun whenPeeking_itReturnsNull() {
        assertThat(queue.peek()).isNull()
    }

    @Test
    fun whenPolling_itReturnsNull() {
        assertThat(queue.poll()).isNull()
    }

    @Test(expected = NoSuchElementException::class)
    fun whenRemoving_itShouldFailWithNSE() {
        queue.remove()
    }

    @Test
    fun whenIterating_itShouldHaveNoElement() {
        val iterator = queue.iterator()
        assertThat(iterator.hasNext()).isFalse()
    }
}

class WithFullQueue {
    private lateinit var queue: ConflatedQueue<String>

    @Before
    fun setUp() {
        queue = ConflatedQueue()
        val queueHasChanged = queue.offer("Foo")
        assume().that(queueHasChanged).isTrue()
    }

    @Test
    fun itShouldNotBeEmpty() {
        assertThat(queue).isNotEmpty()
        assertThat(queue.size).isNotEqualTo(0)
    }

    @Test
    fun whenOfferingAnElement_sizeShouldNotIncrease() {
        val elementHasBeenAdded = queue.offer("Bar")
        assertThat(elementHasBeenAdded).isTrue()
        assertThat(queue.size).isEqualTo(1)
    }

    @Test
    fun whenOfferingMultipleTimes_itShouldOnlyKeepTheLastElement() {
        for (el in arrayOf("Bar", "Baz", "Hello World!")) {
            queue.add(el)
        }

        assertThat(queue.size).isEqualTo(1)
        assertThat(queue.peek()).isEqualTo("Hello World!")
    }

    @Test
    fun whenAddingAnElement_sizeDoesNotIncrease() {
        val queueHasChanged = queue.add("Bar")

        assertWithMessage("Queue should have changed as a result of calling Queue.add")
            .that(queueHasChanged).isTrue()
        assertThat(queue.size).isEqualTo(1)
    }

    @Test
    fun whenPeeking_itReturnsTheLastAddedElement() {
        assertThat(queue.peek()).isEqualTo("Foo")
        assertWithMessage("Peeking multiple times should return the same element")
            .that(queue.peek()).isEqualTo("Foo")

        queue.offer("Bar")
        assertThat(queue.peek()).isEqualTo("Bar")
    }

    @Test
    fun whenPolling_itReturnsThenRemovesTheElement() {
        assertThat(queue.poll()).isEqualTo("Foo")
        assertThat(queue).isEmpty()
    }

    @Test
    fun whenRemoving_itShouldReturnTheElementThenBeEmpty() {
        val element = queue.remove()
        assertThat(element).isEqualTo("Foo")
        assertThat(queue).isEmpty()
    }

    @Test
    fun whenIterating_itShouldReturnTheOnlyElement() {
        val iterator = queue.iterator()
        assertThat(iterator.hasNext()).isTrue()

        val element = iterator.next()
        assertThat(element).isEqualTo("Foo")

        assertThat(iterator.hasNext()).isFalse()
    }

    @Test
    fun whenClearingQueue_itShouldBeEmpty() {
        queue.clear()
        assertThat(queue).isEmpty()
    }
}