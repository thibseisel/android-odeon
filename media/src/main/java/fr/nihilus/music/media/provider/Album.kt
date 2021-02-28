/*
 * Copyright 2021 Thibault Seisel
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

package fr.nihilus.music.media.provider

/**
 * The metadata of an album whose tracks are saved on the device's storage.
 */
data class Album(
    /**
     * The unique identifier of this album on the device's media storage index.
     */
    val id: Long,
    /**
     * The title of the album as it should be displayed to the user.
     */
    val title: String,
    /**
     * The name of the artist that recorded this album.
     * @see Artist.name
     */
    val artist: String,
    /**
     * The number of tracks that are part of this album.
     */
    val trackCount: Int,
    /**
     * The year at which this album has been released.
     */
    val releaseYear: Int,
    /**
     * An optional Uri-formatted String pointing to the artwork associated with this album.
     */
    val albumArtUri: String?,
    /**
     * The unique identifier of the artist that recorded this album.
     * @see Artist.id
     */
    val artistId: Long
)
