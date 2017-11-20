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

import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils

/**
 * For more information about voice search parameters,
 * check https://developer.android.com/guide/components/intents-common.html#PlaySearch
 */
class VoiceSearchParams
/**
 * Creates a simple object describing the search criteria from the query and extras.
 * @param query the query parameter from a voice search
 * @param extras the extras parameter from a voice search
 */
(@JvmField val query: String?, extras: Bundle?) {

    @JvmField var isAny: Boolean = false
    @JvmField var isUnstructured: Boolean = false
    @JvmField var isGenreFocus: Boolean = false
    @JvmField var isArtistFocus: Boolean = false
    @JvmField var isAlbumFocus: Boolean = false
    @JvmField var isSongFocus: Boolean = false
    @JvmField var genre: String? = null
    @JvmField var artist: String? = null
    @JvmField var album: String? = null
    @JvmField var song: String? = null

    init {
        if (TextUtils.isEmpty(query)) {
            // A generic search like "Play music" sends an empty query
            isAny = true
        } else {
            if (extras == null) {
                isUnstructured = true
            } else {
                val genreKey = if (Build.VERSION.SDK_INT >= 21) {
                    MediaStore.EXTRA_MEDIA_GENRE
                } else {
                    "android.intent.extra.genre"
                }

                val mediaFocus = extras.getString(MediaStore.EXTRA_MEDIA_FOCUS)
                if (TextUtils.equals(mediaFocus, MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE)) {
                    // for a Genre focused search, only genre is set:
                    isGenreFocus = true
                    genre = extras.getString(genreKey)
                    if (TextUtils.isEmpty(genre)) {
                        // Because of a bug on the platform, genre is only sent as a query, not as
                        // the semantic-aware extras. This check makes it future-proof when the
                        // bug is fixed.
                        genre = query
                    }
                } else if (TextUtils.equals(mediaFocus, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE)) {
                    // for an Artist focused search, both artist and genre are set:
                    isArtistFocus = true
                    genre = extras.getString(genreKey)
                    artist = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)
                } else if (TextUtils.equals(mediaFocus, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE)) {
                    // for an Album focused search, album, artist and genre are set:
                    isAlbumFocus = true
                    album = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM)
                    genre = extras.getString(genreKey)
                    artist = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)
                } else if (TextUtils.equals(mediaFocus, MediaStore.Audio.Media.ENTRY_CONTENT_TYPE)) {
                    // for a Song focused search, title, album, artist and genre are set:
                    isSongFocus = true
                    song = extras.getString(MediaStore.EXTRA_MEDIA_TITLE)
                    album = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM)
                    genre = extras.getString(genreKey)
                    artist = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)
                } else {
                    // If we don't know the focus, we treat it is an unstructured query:
                    isUnstructured = true
                }
            }
        }
    }

    override fun toString(): String {
        return ("query=" + query
                + " isAny=" + isAny
                + " isUnstructured=" + isUnstructured
                + " isGenreFocus=" + isGenreFocus
                + " isArtistFocus=" + isArtistFocus
                + " isAlbumFocus=" + isAlbumFocus
                + " isSongFocus=" + isSongFocus
                + " genre=" + genre
                + " artist=" + artist
                + " album=" + album
                + " song=" + song)
    }

}