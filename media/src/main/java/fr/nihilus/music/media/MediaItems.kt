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

package fr.nihilus.music.media

import android.support.v4.media.MediaBrowserCompat.MediaItem

/**
 * A helper class grouping [MediaItem]-related constants.
 */
object MediaItems {
    /**
     * The listening time represented by this item, may it be a track, an album or a playlist.
     *
     * Type: long
     */
    const val EXTRA_DURATION = "duration"
    /**
     * The number of tracks that this browsable media item contains.
     *
     * Type: integer
     */
    const val EXTRA_NUMBER_OF_TRACKS = "number_of_tracks"
    /**
     * Year in which this media item has been released.
     *
     * Type: integer
     */
    const val EXTRA_YEAR = "last_year"

    /**
     * The number of this track in its album.
     *
     * Type: integer
     */
    const val EXTRA_TRACK_NUMBER = "trackno"

    /**
     * The number of the disc this track appears.
     *
     * Type: integer
     */
    const val EXTRA_DISC_NUMBER = "discno"

    /**
     * The size of the local file associated with this media, in bytes.
     * This extra is only available on tracks from the [disposable category][MediaId.CATEGORY_DISPOSABLE].
     *
     * Type: long
     */
    const val EXTRA_FILE_SIZE = "filesize"

    /**
     * The epoch time at which that track has been played for the last time.
     * This extra is only available on tracks from the [disposable category][MediaId.CATEGORY_DISPOSABLE],
     * and only if that track has been played at least once.
     *
     * Type: long
     */
    const val EXTRA_LAST_PLAYED_TIME = "last_played_time"
}
