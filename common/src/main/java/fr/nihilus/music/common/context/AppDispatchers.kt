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

package fr.nihilus.music.common.context

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.jetbrains.annotations.TestOnly

/**
 * A group of coroutine dispatchers to be used across the whole app.
 * Unlike [Dispatchers] this allows redefining dispatchers,
 * so that we can avoid concurrency and context switching in tests by using a single dispatcher.
 *
 * @property Main Dispatch execution of coroutine to the Android Main Thread.
 * @property Default Dispatch execution of coroutine to a pool of threads dedicated to computational-heavy tasks.
 * @property IO Dispatch execution of coroutine to a pool of threads dedicated to blocking IO tasks.
 */
class AppDispatchers(
    val Main: CoroutineDispatcher,
    val Default: CoroutineDispatcher,
    val IO: CoroutineDispatcher
) {
    /**
     * Create a group consisting of a single [CoroutineDispatcher].
     * This comes handy with a `TestCoroutineDispatcher` that confines execution of coroutine
     * to the test thread.
     *
     * @param dispatcher The [CoroutineDispatcher] to be used in all contexts.
     */
    @TestOnly constructor(
        dispatcher: CoroutineDispatcher
    ) : this(dispatcher, dispatcher, dispatcher)
}