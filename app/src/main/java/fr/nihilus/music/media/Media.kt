/*
 * Copyright 2018 Thibault Seisel
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

package fr.nihilus.music.media

import org.jetbrains.annotations.Contract

const val BROWSER_ROOT = "__ROOT__"

/**
 * The whole music library composed of all available tracks.
 */
const val CATEGORY_MUSIC = "BY_MUSIC"

/**
 * All available music albums.
 */
const val CATEGORY_ALBUMS = "BY_ALBUM"

/**
 * All artists that produced the available songs.
 */
const val CATEGORY_ARTISTS = "BY_ARTIST"

/**
 * All user-defined getPlaylists.
 */
const val CATEGORY_PLAYLISTS = "BY_PLAYLIST"

/**
 * A special selection of the most recently added songs.
 */
const val CATEGORY_BUILT_IN = "BUILT_IN"
const val CATEGORY_MOST_RECENT = "MOST_RECENT"

/**
 * Creates a String value that represents a playable or a browsable media.
 * This encodes the media browsable categories, if any, and the unique music ID, if any,
 * into a single String.
 *
 * Media IDs are of the form `<categoryType>/<categoryValue>|<musicUniqueId>` where
 * - `categoryType` is the root category this media belongs to like "by_artist" or "by_album",
 * - `categoryValue` defines a subcategory of the root category such as the album id or name,
 * - `musicUniqueId` uniquely identifies a track in the whole music library.
 *
 * Specifying the category hierarchy makes it easy to find the category
 * that a music was selected from.
 * This is specially useful when one music can appear in more than one list,
 * like "by album -> album_1" and "by artist -> artist_3".
 *
 * Depending on the number of provided arguments, this function can create IDs for playable media
 * (by specifying a music ID) or browsable media (only browse categories).
 *
 * @param categories hierarchy of categories representing this item's browsing parents
 * @param musicId If specified, unique music ID of the playable item.
 * @return a hierarchy-aware media ID.
 */
fun mediaIdOf(vararg categories: String, musicId: Long = -1L): String = buildString {
    categories.joinTo(this, separator = "/")
    if (musicId >= 0L) {
        append('|')
        append(musicId)
    }
}

/**
 * Extracts the music id from a given [mediaId].
 *
 * @return The music id in its String form,
 * or `null` if it does not have a music id or [mediaId] is `null`.
 */
@Contract("null -> null")
fun musicIdFrom(mediaId: String?) = mediaId?.substringAfter('|')?.takeUnless(String::isEmpty)

/**
 * Extracts the browse category from a non-browsable media.
 *
 * @param mediaId The media id from which the browse category should be extracted.
 * @return The browse category that is the direct parent of the specified media,
 * or the same [mediaId] if it is already browsable.
 */
fun browseCategoryOf(mediaId: String): String = mediaId.substringBefore('|')