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

package fr.nihilus.music.spotify.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Metadata of a track from the Spotify API.
 */
@JsonClass(generateAdapter = true)
internal class SpotifyTrack(

    /**
     * The unique identifier of this track on Spotify servers.
     */
    @Json(name = "id")
    val id: String,

    /**
     * The name of the track.
     */
    @Json(name = "name")
    val name: String,

    /**
     * The disc number (usually 1 unless the album consists of more than one disc).
     */
    @Json(name = "disc_number")
    val discNumber: Int,

    /**
     * The number of the track.
     * If an album has several discs, the track number is the number on the specified disc.
     */
    @Json(name = "track_number")
    val trackNumber: Int,

    /**
     * The track length in milliseconds.
     */
    @Json(name = "duration_ms")
    val duration: Int,

    /**
     * Whether or not the track has explicit lyrics ( true = yes it does; false = no it does not OR unknown).
     */
    @Json(name = "explicit")
    val explicit: Boolean
) {
    override fun toString(): String = "SpotifyTrack[id=$id, name=$name]"
}
