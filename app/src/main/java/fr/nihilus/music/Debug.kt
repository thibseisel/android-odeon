/*
 * Copyright 2017 Thibault Seisel
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

@file:JvmName("Debug")

package fr.nihilus.music

/**
 * Throws an AssertionError if the value is false and this build is a debug build.
 * This method is a replacement for the `assert` keyword or [kotlin.assert]
 * function that have no effect on Android.
 */
fun assert(value: Boolean) {
    assert(value) { "Assertion failed" }
}

/**
 * Throws an AssertionError calculated by [lazyMessage] if the value is false
 * and this build is a debug build.
 * This method is a replacement for the `assert` keyword or [kotlin.assert]
 * function that have no effect on Android.
 */
inline fun assert(value: Boolean, lazyMessage: () -> Any) {
    if (BuildConfig.DEBUG) {
        if (!value) {
            val message = lazyMessage()
            throw AssertionError(message)
        }
    }
}

/**
 * Decode playback state code into their equivalent constant name.
 */
val playbackStates = arrayOf(
        "NONE", "STOPPED", "PAUSED", "PLAYING", "FAST_FORWARDING",
        "REWINDING", "BUFFERING", "ERROR", "CONNECTING",
        "SKIPPING_TO_PREVIOUS", "SKIPPING_TO_NEXT", "SKIPPING_TO_QUEUE_ITEM")