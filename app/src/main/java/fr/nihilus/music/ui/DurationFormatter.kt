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

package fr.nihilus.music.ui

import android.text.format.DateUtils

/**
 * A [ThreadLocal] variable that is initialized lazily when accessed by a thread.
 * Each thread has its own copy.
 * Once initialized for a thread, the value of this variable can't be changed.
 *
 * @param T Type of the variable.
 *
 * @constructor Instantiates a new immutable thread-local variable.
 * @param valueProvider A function returning the value of this variable for a given thread.
 */
private class ImmutableThreadLocal<T>(
    private val valueProvider: () -> T
) : ThreadLocal<T>() {
    override fun initialValue() = valueProvider()
    override fun set(value: T) = failMutation()
    override fun remove() = failMutation()
    private fun failMutation(): Nothing = error("This thread-local variable is immutable")
}

private val timeBuilder = ImmutableThreadLocal(::StringBuilder)

/**
 * Formats a duration in milliseconds to the `MM:SS` format,
 * where `SS` is the number of seconds and `MM` the number of minutes.
 *
 * @param millis Time duration expressed in milliseconds.
 * @return The duration formatted as a `MM:SS` string.
 */
internal fun formatDuration(millis: Long): String =
    DateUtils.formatElapsedTime(timeBuilder.get(), millis / 1000L)