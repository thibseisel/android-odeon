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

package fr.nihilus.music.media.tracks.local

/**
 * The metadata of a track that is saved on the device's storage.
 */
internal data class LocalTrack(
    /**
     * The unique identifier of the track on the device media storage index.
     */
    val id: Long,
    /**
     * The title of this track as it should be displayed to the user.
     */
    val title: String,
    /**
     * The name of the artist that produced this track.
     */
    val artist: String,
    /**
     * The title of the album this track is part of.
     */
    val album: String,
    /**
     * The playback duration of this track in milliseconds.
     */
    val duration: Long,
    /**
     * The disc number for this track's original source.
     */
    val discNumber: Int,
    /**
     * The position of this track from its original disc.
     */
    val trackNumber: Int,
    /**
     * An Uri-style String pointing to the content associated with this track's metadata.
     */
    val mediaUri: String,
    /**
     * An Uri-style String pointing to an optional artwork for this track's album.
     */
    val albumArtUri: String?,
    /**
     * The time at which this track has been added to the local storage,
     * expressed as the number of seconds elapsed since January 1st 1970 (Unix Epoch).
     */
    val availabilityDate: Long,
    /**
     * The unique identifier of the artist that produced this track.
     */
    val artistId: Long,
    /**
     * The unique identifier of the album this track is part of.
     */
    val albumId: Long,
    /**
     * The size of the file stored on the device's storage, in bytes.
     */
    val fileSize: Long,
)
