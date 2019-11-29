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

import io.kotlintest.*
import io.kotlintest.matchers.beEmpty
import io.kotlintest.matchers.collections.shouldHaveSize
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.matchers.string.shouldStartWith
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import java.util.concurrent.TimeUnit
import kotlin.test.Test

internal class FlowTestOperatorTest {

    @Test
    fun `Given empty flow, when expecting an element then fail assertion`() = runBlockingTest {
        val source = emptyFlow<String>()

        val assertionFailure = shouldThrow<AssertionError> {
            source.test {
                expect(1)
            }
        }

        assertionFailure.message shouldBe "Expected to collect exactly 1 element(s), but source flow unexpectedly completed"
    }

    @Test
    fun `Given delayed flow, when expecting an element right away then fail assertion`() = runBlockingTest {
        val source = flow {
            delay(2000)
            emit(42)
        }

        val assertionFailure = shouldThrow<AssertionError> {
            source.test {
                expect(1)
            }
        }

        assertionFailure.message shouldStartWith "Expected to collect exactly 1 element(s), but did not receive any"
    }

    @Test
    fun `Given failed flow, when expecting an element then fail assertion`() = runBlockingTest {
        val source = flow<Int> {
            throw Exception("Unexpected flow failure")
        }

        val assertionFailure = shouldThrow<AssertionError> {
            source.test {
                expect(1)
            }
        }

        assertionFailure.message shouldBe "Expected to collect exactly 1 element(s), but source flow unexpectedly failed with Exception"
        assertionFailure.cause?.message shouldBe "Unexpected flow failure"
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

        shouldThrow<AssertionError> {
            source.test {
                expectFailure()
            }
        }
    }

    @Test
    fun `Given flows with elements then failure, when expecting failure then fail assertion`() = runBlockingTest {
        val source = flow {
            emit(42)
            throw Exception("Expected failure")
        }

        shouldThrow<AssertionError> {
            source.test {
                expectFailure()
            }
        }
    }

    @Test
    fun `Given flows with elements then failure, when expecting failure after expecting elements, then test passes`() = runBlockingTest {
        val source = flow {
            emit(42)
            throw Exception("Expected failure")
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
            throw Exception("Expected failure")
        }

        shouldNotThrowAny {
            source.test {
                expectFailure()
            }
        }
    }
}