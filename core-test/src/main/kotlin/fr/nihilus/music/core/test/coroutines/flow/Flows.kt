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

package fr.nihilus.music.core.test.coroutines.flow

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Creates a flow that produces values from the specified vararg-arguments,
 * then suspends indefinitely after emitting all elements.
 *
 * This is useful when simulating flows that occasionally emit elements but never terminates
 * such as a hot stream of events.
 *
 * @param elements The elements that are immediately emitted by the resulting flow.
 */
fun <T> infiniteFlowOf(vararg elements: T): Flow<T> = flow {
    for (element in elements) {
        emit(element)
    }

    awaitCancellation()
}
