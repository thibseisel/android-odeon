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

import io.kotlintest.matchers.beEmpty
import io.kotlintest.matchers.collections.shouldContainExactly
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.should
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotThrowAny
import io.kotlintest.shouldThrow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import java.util.concurrent.TimeUnit
import kotlin.test.Test

/**
 * Validates the behavior of the `Flow.test` operator.
 */
internal class FlowTestOperatorTest {

    @Test
    fun `Given empty flow, when expecting an element then fail assertion`() = runBlockingTest {
        val source = emptyFlow<String>()

        // When expecting items immediately.
        source.test {
            val failedAssertion = shouldThrow<AssertionError> { expect(1) }
            failedAssertion.message shouldBe "Expected to collect exactly 1 element(s) but source Flow unexpectedly completed."
        }

        // When expecting items within a delay.
        source.test {
            val failedAssertion = shouldThrow<AssertionError> {
                expect(1, 200, TimeUnit.MILLISECONDS)
            }

            failedAssertion.message shouldBe "Expected to collect exactly 1 element(s) but source Flow unexpectedly completed."
        }
    }

    @Test
    fun `Given delayed flow, when expecting an element then fail assertion`() = runBlockingTest {
        val source = flow {
            delay(3000)
            emit(42)
        }

        // When expecting items immediately.
        source.test {
            val failedAssertion = shouldThrow<AssertionError> { expect(1) }
            failedAssertion.message shouldBe "Expected to collect exactly 1 element(s) but did not receive any."
        }

        // When expecting items within an insufficient delay.
        source.test {
            val failedAssertion = shouldThrow<AssertionError> {
                expect(1, 2, TimeUnit.SECONDS)
            }

            failedAssertion.message shouldBe "Expected to collect exactly 1 element(s) but did not receive any within 2 seconds."
        }
    }

    @Test
    fun `Given delayed flow, when expecting multiple elements and collected only some then fail assertion`() = runBlockingTest {
        val source = flow {
            emit(1)
            emit(2)
            delay(500)
            emit(3)
        }

        // When expecting 3 items immediately.
        source.test {
            val failedAssertion = shouldThrow<AssertionError> { expect(3) }

            failedAssertion.message shouldBe """
                |Expected to collect exactly 3 element(s) but only received [
                |  1,
                |  2
                |].
            """.trimMargin()
        }

        // When expecting 3 items within an insufficient delay.
        source.test {
            val failedAssertion = shouldThrow<AssertionError> {
                expect(3, 200, TimeUnit.MILLISECONDS)
            }

            failedAssertion.message shouldBe """
                |Expected to collect exactly 3 element(s) but only received [
                |  1,
                |  2
                |] within 200 milliseconds.
            """.trimMargin()
        }
    }

    @Test
    fun `Given flow of N elements, when expecting exactly more elements then fail assertion`() = runBlockingTest {
        val source = flowOf(98, 7)

        // When expecting too many items immediately.
        source.test {
            val failedAssertion = shouldThrow<AssertionError> { expect(3) }

            failedAssertion.message shouldBe """
            |Expected to collect exactly 3 element(s) but only received [
            |  98,
            |  7
            |] before source Flow unexpectedly completed.
        """.trimMargin()
        }

        // When expecting too many items immediately within a delay.
        source.test {
            val failedAssertion = shouldThrow<AssertionError> {
                expect(3, 200, TimeUnit.MILLISECONDS)
            }

            failedAssertion.message shouldBe """
                |Expected to collect exactly 3 element(s) but only received [
                |  98,
                |  7
                |] before source Flow unexpectedly completed.
            """.trimMargin()
        }
    }

    @Test
    fun `Given failed flow, when expecting an element then fail assertion`() = runBlockingTest {
        val source = flow<Int> {
            throw Exception("Unexpected Flow failure")
        }

        // When expecting elements immediately and got an exception.
        source.test {
            val failedAssertion = shouldThrow<AssertionError> { expect(1) }

            failedAssertion.message shouldBe "Expected to collect exactly 1 element(s) but source Flow unexpectedly failed with Exception."
            failedAssertion.cause?.message shouldBe "Unexpected Flow failure"
        }

        // When expecting elements within a delay and got an exception.
        source.test {
            val failedAssertion = shouldThrow<AssertionError> {
                expect(1, 200, TimeUnit.MILLISECONDS)
            }

            failedAssertion.message shouldBe "Expected to collect exactly 1 element(s) but source Flow unexpectedly failed with Exception."
            failedAssertion.cause?.message shouldBe "Unexpected Flow failure"
        }
    }

    @Test
    fun `Given flow with elements then failure, when expecting more elements then fail assertion`() = runBlockingTest {
        val source = flow {
            emit(102)
            emit(56)
            throw Exception("Unexpected Flow failure")
        }

        // When expecting 3 items immediately and got an exception instead of 3rd element.
        source.test {
            val failedAssertion = shouldThrow<AssertionError> { expect(3) }

            failedAssertion.message shouldBe """
                |Expected to collect exactly 3 element(s) but only received [
                |  102,
                |  56
                |] before source Flow unexpectedly failed with Exception.
            """.trimMargin()
            failedAssertion.cause?.message shouldBe "Unexpected Flow failure"
        }

        // When expecting 3 items within a delay and got an exception instead of 3rd element.
        source.test {
            val failedAssertion = shouldThrow<AssertionError> {
                expect(3, 200, TimeUnit.MILLISECONDS)
            }

            failedAssertion.message shouldBe """
                |Expected to collect exactly 3 element(s) but only received [
                |  102,
                |  56
                |] before source Flow unexpectedly failed with Exception.
            """.trimMargin()
            failedAssertion.cause?.message shouldBe "Unexpected Flow failure"
        }
    }

    @Test
    fun `Given sequential flow, when expecting elements then expose them in values`() = runBlockingTest {
        val source = flowOf(1, 2, 3)

        source.test {
            // The source flow should not have been collected yet.
            values should beEmpty()

            // Request to only collect one element.
            shouldNotThrowAny {
                expect(1)
            }

            values shouldHaveSize 1
            values[0] shouldBe 1

            // Request 2 more elements.
            // Those elements should be added to the list of collected values.
            shouldNotThrowAny {
                expect(2)
            }

            values shouldHaveSize 3
            values[0] shouldBe 1
            values[1] shouldBe 2
            values[2] shouldBe 3
        }
    }

    @Test
    fun `Given empty flow, when expecting failure then fail assertion`() = runBlockingTest {
        val source = emptyFlow<String>()

        source.test {
            val failedAssertion = shouldThrow<AssertionError> { expectFailure() }
            failedAssertion.message shouldBe "Expected the source Flow to throw an exception but it terminated normally."
        }
    }

    @Test
    fun `Given flow with elements then failure, when expecting failure then fail assertion`() = runBlockingTest {
        val source = flow {
            emit(42)
            throw Exception("Expected Flow failure")
        }

        source.test {
            val failedAssertion = shouldThrow<AssertionError> { expectFailure() }
            failedAssertion.message shouldBe "Expected the source Flow to throw an exception but it emitted \"42\" instead."
        }
    }

    @Test
    fun `Given flows with elements then failure, when expecting failure after expecting elements, then test passes`() = runBlockingTest {
        val source = flow {
            emit(42)
            throw Exception("Expected Flow failure")
        }

        shouldNotThrowAny {
            source.test {
                expect(1)
                expectFailure()
            }
        }
    }

    @Test
    fun `Given failed flow, when expecting failure then test passes`() = runBlockingTest {
        val source = flow<Nothing> {
            throw Exception("Expected Flow failure")
        }

        shouldNotThrowAny {
            source.test {
                expectFailure()
            }
        }
    }

    @Test
    fun `Given delayed flow, when expecting no elements then test passes`() = runBlockingTest {
        val source = flow {
            delay(200)
            emit(42)
        }

        shouldNotThrowAny {
            source.test {
                expectNone()
            }
        }
    }

    @Test
    fun `Given sequential flow, when expecting no elements then fail assertion`() = runBlockingTest {
        val source = flowOf(1, 2, 3)

        source.test {
            val failedAssertion = shouldThrow<AssertionError> { expectNone() }
            failedAssertion.message shouldBe "Expected the source flow to have emitted no elements."
        }
    }

    @Test
    fun `Given failed flow, when expecting no element then fail assertion`() = runBlockingTest {
        val source = flow<Nothing> {
            throw Exception("Unexpected flow failure")
        }

        source.test {
            val failedAssertion = shouldThrow<AssertionError> { expectNone() }
            failedAssertion.message shouldBe "Expected the source flow to have emitted no elements, but unexpectedly failed with Exception."
            failedAssertion.cause?.message shouldBe "Unexpected flow failure"
        }
    }

    @Test
    fun `Given empty flow, when expecting at least 1 element then fail assertion`() = runBlockingTest {
        val source = emptyFlow<Nothing>()

        source.test {
            val failedAssertion = shouldThrow<AssertionError> { expectAtLeast(1) }
            failedAssertion.message shouldBe "Expected to collect at least 1 element(s) but source Flow unexpectedly completed."
        }
    }

    @Test
    fun `Given delayed flow, when expecting at least 1 element then fail assertion`() = runBlockingTest {
        val source = flow {
            delay(200)
            emit(42)
        }

        source.test {
            val failedAssertion = shouldThrow<AssertionError> { expectAtLeast(1) }
            failedAssertion.message shouldBe "Expected to collect at least 1 element(s) but did not receive any."
        }
    }

    @Test
    fun `Given delayed flow, when expecting at least N elements and collected fewer then fail assertion`() = runBlockingTest {
        val source = flow {
            emit(1)
            emit(2)
            delay(500)
            emit(3)
        }

        source.test {
            val failedAssertion = shouldThrow<AssertionError> { expectAtLeast(3) }

            failedAssertion.message shouldBe """
                |Expected to collect at least 3 element(s) but only received [
                |  1,
                |  2
                |].
            """.trimMargin()
        }
    }

    @Test
    fun `Given flow of N elements, when expecting at least more elements then fail assertion`() = runBlockingTest {
        val source = flowOf(98, 7)

        source.test {
            val failedAssertion = shouldThrow<AssertionError> { expectAtLeast(3) }

            failedAssertion.message shouldBe """
            |Expected to collect at least 3 element(s) but only received [
            |  98,
            |  7
            |] before source Flow unexpectedly completed.
        """.trimMargin()
        }
    }

    @Test
    fun `Given failed flow, when expecting at least an element then fail assertion`() = runBlockingTest {
        val source = flow<Int> {
            throw Exception("Unexpected Flow failure")
        }

        source.test {
            val failedAssertion = shouldThrow<AssertionError> { expectAtLeast(1) }

            failedAssertion.message shouldBe "Expected to collect at least 1 element(s) but source Flow unexpectedly failed with Exception."
            failedAssertion.cause?.message shouldBe "Unexpected Flow failure"
        }
    }

    @Test
    fun `Given flow of N elements, when expecting at least N-1 elements then collect N`() = runBlockingTest {
        val source = flowOf(1, 2, 3)

        shouldNotThrowAny {
            source.test {
                expectAtLeast(2)
                values.shouldContainExactly(1, 2, 3)
            }
        }
    }
}