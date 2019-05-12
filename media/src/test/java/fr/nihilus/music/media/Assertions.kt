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

package fr.nihilus.music.media

import org.junit.AssumptionViolatedException

/**
 * Assert that the following block function throws a given [Exception].
 *
 * @param E The type of the expected exception.
 * @param block The code block that should throw an exception of type [E].
 *
 * @return The expected exception.
 */
inline fun <reified E : Exception> assertThrows(block: () -> Unit): E {
    try {
        block()
        throw AssertionError("Expected an ${E::class.java.name} but no exception have been thrown.")
    } catch (e: Exception) {
        if (e !is E) {
            throw AssertionError("Expected an ${E::class.java.name} but was: $e")
        }

        return e
    }
}

/**
 * Make the current test fail.
 * @param message Description of the assertion that failed.
 */
fun fail(message: String): Nothing {
    throw AssertionError(message)
}

/**
 * Denote that the current test is not relevant because an assumption is invalid.
 * @param message Description of the assumption that failed.
 */
fun failAssumption(message: String): Nothing {
    throw AssumptionViolatedException(message)
}

fun stub(): Nothing {
    throw UnsupportedOperationException("Unexpected call to a stubbed method.")
}