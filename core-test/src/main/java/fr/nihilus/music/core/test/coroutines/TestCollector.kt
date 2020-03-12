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

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Subscribes to the source Flow, allowing to control the pace at which elements are collected.
 * This makes it easier to test scenarios that require to manually collect elements such as
 * infinite streams of values, flows that use a buffering policy (slow consumers, conflation)
 * or callback-driven flows.
 *
 * ## Example usage ##
 *
 * ```kotlin
 * val source = flow {
 *     emit(0)
 *     delay(200)
 *     emit(1)
 *     emit(2)
 *     throw Exception("Flow failure")
 * }
 *
 * source.test {
 *     expect(1)
 *     assertEquals(0, values[0])
 *
 *     expect(2, 200, TimeUnit.MILLISECONDS)
 *     assertEquals(1, values[1])
 *     assertEquals(2, values[2])
 *
 *     val exception = expectFailure()
 *     assertEquals("Flow failure", exception.message)
 * }
 * ```
 *
 * @param block Assertions to be performed on the elements to be collected from the source flow.
 * Collection of the flow is automatically cancelled at the end of the block.
 */
suspend fun <T> Flow<T>.test(block: suspend TestCollector<T>.() -> Unit) {
    val upstream = this
    supervisorScope {
        val channel = upstream.produceIn(this)
        val collector = TestCollector(channel)

        try {
            block(collector)
        } finally {
            channel.cancel()
        }
    }
}

/**
 * Controls the collection of a source `Flow`.
 */
class TestCollector<T> internal constructor(
    private val channel: ReceiveChannel<T>
) {
    private val _values = mutableListOf<T>()

    /**
     * The list of elements that have been currently collected from the source `Flow`
     * in the order of reception.
     *
     * Elements are appended to this list by calls to the [expect] function.
     */
    val values: List<T>
        get() = _values

    /**
     * Immediately consumes exactly [count] elements from the source flow.
     * The assertion will fail if the source Flow terminates
     * or throws an exception before those elements could be collected.
     *
     * @param count The number of elements to immediately expect from the flow.
     * Should be strictly positive.
     */
    suspend fun expect(count: Int) {
        require(count > 0)

        var collectCount = 0
        try {
            repeat(count) {
                yield()
                val value = channel.poll()

                when {
                    value != null -> {
                        _values += value
                        collectCount++
                    }

                    channel.isClosedForReceive -> {
                        throw AssertionError(buildString {
                            append("Expected to collect exactly ").append(count).append(" element(s) but ")
                            if (collectCount == 0) {
                                append("source Flow unexpectedly completed.")
                            } else {
                                append("only received ")
                                appendElements(values.takeLast(collectCount))
                                append(" before source Flow unexpectedly completed.")
                            }
                        })
                    }

                    else -> {
                        throw AssertionError(buildString {
                            append("Expected to collect exactly ").append(count).append(" element(s) but ")
                            if (collectCount == 0) {
                                append("did not receive any.")
                            } else {
                                append("only received ")
                                appendElements(values.takeLast(collectCount))
                                append('.')
                            }
                        })
                    }
                }
            }

        } catch (flowFailure: Exception) {
            throw AssertionError(buildString {
                append("Expected to collect exactly ").append(count).append(" element(s) but ")
                if (collectCount == 0) {
                    append("source Flow unexpectedly failed with ")
                    append(flowFailure::class.simpleName)
                    append('.')
                } else {
                    append("only received ")
                    appendElements(values.takeLast(collectCount))
                    append(" before source Flow unexpectedly failed with ")
                    append(flowFailure::class.simpleName)
                    append('.')
                }
            }, flowFailure)
        }
    }

    /**
     * Immediately collects at least [count] from the source flow.
     * Unlike [expect], more elements may be collected from the flow if available.
     *
     * The assertion will fail if the source Flow terminates
     * or throws an exception before those elements could be collected.
     *
     * @param count The minimum number of elements to be collected from the source flow.
     * Should be strictly positive.
     */
    suspend fun expectAtLeast(count: Int) {
        require(count > 0)

        var collectCount = 0
        try {
            collector@ while (true) {
                yield()
                val value = channel.poll()

                when {
                    value != null -> {
                        _values += value
                        collectCount++
                    }

                    channel.isClosedForReceive -> {
                        if (collectCount < count) {
                            throw AssertionError(buildString {
                                append("Expected to collect at least ").append(count).append(" element(s) but ")
                                if (collectCount == 0) {
                                    append("source Flow unexpectedly completed.")
                                } else {
                                    append("only received ")
                                    appendElements(values.takeLast(collectCount))
                                    append(" before source Flow unexpectedly completed.")
                                }
                            })
                        } else {
                            break@collector
                        }
                    }

                    else -> {
                        if (collectCount < count) {
                            throw AssertionError(buildString {
                                append("Expected to collect at least ").append(count).append(" element(s) but ")
                                if (collectCount == 0) {
                                    append("did not receive any.")
                                } else {
                                    append("only received ")
                                    appendElements(values.takeLast(collectCount))
                                    append('.')
                                }
                            })
                        } else {
                            break@collector
                        }
                    }
                }
            }

        } catch (flowFailure: Exception) {
            throw AssertionError(buildString {
                append("Expected to collect at least ").append(count).append(" element(s) but ")
                if (collectCount == 0) {
                    append("source Flow unexpectedly failed with ")
                    append(flowFailure::class.simpleName)
                    append('.')
                } else {
                    append("only received ")
                    appendElements(values.takeLast(collectCount))
                    append(" before source Flow unexpectedly failed with ")
                    append(flowFailure::class.simpleName)
                    append('.')
                }
            }, flowFailure)
        }
    }

    /**
     * Assert that there is currently no elements to be collected from the source flow.
     * This assertion will fail if one or more elements are immediately available
     * or if the source flow has thrown an exception.
     */
    fun expectNone() {
        try {
            val element = channel.poll()
            if (element != null) {
                throw AssertionError("Expected the source flow to have emitted no elements.")
            }
        } catch (flowFailure: Exception) {
            throw AssertionError(buildString {
                append("Expected the source flow to have emitted no elements, ")
                append("but unexpectedly failed with ")
                append(flowFailure::class.simpleName)
                append('.')
            }, flowFailure)
        }
    }

    /**
     * Consumes exactly [count] elements from the source flow, waiting at most the specified [duration].
     * This checks that at least [count] elements remains to be collected from the flow.
     * The assertion will fail if those elements couldn't be collected before [duration] has elapsed.
     *
     * @param count The number of elements to expect from the source flow within the given time duration.
     * Should be strictly positive.
     * @param duration The maximum time to wait for the requested elements.
     * Should be strictly positive.
     * @param unit The unit of time that [duration] is expressed in.
     */
    suspend fun expect(count: Int, duration: Long, unit: TimeUnit) {
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

        } catch (collectTimeout: TimeoutCancellationException) {
            throw AssertionError(buildString {
                append("Expected to collect exactly ").append(count).append(" element(s) ")
                if (collectCount == 0) {
                    append("but did not receive any")
                } else {
                    append("but only received ")
                    appendElements(values.takeLast(collectCount))
                }

                append(" within ")
                append(duration)
                append(' ')
                append(unit.name.toLowerCase(Locale.ENGLISH))
                append('.')
            })

        } catch (flowCompletion: ClosedReceiveChannelException) {
            throw AssertionError(buildString {
                append("Expected to collect exactly ").append(count).append(" element(s) ")
                if (collectCount == 0) {
                    append("but source Flow unexpectedly completed.")
                } else {
                    append("but only received ")
                    appendElements(values.takeLast(collectCount))
                    append(" before source Flow unexpectedly completed.")
                }
            })

        } catch (flowFailure: Exception) {
            throw AssertionError(buildString {
                append("Expected to collect exactly ").append(count).append(" element(s) ")
                if (collectCount == 0) {
                    append("but source Flow unexpectedly failed with ")
                    append(flowFailure::class.simpleName)
                    append('.')
                } else {
                    append("but only received ")
                    appendElements(values.takeLast(collectCount))
                    append(" before source Flow unexpectedly failed with ")
                    append(flowFailure::class.simpleName)
                    append('.')
                }
            }, flowFailure)
        }
    }

    /**
     * Expects the source flow to terminate with an exception.
     * This assertion will fail if the source Flow emits an element or terminates normally.
     *
     * @return The exception thrown by the source Flow.
     */
    suspend fun expectFailure(): Exception {
        try {
            val value = channel.receive()
            throw AssertionError("Expected the source Flow to throw an exception but it emitted \"$value\" instead.")

        } catch (flowCompletion: ClosedReceiveChannelException) {
            throw AssertionError("Expected the source Flow to throw an exception but it terminated normally.")

        } catch (flowFailure: Exception) {
            return flowFailure
        }
    }

    private fun Appendable.appendElements(values: List<*>) {
        if (values.isNotEmpty()) {
            values.joinTo(
                buffer = this,
                separator = ",\n  ",
                prefix = "[\n  ",
                postfix = "\n]",
                limit = 10
            )
        } else {
            append("[]")
        }
    }
}