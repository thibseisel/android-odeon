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

package fr.nihilus.music.media

import android.support.v4.media.MediaBrowserCompat.MediaItem

/**
 * A helper class grouping [MediaItem]-related constants.
 */
object MediaItems {
    /**
     * A non human-readable key used for sorting generated from the title of a track.
     *
     * Type: String
     */
    const val EXTRA_TITLE_KEY = "title_key"
    /**
     * The listening time represented by this item, may it be a track, an album or a playlist.
     *
     * Type: long
     */
    const val EXTRA_DURATION = "duration"
    /**
     * A non human-readable key used for sorting generated from the title of an album.
     *
     * Type: String
     */
    const val EXTRA_ALBUM_KEY = "album_key"
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
}
