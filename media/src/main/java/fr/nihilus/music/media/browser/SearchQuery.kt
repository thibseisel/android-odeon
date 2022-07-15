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

package fr.nihilus.music.media.browser

import android.os.Bundle
import android.provider.MediaStore

/**
 * A compact representation of a request to list or play media.
 */
sealed class SearchQuery {

    /**
     * An empty search query.
     * This should be treated as a request to play any music.
     */
    object Empty : SearchQuery()

    /**
     * A query to search a given artist.
     * @param name The name of the searched artist.
     */
    class Artist(val name: String?) : SearchQuery()

    /**
     * A query to search a specified album.
     * @param artist The name of the artist that produced the album.
     * @param title The title of the album.
     */
    class Album(val artist: String?, val title: String?) : SearchQuery()

    /**
     * A query to search a specific track.
     *
     * @param artist The name of the artist that participated in producing this track.
     * @param album The title of the album this track is part of.
     * @param title The title of the track.
     */
    class Song(val artist: String?, val album: String?, val title: String?) : SearchQuery()

    /**
     * A query whose search focus is unknown.
     * This should search the whole library for media that matches the query.
     *
     * @param userQuery The raw search query, as specified by clients.
     */
    class Unspecified(val userQuery: String) : SearchQuery()

    companion object {

        /**
         * Create a new search query by extracting information from the [raw query][query]
         * and optional search extras.
         * This uses the [search mode][MediaStore.EXTRA_MEDIA_FOCUS] to determine
         * what kind of media the query is about, as specified in the
         * [documentation for handling voice searches][https://developer.android.com/guide/components/intents-common.html#PlaySearch].
         *
         * @param query The raw search query as sent by clients.
         * @param options Optional extras that can include information about the query.
         */
        fun from(query: String?, options: Bundle?): SearchQuery =
            if (query.isNullOrBlank()) Empty
            else when (options?.getString(MediaStore.EXTRA_MEDIA_FOCUS)) {

                MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> Artist(
                    name = options.getString(MediaStore.EXTRA_MEDIA_ARTIST)
                )

                MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> Album(
                    artist = options.getString(MediaStore.EXTRA_MEDIA_ARTIST),
                    title = options.getString(MediaStore.EXTRA_MEDIA_ALBUM)
                )

                MediaStore.Audio.Media.ENTRY_CONTENT_TYPE -> Song(
                    artist = options.getString(MediaStore.EXTRA_MEDIA_ARTIST),
                    album = options.getString(MediaStore.EXTRA_MEDIA_ALBUM),
                    title = options.getString(MediaStore.EXTRA_MEDIA_TITLE)
                )

                else -> Unspecified(query)
            }
    }
}
