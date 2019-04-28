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

private const val CATEGORY_SEPARATOR = "/"
private const val LEAF_SEPARATOR = "|"

@Deprecated(
    "Use MediaId.TYPE_ROOT instead.",
    ReplaceWith("MediaId.TYPE_ROOT", "fr.nihilus.music.media.MediaId"))
const val BROWSER_ROOT = "__ROOT__"

/**
 * The whole music library composed of all available tracks.
 */
@Deprecated(
    "\"tracks/all\" is the new media id for the music category",
    ReplaceWith(
        "MediaId.encode(MediaId.TYPE_TRACKS, MediaId.CATEGORY_ALL)",
        "fr.nihilus.music.media.MediaId"
    )
)
const val CATEGORY_MUSIC = "MUSIC"

/**
 * All available music albums.
 */
@Deprecated(
    "\"${MediaId.TYPE_ALBUMS}\" is the new media id for the album category",
    ReplaceWith("MediaId.TYPE_ALBUMS", "fr.nihilus.music.media.MediaId")
)
const val CATEGORY_ALBUMS = "ALBUMS"

/**
 * All artists that produced the available songs.
 */
@Deprecated(
    "\"${MediaId.TYPE_ARTISTS}\" is the new media id for the artist category",
    ReplaceWith("MediaId.TYPE_ARTISTS", "fr.nihilus.music.media.MediaId")
)
const val CATEGORY_ARTISTS = "ARTISTS"

/**
 * All user-defined getPlaylists.
 */
@Deprecated(
    "\"${MediaId.TYPE_PLAYLISTS}\" is the new media id for the playlist category",
    ReplaceWith("MediaId.TYPE_PLAYLISTS", "fr.nihilus.music.media.MediaId")
)
const val CATEGORY_PLAYLISTS = "PLAYLISTS"

@Deprecated(
    "\"${MediaId.TYPE_TRACKS}/${MediaId.CATEGORY_RECENTLY_ADDED}\" is the new media id for the most recent category",
    ReplaceWith(
        "MediaId.encode(MediaId.TYPE_TRACKS, MediaId.CATEGORY_RECENTLY_ADDED)",
        "fr.nihilus.music.media.MediaId"
    )
)
const val CATEGORY_MOST_RECENT = "RECENT"

@Deprecated(
    "\"${MediaId.TYPE_TRACKS}/${MediaId.CATEGORY_MOST_RATED}\" is the new media id for the most rated category",
    ReplaceWith(
        "MediaId.encode(MediaId.TYPE_TRACKS, MediaId.CATEGORY_MOST_RATED)",
        "fr.nihilus.music.media.MediaId"
    )
)
const val CATEGORY_MOST_RATED = "MOST_RATED"

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
@Deprecated("Use MediaId factory functions to create a media id in a safer way.")
fun mediaIdOf(vararg categories: String, musicId: Long = -1L): String = buildString {
    require(categories.all(::isValidCategory)) {
        "Categories cannot contain the following characters: $CATEGORY_SEPARATOR or $LEAF_SEPARATOR"
    }

    categories.joinTo(this, CATEGORY_SEPARATOR)
    if (musicId >= 0L) {
        append('|')
        append(musicId)
    }
}

/**
 * Indicates if a category is considered valid.
 * The only restriction is to avoid using the separator characters `/` and `|`.
 */
@Deprecated("Create media ids using factories defined in MediaId.")
private fun isValidCategory(category: String) =
    category.indexOf(CATEGORY_SEPARATOR) < 0 && category.indexOf(LEAF_SEPARATOR) < 0

/**
 * Extracts the music id from a given [mediaId].
 *
 * @return The music id in its String form,
 * or `null` if it does not have a music id or [mediaId] is `null`.
 */
@Deprecated("Music id can be extracted from MediaId instances.")
@Contract("null -> null")
fun musicIdFrom(mediaId: String?): String? =
    mediaId?.substringAfter(LEAF_SEPARATOR, "")?.takeUnless(String::isEmpty)

/**
 * Extracts the browse category from a non-browsable media.
 *
 * @param mediaId The media id from which the browse category should be extracted.
 * @return The browse category that is the direct parent of the specified media,
 * or the same [mediaId] if it is already browsable.
 */
@Deprecated("Browse category doesn't exist anymore. See MediaId for more detail on the new structure of media ids.")
fun browseCategoryOf(mediaId: String): String = mediaId.substringBefore(LEAF_SEPARATOR)

/**
 * Extracts category and category value from a [mediaId].
 *
 * @return A list whose first element is the browse category
 * and the second element, if present, is the category value.
 */
@Deprecated("Browse hierarchy doesn't exist anymore. See MediaId for more detail on the new structure of media ids.")
fun browseHierarchyOf(mediaId: String): List<String> =
    mediaId.substringBefore(LEAF_SEPARATOR).split(CATEGORY_SEPARATOR)