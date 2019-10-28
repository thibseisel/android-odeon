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

package fr.nihilus.music.core.media

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

/**
 * Thrown when [parsing][MediaId.parse] or [encoding][MediaId.encode] a media ID
 * that does not match the required format.
 *
 * @see MediaId
 */
class InvalidMediaException(message: String) : Exception(message)

/**
 * The format of media identifier used by the media browser of this application.
 * Media ids can identify media in a browse hierarchy, specifically:
 * - browsable media items, such as the root, all tracks, albums and artists
 * - playable tracks.
 *
 * ## Format ##
 *
 * Media ids are composed of a maximum of 3 parts delimited by separators:
 * - the _[type]_ of media, indicating which kind of media to browse,
 * - the browse _[category]_, that is the set of media from the given type to browse,
 * - the _[track]_ identifier that points to a playable content.
 *
 * Media ids accept 3 different formats, depending on the media they represent:
 *
 * 1. Type-only - `type`
 *
 * Those media ids only feature the type of media to be browsed.
 * Media with this kind of identifier are expected to be on top of the media hierarchy.
 *
 * 2. Type + category - `type/category`
 *
 * Those media ids define a subset of the specified media type,
 * such as a given artist, album or playlist.
 * While those media are expected to be browsable, they can also be playable:
 * in that case, all its playable children will be played.
 *
 * 3. Hierarchy-aware track - `type/category|track`
 *
 * This format includes the type, category and track identifier of a playable media.
 * Its [track] part uniquely identifies the playable media.
 * Note that 2 different media ids can have the same track part ; in this case, both media ids
 * refer to the same playable content, but browsed from a different type and category.
 * This way, it is possible to rebuild the same list of tracks to be played from the media id alone.
 *
 * ## Examples ##
 *
 * Here are some examples of valid media ids and their meaning.
 *
 * - `tracks` : define the root for browsing track media.
 * - `tracks/all` : browsing all tracks.
 * - `tracks/all|42` : the track with the ID `42` among all tracks.
 * - `albums/56` : the album whose identifier on the given storage is `56`.
 * - `albums/56|42` : the tracks with the ID `42` on the album number `56`.
 */
class MediaId
private constructor(

    /**
     * The type of media, indicating the kind of media to be browsed.
     */
    val type: String,

    /**
     * The browse category.
     * When specified, this defines a subset of media of the given [type].
     */
    val category: String?,

    /**
     * The track identifier.
     * When specified, this indicates that the media is playable.
     * This part uniquely identifies the playable content on a given storage.
     */
    val track: Long?,

    /**
     * This media id encoded as a String.
     * Two media ids with the same encoded form are considered equal.
     */
    val encoded: String

) {

    override fun equals(other: Any?): Boolean = when {
        other === this -> true
        other !is MediaId -> false
        else -> encoded == other.encoded
    }

    override fun hashCode(): Int = encoded.hashCode()

    /**
     * Returns this media id encoded as a String.
     */
    override fun toString(): String = encoded

    operator fun component1(): String = type
    operator fun component2(): String? = category
    operator fun component3(): Long? = track

    companion object Builder {
        private const val CATEGORY_SEPARATOR = '/'
        private const val TRACK_SEPARATOR = '|'

        /**
         * The media browser root.
         * This specific type has no category, and its children are all other types.
         */
        const val TYPE_ROOT = "root"

        /**
         * The media type that defines group of tracks that share common characteristics.
         *
         * @see MediaId.type
         */
        const val TYPE_TRACKS = "tracks"

        /**
         * Media of this type are artists albums.
         *
         * @see MediaId.type
         */
        const val TYPE_ALBUMS = "albums"

        /**
         * Media of this type are artists that participated in recording available tracks.
         *
         * @see MediaId.type
         */
        const val TYPE_ARTISTS = "artists"

        /**
         * Media of this type are user-defined playlists.
         * A playlist is a group of tracks composed by users.
         *
         * @see MediaId.type
         */
        const val TYPE_PLAYLISTS = "playlists"

        /**
         * Sub-category of the ["tracks" type][MediaId.TYPE_TRACKS] that lists all available tracks.
         *
         * @see MediaId.TYPE_TRACKS
         * @see MediaId.category
         */
        const val CATEGORY_ALL = "all"

        /**
         * Sub-category of the ["tracks" type][MediaId.TYPE_TRACKS] that lists the most recently added tracks first.
         *
         * TODO Semantics of what "most recently added" means are still to be defined.
         *
         * @see MediaId.TYPE_TRACKS
         * @see MediaId.category
         */
        const val CATEGORY_RECENTLY_ADDED = "new"

        /**
         * Sub-category of the ["tracks" type][MediaId.TYPE_TRACKS] that lists the user's preferred tracks by descending score.
         * Track score is based on usage statistics.
         *
         * TODO Semantics of what "most rated tracks" means are still to be defined.
         *
         * @see MediaId.TYPE_TRACKS
         * @see MediaId.category
         */
        const val CATEGORY_MOST_RATED = "rated"

        /**
         * Sub-category of the ["tracks" type][MediaId.TYPE_TRACKS] that lists tracks that could be deleted
         * to free up the device's space.
         * tracks are selected according to the following criteria:
         * 1. The filesize for that track in bytes.
         * 2. The last time that track has been listened to
         * 3. The score for that track (based on usage statistics)
         *
         * @see MediaId.TYPE_TRACKS
         * @see MediaId.category
         */
        const val CATEGORY_DISPOSABLE = "disposable"

        /**
         * The unique identifier of the root of the media browser.
         */
        @JvmField val ROOT =
            encode(TYPE_ROOT)

        /**
         * The unique identifier of the parent of all available tracks.
         * @see CATEGORY_ALL
         */
        @JvmField val ALL_TRACKS = encode(
            TYPE_TRACKS,
            CATEGORY_ALL
        )

        /**
         * The unique identifier of the parent of all albums.
         */
        @JvmField val ALL_ALBUMS = encode(TYPE_ALBUMS)

        /**
         * The unique identifier of the parent of all artists.
         */
        @JvmField val ALL_ARTISTS = encode(TYPE_ARTISTS)

        /**
         * The unique identifier of the parent of all playlists.
         */
        @JvmField val ALL_PLAYLISTS = encode(TYPE_PLAYLISTS)

        /**
         * Convenience function for creating media ids from their component parts.
         *
         * @see MediaId.fromParts
         */
        operator fun invoke(type: String, category: String? = null, track: Long? = null): MediaId =
            fromParts(type, category, track)

        /**
         * Assert that that passed [encoded] String matches the format for a media id
         * then split it into its composing parts.
         *
         * @throws InvalidMediaException If the passed String does not match the required format.
         */
        fun parse(encoded: String?): MediaId {
            if (encoded == null) throw InvalidMediaException("Null is not allowed as a media id.")

            val categorySeparatorIndex = encoded.indexOf(CATEGORY_SEPARATOR)
            if (categorySeparatorIndex == -1) {
                // First format: type only
                checkMediaType(encoded)
                return MediaId(encoded, null, null, encoded)
            }

            val type = encoded.substring(0, categorySeparatorIndex)
            checkMediaType(type)

            val trackSeparatorIndex = encoded.indexOf(TRACK_SEPARATOR, categorySeparatorIndex)
            if (trackSeparatorIndex == -1) {
                // Second format: type/category
                val category = encoded.substring(categorySeparatorIndex + 1)
                checkCategory(category)
                return MediaId(type, category, null, encoded)
            }

            // General format: type/category|track
            val category = encoded.substring(categorySeparatorIndex + 1, trackSeparatorIndex)
            checkCategory(category)

            val track = encoded.substring(trackSeparatorIndex + 1)
            checkTrackIdentifier(track)
            return MediaId(type, category, track.toLong(), encoded)
        }

        /**
         * Assembles a media id from its 3 components parts.
         * This checks that all parts are correctly formatted.
         *
         * @param type The type of the media, as per [MediaId.type].
         * @param category The optional category of the media, as per [MediaId.category].
         * @param track The optional track identifier, as per [MediaId.track].
         * @throws InvalidMediaException If one if the provided parts is incorrectly formatted.
         */
        fun fromParts(type: String, category: String? = null, track: Long? = null): MediaId {
            // Validate parts while creating the encoded form.
            val encoded = encode(type, category, track)
            return MediaId(type, category, track, encoded)
        }

        /**
         * Create a string representation of a media id from the provided parts.
         * Note that creating media ids this way follow the same rules defined by the [MediaId] class.
         *
         * @throws InvalidMediaException If one of the parts does not match the required format.
         */
        fun encode(type: String, category: String? = null, track: Long? = null): String {
            checkMediaType(type)

            return if (category == null && track == null)
                type
            else if (category != null) buildString {
                append(type)

                checkCategory(category)
                append(CATEGORY_SEPARATOR)
                append(category)

                if (track != null) {
                    checkTrackIdentifier(track)
                    append(TRACK_SEPARATOR)
                    append(track)
                }
            } else {
                throw InvalidMediaException("Media ids for tracks require a category.")
            }
        }

        private fun isValidType(type: String): Boolean = type.isNotEmpty() && type.all(Char::isLetter)

        private fun isValidCategory(category: String) = category.isNotEmpty() && category.all(Char::isLetterOrDigit)

        private fun isValidTrack(track: String) = track.isNotEmpty() && track.all(Char::isDigit)

        private fun checkMediaType(type: String) {
            if (!isValidType(type)) {
                throw InvalidMediaException("Invalid media type: $type.")
            }
        }

        private fun checkCategory(category: String) {
            if (!isValidCategory(category)) {
                throw InvalidMediaException("Invalid media category: $category")
            }
        }

        private fun checkTrackIdentifier(track: String) {
            if (!isValidTrack(track)) {
                throw InvalidMediaException("Invalid track identifier: $track")
            }
        }

        private fun checkTrackIdentifier(track: Long) {
            if (track < 0L) {
                throw InvalidMediaException("Invalid track identifier: $track")
            }
        }
    }
}

/**
 * Parse the receiver string to a media id,
 * checking its format and splitting it into its component parts.
 *
 * This is a convenient extension over [MediaId.parse].
 *
 * @receiver A media id in its string-encoded format.
 * While `null` is accepted as a value, its parsing will always fail.
 * @return A valid media id whose [encoded format][MediaId.encoded] is the same as the receiver.
 * @throws InvalidMediaException If the parsed string is `null` or an invalid media id.
 */
@UseExperimental(ExperimentalContracts::class)
fun String?.toMediaId(): MediaId {
    contract {
        returns() implies (this@toMediaId != null)
    }
    return MediaId.parse(this)
}

/**
 * Attempt to parse the receiver string to a media id,
 * returning `null` if its format is incorrect.
 *
 * @receiver A media id in its string-encoded format.
 * @return A valid media id whose [encoded format][MediaId.encoded] is the same as the receiver,
 * or `null` if parsing failed.
 */
fun String.toMediaIdOrNull(): MediaId? = try {
    MediaId.parse(this)
} catch (ime: InvalidMediaException) { null }