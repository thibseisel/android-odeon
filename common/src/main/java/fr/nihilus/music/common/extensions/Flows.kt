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

package fr.nihilus.music.common.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Terminal flow operator that launches a new coroutine to collect elements from the source flow
 * in the given [scope].
 * This is a shorthand for `flow.onEach(action).launchIn(scope)`.
 *
 * @receiver The source flow to collect.
 * @param scope The scope in which the new coroutine is created.
 * @return A reference to the collect job to manually cancel collection of the flow if necessary.
 */
inline fun <T> Flow<T>.collectIn(
    scope: CoroutineScope,
    crossinline action: suspend (T) -> Unit
): Job = scope.launch {
    collect(action)
}