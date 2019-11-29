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

import io.kotlintest.shouldBe
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.Exception

suspend inline fun <T> Flow<T>.test(crossinline block: suspend TestCollector<T>.() -> Unit) {
    val upstream = this
    coroutineScope {
        val collector = TestCollector<T>()
        launch {
            collector.emitAll(upstream)
        }

        block(collector)
        collector.dispose()
    }
}

class TestCollector<T> : FlowCollector<T> {
    private val channel = Channel<T>(Channel.RENDEZVOUS)

    private val _values = mutableListOf<T>()
    val values: List<T>
        get() = _values

    override suspend fun emit(value: T) {
        channel.send(value)
    }

    /**
     * Freely consumes elements from the source flow for a given amount of time,
     * suspending the caller while doing so.
     * If collecting the flow throws an exception while waiting,
     * then this exception is rethrown by this function.
     *
     * @param duration The duration to wait.
     * @param unit The unit of time that [duration] is expressed in.
     */
    suspend fun wait(duration: Long, unit: TimeUnit) {
        require(duration >= 0)
        delay(unit.toMillis(duration))
    }

    /**
     * Consumes exactly [count] elements from the source flow, waiting at most the specified [duration].
     * This checks that at least [count] elements remains to be collected from the flow.
     * The assertion will fail if those elements couldn't be collected before [duration] has elapsed.
     *
     * @param count The number of elements to expect from the flow.
     * @param duration The maximum time to wait for the requested elements. Defaults to 1 second.
     * @param unit The unit of time that [duration] is expressed in.
     */
    suspend fun expect(
        count: Int,
        duration: Long = 1,
        unit: TimeUnit = TimeUnit.SECONDS
    ) {
        require(count > 0)
        require(duration > 0)

        var collectCount = 0
        try {
            withTimeout(unit.toMillis(duration)) {
                repeat(count) {
                    _values += channel.receive()
                    collectCount++
                }
            }

        } catch (tce: TimeoutCancellationException) {
            throw AssertionError(buildString {
                append("Expected to collect exactly ").append(count).append(" element(s), ")
                if (collectCount == 0) {
                    append("but did not receive any")
                } else {
                    append("but only received those ").append(collectCount).append(' ')
                    values.takeLast(collectCount).joinTo(this, prefix = "[", postfix = "]")
                }

                append(" after waiting for ")
                append(duration)
                append(' ')
                append(unit.name.toLowerCase(Locale.ENGLISH))
            })

        } catch (flowCompletion: ClosedReceiveChannelException) {
            throw AssertionError(buildString {
                append("Expected to collect exactly ").append(count).append(" element(s) ")
                if (collectCount == 0) {
                    append("but source flow unexpectedly completed")
                } else {
                    append("but only received those ").append(collectCount).append(' ')
                    values.takeLast(collectCount).joinTo(this, prefix = "[", postfix = "]")
                    append(" before it completed.")
                }
            })

        } catch (flowFailure: Exception) {
            throw AssertionError(buildString {
                append("Expected to collect exactly ").append(count).append(" element(s) ")
                if (collectCount == 0) {
                    append("but source flow unexpected failed with ").append(flowFailure::class.simpleName)
                } else {
                    append("but only received those ").append(collectCount).append(' ')
                    values.takeLast(collectCount).joinTo(this, prefix = "[", postfix = "]")
                    append(" before it failed with ").append(flowFailure::class.simpleName)
                }
            }, flowFailure)
        }
    }

    /**
     * Expects the source flow to terminate normally without an exception.
     */
    suspend fun expectTermination() {
        TODO()
    }

    /**
     * Expects the source flow to terminate with an exception.
     */
    suspend fun expectFailure(): Exception {
        try {
            channel.receive()
            throw AssertionError("Expected the Flow to terminate with an error, but none was thrown.")
        } catch (closed: ClosedReceiveChannelException) {
            throw AssertionError("Expected the Flow to terminate with an error, but it terminated normally.")
        } catch (failure: Exception) {
            return failure
        }
    }

    fun dispose() {
        channel.cancel()
    }
}

fun main() = runBlockingTest {
    try {
        runTest()
        println("Test completed successfully.")
    } catch (assert: AssertionError) {
        println("Test failure: ${assert.message}")
    }
}

private suspend fun TestCoroutineScope.runTest() {
    val source = flow {
        emit(1)
        emit(2)
        delay(200)
        error("Well, that escalated quickly.")
    }

    source.test {
        expect(2)
        values[0] shouldBe 1
        values[1] shouldBe 2
        expect(1, 200, TimeUnit.MILLISECONDS)
        values[2] shouldBe 3
    }
}