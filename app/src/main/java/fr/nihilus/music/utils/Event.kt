/*
 * Copyright 2018 Thibault Seisel
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

package fr.nihilus.music.utils

import android.arch.lifecycle.LiveData
import android.support.design.widget.Snackbar

/**
 * Encapsulate data that is exposed via [LiveData] to represent it as an event.
 * This prevents observers from using the same data more than a single time,
 * such as the text of a [Snackbar] or the result of a background operation.
 */
class Event<out T>(private val _data: T) {

    /**
     * Whether the event has already been handled.
     * An event is considered handled if its data has been read.
     */
    var hasBeenHandled = false
        private set

    /**
     * The data associated with the event.
     * This will be `null` if the event has already been handled.
     * Reading the data associated with this event marks it as handled.
     */
    val data: T?
        get() = if (!hasBeenHandled) {
            hasBeenHandled = true
            _data
        } else null
}