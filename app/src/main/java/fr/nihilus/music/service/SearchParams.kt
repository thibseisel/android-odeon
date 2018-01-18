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

package fr.nihilus.music.service

import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils

/**
 * For more information about voice search parameters,
 * check https://developer.android.com/guide/components/intents-common.html#PlaySearch
 */
class SearchParams
/**
 * Creates a simple object describing the search criteria from the query and extras.
 * @param query the query parameter from a voice search
 * @param extras the extras parameter from a voice search
 */
    (@JvmField val query: String?, private val extras: Bundle?) {

    /**
     * If `true`, the search query is unspecified and its result could be anything,
     * provided the result is calculated based on a smart choice,
     * for example the last playlist the user listened to.
     */
    @JvmField val isAny: Boolean

    /**
     * If `true`, the search query gives no detail on what to search.
     */
    @JvmField val isUnstructured: Boolean

    /**
     * If `true`, this search is artist-specific.
     * That artist name is stored in [artist].
     */
    @JvmField val isArtistFocus: Boolean

    /**
     * If `true`, this search is album-specific.
     * The album name is stored in [album], and its related artist in [artist].
     */
    @JvmField val isAlbumFocus: Boolean

    /**
     * If `true`, this search is song-specific.
     * The song name is stored in [song].
     * This query also provides the album and the artist of this song.
     */
    @JvmField val isSongFocus: Boolean

    /**
     * Retrieve the artist name associated with this search.
     * @throws IllegalStateException If this search does not provide such information.
     */
    val artist: String
        get() = extras?.getString(MediaStore.EXTRA_MEDIA_ARTIST) ?: throw IllegalStateException()

    /**
     * Retrieve the name of album associated with this search.
     * @throws IllegalStateException If this search does not provide such information.
     */
    val album: String
        get() = extras?.getString(MediaStore.EXTRA_MEDIA_ALBUM) ?: throw IllegalStateException()

    /**
     * Retrieve the song name associated with this search.
     * @throws IllegalStateException If this search does not provide such information.
     */
    val song: String
        get() = extras?.getString(MediaStore.EXTRA_MEDIA_TITLE) ?: throw IllegalStateException()

    init {
        // Default values for each field
        var isUnstructured = false
        var isAlbumFocus = false
        var isArtistFocus = false
        var isSongFocus = false

        // There's no query. The application should play music based on a smart choice.
        this.isAny = TextUtils.isEmpty(query)

        if (!isAny) {
            if (extras == null) {
                // The type of the searched item is not specified
                isUnstructured = true

            } else {
                // Add more precision to the search by specifying the type of the searched item
                val mediaFocus = extras.getString(MediaStore.EXTRA_MEDIA_FOCUS)
                when (mediaFocus) {
                    MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> isArtistFocus = true
                    MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> isAlbumFocus = true
                    MediaStore.Audio.Media.ENTRY_CONTENT_TYPE -> isSongFocus = true
                    else -> isUnstructured = true
                }
            }
        }

        this.isUnstructured = isUnstructured
        this.isArtistFocus = isArtistFocus
        this.isAlbumFocus = isAlbumFocus
        this.isSongFocus = isSongFocus
    }

    override fun toString(): String {
        return ("query=" + query
                + " isAny=" + isAny
                + " isUnstructured=" + isUnstructured
                + " isArtistFocus=" + isArtistFocus
                + " isAlbumFocus=" + isAlbumFocus
                + " isSongFocus=" + isSongFocus
                + " artist=" + artist
                + " album=" + album
                + " song=" + song)
    }

}