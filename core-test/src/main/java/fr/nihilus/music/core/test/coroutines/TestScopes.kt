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

package fr.nihilus.music.core.test.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest

/**
 * Runs a test within a child [CoroutineScope].
 * That scope is cancelled after [testBody] is run.
 *
 * This is necessary to test cases where the test subject has a dependency on a [CoroutineScope].
 * Since the test subject may launch coroutines tied to that scope, we want those coroutines
 * to be cancelled after executing the test.
 *
 * @receiver The parent of the created scope.
 * This is typically the scope provided by [runBlockingTest].
 * @param testBody The body of the test to be executed.
 */
suspend inline fun TestCoroutineScope.withinScope(
    crossinline testBody: suspend TestCoroutineScope.() -> Unit
) {
    val parentScope = this
    val childContext = SupervisorJob(parent = parentScope.coroutineContext[Job]) + CoroutineName("withinScope")
    val scope = TestCoroutineScope(parentScope.coroutineContext + childContext)
    try {
        scope.testBody()
    } finally {
        scope.cancel()
    }
}