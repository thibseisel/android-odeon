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

package fr.nihilus.music.library.albums

import android.net.Uri
import fr.nihilus.music.library.albums.AlbumDetailState.Track

/**
 * UI representation of a music album with its composing tracks.
 * Albums are a compilation of tracks that were released at the same time,
 * and (generally) recorded by the same artist.
 */
internal class AlbumDetailState(

    /**
     * The media id of the album.
     * This can be used as an identifier to request the media service
     * to play all tracks from this album.
     */
    val id: String,

    /**
     * The title of the album.
     */
    val title: String,

    /**
     * The name of the artist that recorded this album.
     */
    val subtitle: String,

    /**
     * An Uri pointing to the album artwork, or `null` if there is none.
     */
    val artworkUri: Uri?,

    /**
     * The list of tracks that are part of the album
     * sorted by ascending [track number][Track.number].
     */
    val tracks: List<Track>
) {
    /**
     * An audio track, as part of an album.
     */
    data class Track(

        /**
         * The media id of this track.
         * This can be used as an identifier to request the media service to play this track.
         */
        val id: String,

        /**
         * The position of the track in the album.
         * Each track in an album is assigned a unique number indicating in which order
         * tracks are played on the original disc.
         */
        val number: Int,

        /**
         * The title of the track.
         */
        val title: String,

        /**
         * The duration of the track in milliseconds.
         * This should be a positive number.
         */
        val duration: Long,

        /**
         * Whether this track is the one currently playing.
         */
        val isPlaying: Boolean
    )
}