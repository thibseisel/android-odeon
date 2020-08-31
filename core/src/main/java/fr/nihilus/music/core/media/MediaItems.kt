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

package fr.nihilus.music.core.media

/**
 * A helper class grouping media-related constants.
 */
object MediaItems {

    /**
     * The listening time represented by this item, may it be a track, an album or a playlist.
     *
     * Type: long
     */
    const val EXTRA_DURATION = "fr.nihilus.music.extra.DURATION"

    /**
     * The number of tracks that this browsable media item contains.
     *
     * Type: integer
     */
    const val EXTRA_NUMBER_OF_TRACKS = "fr.nihilus.music.extra.NUMBER_OF_TRACKS"

    /**
     * The number of this track in its album.
     *
     * Type: integer
     */
    const val EXTRA_TRACK_NUMBER = "fr.nihilus.music.extra.TRACK_NUMBER"

    /**
     * The number of the disc this track appears.
     *
     * Type: integer
     */
    const val EXTRA_DISC_NUMBER = "fr.nihilus.music.extra.DISC_NUMBER"
}
