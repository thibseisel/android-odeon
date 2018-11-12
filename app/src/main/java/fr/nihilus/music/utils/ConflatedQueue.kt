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

import java.util.*
import kotlin.NoSuchElementException

/**
 * A [Queue] implementation that only stores the last element enqueued.
 *
 * Note: this queue is not thread-safe.
 */
class ConflatedQueue<E> : Queue<E> {
    private var element: E? = null

    override val size: Int
        get() = if (element != null) 1 else 0

    override fun add(element: E?): Boolean = offer(element)

    override fun addAll(elements: Collection<E>): Boolean {
        if (elements.isNotEmpty()) {
            element = elements.last()
            return true
        }

        return false
    }

    override fun offer(e: E?): Boolean {
        if (e != null) {
            element = e
            return true
        } else {
            throw NullPointerException()
        }
    }

    override fun iterator(): MutableIterator<E> = Iterator(element)

    override fun peek(): E? = element

    override fun poll(): E? {
        val value = this.element
        this.element = null
        return value
    }

    override fun clear() {
        element = null
    }

    override fun remove(): E = poll() ?: throw NoSuchElementException()

    override fun remove(element: E?): Boolean {
        if (element == this.element) {
            this.element = null
            return true
        }

        return false
    }

    override fun removeAll(elements: Collection<E?>): Boolean {
        val currentElement = element ?: return false
        return if (currentElement in elements) {
            element = null
            true
        } else false
    }

    override fun element(): E = element ?: throw NoSuchElementException()

    override fun isEmpty(): Boolean = element == null

    override fun contains(element: E?): Boolean = (element == this.element)

    override fun containsAll(elements: Collection<E?>): Boolean = when (elements.size) {
        0 -> true
        1 -> this.element == elements.first()
        else -> false
    }

    override fun retainAll(elements: Collection<E?>): Boolean {
        val currentElement = element ?: return false
        return if (currentElement !in elements) {
            element = null
            true
        } else false
    }

    private class Iterator<E>(
        private val value: E?
    ) : MutableIterator<E> {
        private var hasNext = value != null

        override fun hasNext(): Boolean = hasNext

        override fun next(): E {
            if (hasNext) {
                hasNext = false
                return value!!
            } else {
                throw NoSuchElementException()
            }
        }

        override fun remove() {
            throw UnsupportedOperationException()
        }

    }
}