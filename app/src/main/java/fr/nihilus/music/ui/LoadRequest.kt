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

/**
 * Encapsulate state of a data load operation.
 * @param T The type of data to be loaded.
 */
sealed class LoadRequest<out T> {

    /**
     * State of a data load that is pending.
     * @param T The type of data currently being loaded.
     */
    class Pending<T> : LoadRequest<T>()

    /**
     * State of a data load where the data is available.
     * @param T The type of data that has been loaded.
     * @param data The loaded data.
     */
    class Success<T>(val data: T) : LoadRequest<T>()

    /**
     * State of a data load that has failed for some reason.
     * @param T The type of data that should have been loaded in case of success.
     * @param error An exception describing the error that occurred.
     */
    class Error<T>(val error: Exception?) : LoadRequest<T>()
}
