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

package fr.nihilus.music.media.actions

import android.os.Bundle
import fr.nihilus.music.media.MediaId

/**
 * A service-side handler for executing media browser custom actions.
 * Each custom action has a unique name that should be a constant from `CustomActions.ACTION_*`,
 * and can have an arbitrary number of client-specified parameters.
 *
 * In case the action cannot be performed for some reason (missing or invalid parameter, etc.)
 * an [ActionFailure] exception should be thrown.
 *
 * When completing normally, actions may return a [Bundle] containing the result of executing the action
 * for use by the calling media browser client.
 */
internal interface BrowserAction {

    /**
     * The name that uniquely identified this custom action.
     * Media browser clients should use this name to execute this action.
     *
     * The name of a custom action must be constant.
     */
    val name: String

    /**
     * Execute the action with the specified parameters, returning an optional result [Bundle] on success
     * or throwing an [ActionFailure] otherwise.
     *
     * Implementations should check that all required parameters are specified,
     * throwing an [ActionFailure] with code [CustomActions.ERROR_CODE_PARAMETER] if an expected parameters is missing.
     *
     * @param parameters The parameters for executing the action as provided by the calling media browser client.
     * @return an optional bundle containing results computed by the action.
     *
     * @throws ActionFailure When the action cannot be completed for some reason described by [ActionFailure.errorCode].
     */
    suspend fun execute(parameters: Bundle?): Bundle?
}

/**
 * Defines contracts for executing custom actions.
 */
object CustomActions {

    /**
     * Custom action that deletes media available from the media browser.
     * Media to be deleted with this action are identified by their media id.
     *
     * At the time, only tracks from `tracks/all` and playlists can be deleted with this action.
     * Any attempt to delete another type of media will fail.
     *
     * Executing this action requires the following parameters:
     * - [EXTRA_MEDIA_IDS] : the media ids of media to be deleted.
     *
     * On success, this action returns the following result:
     * - [RESULT_TRACK_COUNT] : when tracks have been deleted, the number of tracks that have been successfully deleted.
     */
    const val ACTION_DELETE_MEDIA = "fr.nihilus.music.media.actions.DELETE_MEDIA"

    /**
     * Custom action for creating and editing user-defined playlists.
     * Depending on the provided parameters, this action can perform in _CREATE_ and _EDIT_ mode.
     *
     * To *create a playlist*, you should specify the following parameters:
     * - [EXTRA_TITLE] : the title of the newly created playlist.
     * - [EXTRA_MEDIA_IDS] (optional) : the media ids of tracks to be added to the playlist, in order.
     * When specified, each media id is expected to have a [track identifier][MediaId.track] part.
     *
     * To *add tracks to an existing playlist*, you should specify the following parameters:
     * - [EXTRA_PLAYLIST_ID] : the media id of the playlist to edit.
     * This is expected to be a valid media id for an existing playlist.
     * - [EXTRA_MEDIA_IDS] : the media ids of tracks to be appended to the playlist, in order.
     * Each media id is expected to have a [track identifier][MediaId.track] part.
     */
    const val ACTION_MANAGE_PLAYLIST = "fr.nihilus.music.media.actions.MANAGE_PLAYLIST"

    /**
     * A number of tracks, returned as the result of executing a custom action.
     *
     * Type: `Int`
     */
    const val RESULT_TRACK_COUNT = "fr.nihilus.music.media.result.TRACK_COUNT"

    /**
     * An array of media ids.
     *
     * Type: `Array<String>`
     */
    const val EXTRA_MEDIA_IDS = "fr.nihilus.music.media.EXTRA_MEDIA_IDS"

    /**
     * The title to be given to a newly created entity.
     *
     * Type: `String`
     */
    const val EXTRA_TITLE = "fr.nihilus.music.media.EXTRA_TITLE"

    /**
     * The media id of a playlist.
     *
     * Type: `String`
     */
    const val EXTRA_PLAYLIST_ID = "fr.nihilus.music.media.EXTRA_PLAYLIST_ID"

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
}

/**
 * Denote that the execution of a [BrowserAction] has failed for some reason.
 *
 * @param errorCode A numeric code from `CustomActions.ERROR_CODE_*` that describes the error to media browser clients.
 * @param errorMessage An optional message describing the error.
 * Depending on the error code, this message may be displayed directly to users.
 */
internal class ActionFailure(
    val errorCode: Int,
    val errorMessage: String?
) : Exception(errorMessage)