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

package fr.nihilus.music.media

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import org.junit.AssumptionViolatedException

/**
 * Run the given [block] in a new coroutine scope based on the context of the receiver.
 * That scope is cancelled after [block] ends.
 *
 * This allows simulating the lifecycle of a [CoroutineScope].
 *
 * @receiver The scope whose [CoroutineScope.coroutineContext] should be inherited from to create the new scope.
 * @param block The block to execute against the newly created [CoroutineScope].
 */
internal inline fun CoroutineScope.usingScope(block: (CoroutineScope) -> Unit) {
    val childScope = CoroutineScope(coroutineContext + Job())
    block(childScope)
    childScope.cancel()
}

/**
 * Convenience function to create a media id in a less verbose way.
 */
@Suppress("nothing_to_inline")
internal inline fun mediaId(type: String, category: String? = null, track: String? = null): MediaId =
    MediaId.fromParts(type, category, track)

internal fun failAssumption(failureMessage: String): Nothing {
    throw AssumptionViolatedException(failureMessage)
}