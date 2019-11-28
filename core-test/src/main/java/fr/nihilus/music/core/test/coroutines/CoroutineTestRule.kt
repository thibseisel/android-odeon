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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

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
 *    fun `Check happy path`() = test.run {
 *        // This test body can now run suspend functions.
 *    }
 * }
 * ```
 */
@ExperimentalCoroutinesApi
class CoroutineTestRule : TestRule {

    /**
     * The dispatcher used for immediate execution of coroutines in test.
     * Use this dispatcher to virtually advance time or pause the immediate execution of coroutines.
     */
    val dispatcher = TestCoroutineDispatcher()

    /**
     * Executes a [testBody] inside an immediate execution dispatcher.
     * This a convenience function over `dispatcher.runBlockingTest`.
     *
     * @param testBody The code of the unit test.
     *
     * @see runBlockingTest
     */
    fun run(
        testBody: suspend TestCoroutineScope.() -> Unit
    ) = dispatcher.runBlockingTest(testBody)

    override fun apply(base: Statement, description: Description?): Statement = object : Statement() {
        override fun evaluate() {
            try {
                base.evaluate()
            } finally {
                dispatcher.cleanupTestCoroutines()
            }
        }
    }
}