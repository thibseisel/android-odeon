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

package fr.nihilus.music.core.media

/**
 * Defines contracts for executing custom actions.
 */
object CustomActions {

    /**
     * A numeric code describing an error that occurred while executing a custom action.
     *
     * Type: `Int`
     */
    const val EXTRA_ERROR_CODE = "fr.nihilus.music.media.actions.EXTRA_ERROR_CODE"

    /**
     * An optional message describing an error that occurred while executing a custom action.
     * Depending on the associated [error code][EXTRA_ERROR_CODE],
     * the provided message may be user-readable.
     *
     * Type: `String?`
     */
    const val EXTRA_ERROR_MESSAGE = "fr.nihilus.music.media.actions.EXTRA_ERROR_MESSAGE"

    /**
     * Denotes that a custom action failed because it requires one or more extras
     * to be passed as parameters.
     * The message provided with this code is not suitable for display.
     *
     * @see EXTRA_ERROR_CODE
     */
    const val ERROR_CODE_PARAMETER = -1

    /**
     * Denotes that one or more media id passed as parameters for a custom action
     * are not properly formatted, or identify media that are unsupported.
     * The message provided with this code is not suitable for display.
     *
     * @see EXTRA_ERROR_CODE
     */
    const val ERROR_CODE_UNSUPPORTED_MEDIA_ID = -2

    /**
     * Denotes that a custom action failed because it requires a runtime permission to be granted.
     * The error message provided with this error code should be the name of the required permission.
     *
     * @see EXTRA_ERROR_CODE
     * @see EXTRA_ERROR_MESSAGE
     */
    const val ERROR_CODE_PERMISSION_DENIED = -3
}