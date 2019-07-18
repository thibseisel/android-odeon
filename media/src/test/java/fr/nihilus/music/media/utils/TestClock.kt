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

package fr.nihilus.music.media.utils

/**
 * A [Clock] implementation used for testing.
 * This clock is stopped: its [current time][currentEpochTime] is fixed and can only be updated manually.
 * For consistency reasons, you cannot go back in time.
 *
 * @param startTime The initial epoch time. Should be positive or zero.
 */
internal class TestClock(startTime: Long) : Clock {
    init { require(startTime >= 0L) { "Invalid epoch time: $startTime" } }

    override var currentEpochTime: Long = startTime
        set(value) {
            require(value >= field) { "Attempt to go back in time!" }
            field = value
        }

}