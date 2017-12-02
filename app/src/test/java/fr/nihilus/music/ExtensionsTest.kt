/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music

import org.junit.Assert.assertEquals
import org.junit.Test

class ExtensionsTest {

    @Test
    fun comparatorInReversedOrder() {
        val numbers = arrayOf(6, 19, 3, 0, 13, 12, 1)
        val sortAscending = Comparator<Int> { a, b -> a - b }

        val reversedSorting = sortAscending.inReversedOrder()
        numbers.sortWith(reversedSorting)

        arrayOf(19, 13, 12, 6, 3, 1, 0).forEachIndexed { index, expected ->
            assertEquals(expected, numbers[index])
        }
    }
}