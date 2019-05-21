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

private const val CATEGORY_SEPARATOR = "/"
private const val LEAF_SEPARATOR = "|"

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
    require(categories.all { it.indexOf(CATEGORY_SEPARATOR) < 0 && it.indexOf(LEAF_SEPARATOR) < 0 }) {
        "Categories cannot contain the following characters: $CATEGORY_SEPARATOR or $LEAF_SEPARATOR"
    }

    categories.joinTo(this, CATEGORY_SEPARATOR)
    if (musicId >= 0L) {
        append('|')
        append(musicId)
    }
}