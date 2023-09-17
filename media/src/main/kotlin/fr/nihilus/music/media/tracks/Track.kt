/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.media.tracks

import fr.nihilus.music.core.files.FileSize

/**
 * Playable media content.
 * Tracks may be stored on the device's storage or a remote server.
 *
 * Note: currently, tracks listed by this application are only retrieved from the device's storage.
 */
data class Track(
    /**
     * Unique identifier of the track.
     */
    val id: Long,
    /**
     * Title of this track as it should be displayed to the user.
     */
    val title: String,
    /**
     * Unique identifier of the artist that produced this track.
     */
    val artistId: Long,
    /**
     * Name of the artist that produced this track.
     */
    val artist: String,
    /**
     * Unique identifier of the album this track is part of.
     */
    val albumId: Long,
    /**
     * Title of the album this track is part of.
     */
    val album: String,
    /**
     * Playback duration of this track in milliseconds.
     */
    val duration: Long,
    /**
     * Disc number for this track's original source.
     */
    val discNumber: Int,
    /**
     * Position of this track in its original disc.
     */
    val trackNumber: Int,
    /**
     * Uri-style string pointing to the content associated with this track's metadata.
     * For media file stored on the device, this is a `content://` Android URI.
     * For a file stored on a remote server, this is an HTTP URL.
     */
    val mediaUri: String,
    /**
     * Uri-style string pointing to an optional artwork for this track's album.
     * May either be a `content://` URI or an HTTP URL.
     */
    val albumArtUri: String?,
    /**
     * Time at which this track has been made available to the application,
     * expressed as the number of seconds elapsed since January 1st 1970 (Unix Epoch).
     *
     * For an audio file stored locally, this is the time at which the file has been copied
     * to the device's storage.
     */
    val availabilityDate: Long,
    /**
     * Size of the audio file.
     */
    val fileSize: FileSize,
    /**
     * Time at which this track has been excluded from the music library.
     * This is `null` if this track has not been excluded.
     *
     * Excluded tracks are still present on the device's storage, but won't be displayed
     * in most parts of the application.
     */
    val exclusionTime: Long?,
)
