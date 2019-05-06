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

internal interface BrowserAction {
    suspend fun execute(parameters: Bundle?): Bundle?
}

/**
 * Defines contracts for executing custom actions.
 */
object CustomActions {

    /**
     * Custom action that deletes tracks identified by their media id.
     *
     * Executing this action requires the following parameters:
     * - [EXTRA_MEDIA_IDS] : the media ids of the tracks that should be deleted.
     *
     * On success, this action returns the following results:
     * - [RESULT_TRACK_COUNT] : the number of tracks that have been deleted.
     */
    const val ACTION_DELETE_TRACKS = "fr.nihilus.music.media.action.DELETE_TRACKS"

    /**
     * Custom action that adds tracks to a user-defined playlist.
     * If no playlist media id is specified, a new playlist will be created with a given title and the given tracks.
     *
     * ## Creation mode ##
     * Executing this action in *creation mode* requires the following parameters:
     * - [EXTRA_TITLE] : the title to be given to the newly created playlist.
     * - [EXTRA_MEDIA_IDS] (optional) : the media ids of tracks to be added to the new playlist.
     * If this parameter is unspecified, the playlist will be created without tracks.
     *
     * ## Edition mode ##
     * Executing this action in *edition mode* requires the following parameters:
     * - [EXTRA_PLAYLIST_ID] : the media id of the playlist to which tracks should be added.
     * - [EXTRA_MEDIA_IDS] : the media ids of tracks to be added to the existing playlist.
     *
     * Edition of a playlist fails if that playlist does not exist.
     * Also, tracks that are already in the target playlist are *not* added again.
     */
    const val ACTION_EDIT_PLAYLIST = "fr.nihilus.music.media.action.EDIT_PLAYLIST"

    /**
     * Custom action that deletes an existing user-defined playlist.
     *
     * Executing this action requires the following parameters:
     * - [EXTRA_PLAYLIST_ID] : the media id of the playlist to be deleted.
     * This action will fail if a playlist with the given media id does not exists.
     */
    const val ACTION_DELETE_PLAYLIST = "fr.nihilus.music.media.action.DELETE_PLAYLIST"

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

internal class ActionFailure(
    val errorCode: Int,
    val errorMessage: String?
) : Exception(errorMessage)