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

package fr.nihilus.music.core.test.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.rules.TestWatcher
import org.junit.runner.Description

private const val TEST_TIMEOUT = 5_000L

/**
 * A JUnit Rule for running tests that use Kotlin Coroutines.
 *
 * Example usage:
 * ```
 * class FooTest {
 *
 *    @get:Rule
 *    val test = CoroutineTestRule()
 *
 *    @Test
 *    fun `Check happy path`() = test {
 *        // This test body can now run suspend functions.
 *    }
 * }
 * ```
 */
class CoroutineTestRule : TestWatcher() {
    private val scope = TestScope()

    val dispatcher: TestDispatcher = StandardTestDispatcher(scope.testScheduler)

    operator fun invoke(testBody: suspend TestScope.() -> Unit) = run(testBody)

    fun run(block: suspend TestScope.() -> Unit) = scope.runTest(TEST_TIMEOUT, testBody = block)

    /**
     * Runs a test within a child [CoroutineScope]. That scope is cancelled after [testBody] is run.
     *
     * This may be necessary when the test subject has a dependency on [CoroutineScope].
     * Since the test subject may launch coroutines tied to that scope, we want those coroutines
     * to be cancelled after executing the test ; otherwise the test would hang forever,
     * waiting for those coroutines to terminate.
     *
     * ```kotlin
     * @Test myTest() = test.runWithin { scope ->
     *   val subject = ClassUnderTest(scope)
     *   val result = subject.doSomething()
     *   assertEquals(expected, result)
     * }
     * ```
     *
     * @param testBody Test block. Unlike [run], the test lambda receives a parameter that's
     * the [CoroutineScope] to be injected into the test subject.
     */
    fun runWithin(testBody: suspend TestScope.(childScope: CoroutineScope) -> Unit) = run {
        launch(CoroutineName("runWithinScope")) {
            testBody(this)
            coroutineContext.cancelChildren(
                CancellationException("Reached end of test body of runWithinScope")
            )
        }
    }

    override fun starting(description: Description?) {
        super.starting(description)
        Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}
