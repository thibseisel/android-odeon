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

package fr.nihilus.music.core.collections

import androidx.collection.LongSparseArray

/**
 * Returns a [LongSparseArray] containing the elements from the given collection indexed by the key
 * returned from [keySelector] function applied to each element.
 *
 * If any two elements would have the same key returned by [keySelector] the last one gets added to the array.
 */
inline fun <T> Iterable<T>.associateByLong(keySelector: (T) -> Long) = LongSparseArray<T>().also {
    for (element in this) {
        it.put(keySelector(element), element)
    }
}

/**
 * The list of values that are stored in the [LongSparseArray].
 */
val <E> LongSparseArray<E>.values: List<E>
    get() = ArrayList<E>(size()).also {
        for (index in 0 until size()) {
            it.add(valueAt(index))
        }
    }
