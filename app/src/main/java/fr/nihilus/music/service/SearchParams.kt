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
(@JvmField val query: String?, extras: Bundle?) {

    @JvmField val isAny: Boolean
    @JvmField val isUnstructured: Boolean
    @JvmField val isArtistFocus: Boolean
    @JvmField val isAlbumFocus: Boolean
    @JvmField val isSongFocus: Boolean
    @JvmField val artist: String?
    @JvmField val album: String?
    @JvmField val song: String?

    init {
        // Default values for each field
        var isUnstructured = false
        var isAlbumFocus = false
        var isArtistFocus = false
        var isSongFocus = false
        var artist: String? = null
        var album: String? = null
        var song: String? = null

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
                    // for an Artist focused search, both artist and genre are set
                    MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE -> {
                        isArtistFocus = true
                        artist = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)
                    }
                    // for an Album focused search, album, artist and genre are set
                    MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE -> {
                        isAlbumFocus = true
                        album = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM)
                        artist = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)
                    }
                    // for a Song focused search, title, album, artist and genre are set
                    MediaStore.Audio.Media.ENTRY_CONTENT_TYPE -> {
                        isSongFocus = true
                        song = extras.getString(MediaStore.EXTRA_MEDIA_TITLE)
                        album = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM)
                        artist = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST)
                    }
                    // If we don't know the focus, treat it as an unstructured query
                    else -> isUnstructured = true
                }
            }
        }

        this.isUnstructured = isUnstructured
        this.isArtistFocus = isArtistFocus
        this.isAlbumFocus = isAlbumFocus
        this.isSongFocus = isSongFocus
        this.artist = artist
        this.album = album
        this.song = song
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