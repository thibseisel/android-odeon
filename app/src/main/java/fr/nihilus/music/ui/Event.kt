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

package fr.nihilus.music.ui

import androidx.lifecycle.LiveData
import com.google.android.material.snackbar.Snackbar

/**
 * Encapsulate data that is exposed via [LiveData] to represent it as an event.
 * This prevents observers from using the same data more than a single time,
 * such as the text of a [Snackbar] or the result of a background operation.
 *
 * @param data The data associated with the event.
 */
class Event<out T>(data: T) {

    /**
     * The data associated with the event.
     * Reading the data associated with this event marks it as handled.
     */
    val data: T = data
        get() {
            hasBeenHandled = true
            return field
        }

    /**
     * Whether the event has already been handled.
     * An event is considered handled if its data has been read.
     */
    var hasBeenHandled = false
        private set

    /**
     * Handle the event only if it has not been already, consuming its associated data.
     * @param consumer A block of code to be executed only if the event has not been handled.
     */
    inline fun handle(consumer: (T) -> Unit) {
        if (!hasBeenHandled) {
            consumer(data)
        }
    }
}