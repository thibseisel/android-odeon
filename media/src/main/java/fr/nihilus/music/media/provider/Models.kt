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

package fr.nihilus.music.media.provider

/**
 * The metadata of a track that is saved on the device's storage.
 */
internal class Track(

    /** The unique identifier of the track on the device media storage index. */
    val id: Long,

    /** The title of this track as it should be displayed to the user. */
    val title: String,

    /**
     * The name of the artist that produced this track.
     * @see Artist.name
     */
    val artist: String,

    /**
     * The title of the album this track is part of.
     * @see Album.title
     */
    val album: String,

    /** The playback duration of this track in milliseconds. */
    val duration: Long,

    /** The disc number for this track's original source. */
    val discNumber: Int,

    /** The position of this track from its original disc. */
    val trackNumber: Int,

    /** An Uri-style String pointing to the content associated with this track's metadata. */
    val mediaUri: String,

    /** An Uri-style String pointing to an optional artwork for this track's album. */
    val albumArtUri: String?,

    /**
     * The time at which this track has been added to the local storage,
     * expressed as the number of seconds elapsed since January 1st 1970 (Unix Epoch).
     */
    val availabilityDate: Long,

    /**
     * The unique identifier of the artist that produced this track.
     * @see Artist.id
     */
    val artistId: Long,

    /**
     * The unique identifier of the album this track is part of.
     * @see Album.id
     */
    val albumId: Long,

    /** The size of the file stored on the device's storage, in bytes. */
    val fileSize: Long
) {
    override fun toString(): String = "Track{id=$id, title=$title}"
    override fun equals(other: Any?): Boolean = this === other || (other is Track && other.id == id)
    override fun hashCode(): Int = id.toInt()
}

/**
 * The metadata of an album whose tracks are saved on the device's storage.
 */
internal class Album(
    /** The unique identifier of this album on the device's media storage index. */
    val id: Long,
    /** The title of the album as it should be displayed to the user. */
    val title: String,
    /**
     * The name of the artist that recorded this album.
     * @see Artist.name
     */
    val artist: String,
    /** The number of tracks that are part of this album. */
    val trackCount: Int,
    /** The year at which this album has been released. */
    val releaseYear: Int,
    /** An optional Uri-formatted String pointing to the artwork associated with this album. */
    val albumArtUri: String?,
    /**
     * The unique identifier of the artist that recorded this album.
     * @see Artist.id
     */
    val artistId: Long
) {
    override fun toString(): String = "Album{id=$id, title=$title, artist=$artist}"
    override fun equals(other: Any?): Boolean = this === other || (other is Album && other.id == id)
    override fun hashCode(): Int = id.toInt()
}

/**
 * The metadata of an artist that produced tracks that are saved on the device's storage.
 */
internal class Artist(
    /** The unique identifier of this artist on the device's media storage index. */
    val id: Long,
    /** The full name of this artist as it should be displayed to the user. */
    val name: String,
    /** The number of album this artist as recorded. */
    val albumCount: Int,
    /** The number of tracks that have been produced by this artist, all albums combined. */
    val trackCount: Int,
    /** An optional Uri-formatted String pointing to an artwork representing this artist. */
    val iconUri: String?
) {
    override fun toString(): String = "Artist{id=$id, name=$name}"
    override fun equals(other: Any?): Boolean = this === other || (other is Album && other.id == id)
    override fun hashCode(): Int = id.toInt()
}