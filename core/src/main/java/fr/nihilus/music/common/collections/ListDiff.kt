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

package fr.nihilus.music.common.collections

class ListDiff<out E>(
    val additions: List<E>,
    val deletions: List<E>
) {
    operator fun component1() = additions
    operator fun component2() = deletions
}

fun <E : Any> diffList(
    original: List<E>,
    modified: List<E>,
    equalizer: (a: E, b: E) -> Boolean = { a, b -> a == b }
): ListDiff<E> {
    val additions = modified.filter { potentiallyAdded -> original.none { equalizer(it, potentiallyAdded) } }
    val removals = original.filter { potentiallyRemoved -> modified.none { equalizer(it, potentiallyRemoved) } }
    return ListDiff(additions, removals)
}