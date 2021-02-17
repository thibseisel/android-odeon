/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.spotify.service

import fr.nihilus.music.spotify.model.SpotifyAlbum
import fr.nihilus.music.spotify.model.SpotifyArtist
import fr.nihilus.music.spotify.model.SpotifyTrack
import java.util.*

/**
 * Abstraction over a search query to be submitted to the Spotify search endpoint.
 * The Spotify Search endpoint accepts a query with a special syntax to refine the search results.
 * - use simple keywords to return all results whose name matches in any order.
 * - surround keywords with quotation marks `"` to match the keywords in order.
 * - prefix keywords with `artist:` to match on the artist name.
 * - prefix keywords with `album:` to match on the album name.
 * - prefix keywords with `track:` to match on the track title.
 *
 * Implementations of this class are responsible for building a query suitable
 * for searching media of type [T].
 *
 * @param T The type of the media to be searched from Spotify.
 */
internal sealed class SpotifyQuery<T : Any> {

    /**
     * Encodes the parameters of the query to a String.
     * The query is formed in a way that the provided keywords are matched in the same order
     * they are written.
     */
    abstract override fun toString(): String

    /**
     * Transforms user input to make it easier for Spotify to find matching results.
     *
     * It seems that Spotify cannot find results if the query contains single quotes
     * in a quoted field filter:
     * ```
     * track:"don't stop me now"
     * ```
     * does not work, so we remove single quotes from the query.
     *
     * @param text The input to sanitize.
     * @return The same text in lowercase and without single quotes.
     */
    protected fun sanitize(text: String): String {
        var result: String = text
        if (text.contains('\'')) {
            result = text.replace("'", "")
        }

        return result.toLowerCase(Locale.ENGLISH)
    }

    /**
     * Query parameters to find a track.
     *
     * @param title Keywords contained in the title of the searched track.
     * @param artist Optional keywords contained in the name of the artist that recorded the track.
     * @param album Optional keywords contained in the title of the album the track features in.
     */
    class Track(
        title: String,
        artist: String? = null,
        album: String? = null
    ) : SpotifyQuery<SpotifyTrack>() {

        val title = sanitize(title)
        val artist = artist?.let { sanitize(it) }
        val album = album?.let { sanitize(it) }

        override fun toString(): String = buildString {
            append("track:")
            append('"')
            append(title)
            append('"')

            if (artist != null) {
                append(' ')
                append("artist:")
                append('"')
                append(artist)
                append('"')
            }

            if (album != null) {
                append(' ')
                append("album:")
                append('"')
                append(album)
                append('"')
            }
        }
    }

    /**
     * Query parameters to find an album.
     *
     * @param title Keywords contained in the title of the searched album.
     * @param artist Optional keywords contained in the name of the artist that produced the album.
     */
    class Album(
        title: String,
        artist: String? = null
    ) : SpotifyQuery<SpotifyAlbum>() {

        val title = sanitize(title)
        val artist = artist?.let { sanitize(it) }

        override fun toString(): String = buildString {
            append('"')
            append(title)
            append('"')

            if (artist != null) {
                append(' ')
                append("artist:")
                append('"')
                append(artist)
                append('"')
            }
        }
    }

    /**
     * Query parameters to find an artist.
     *
     * @param name Keywords contained in the name of the searched artist.
     */
    class Artist(name: String) : SpotifyQuery<SpotifyArtist>() {
        val name = sanitize(name)

        override fun toString(): String = buildString {
            append('"')
            append(name)
            append('"')
        }
    }
}