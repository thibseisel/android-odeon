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

package fr.nihilus.music.media

import android.net.Uri
import fr.nihilus.music.core.media.MediaId

/**
 * A node from the media tree.
 */
sealed class MediaContent {

    /**
     * Unique identifier of this media node in the browse hierarchy.
     * [Playable media (leaf nodes)][AudioTrack] have a non-null [track-specific part][MediaId.track]
     * while [categories (browsable nodes)][MediaCategory] doesn't.
     */
    abstract val id: MediaId

    /**
     * The user-readable title of the media.
     * Depending on the kind of media, this is either the song name (for playable nodes)
     * or the label of the category (for browsable nodes).
     */
    abstract val title: String

    /**
     * Optional uri pointing to a graphical representation of this node.
     */
    abstract val iconUri: Uri?

    /**
     * Whether this media content has children on his own.
     */
    abstract val browsable: Boolean

    /**
     * Whether this media content can be played.
     *
     * Some media may be both browsable and playable: users may either browse their children
     * and play them individually, or play all its children at once.
     */
    abstract val playable: Boolean
}

/**
 * A leaf node in the media tree.
 */
data class AudioTrack(

    /**
     * Unique identifier of this track in the browse hierarchy.
     * Tracks are required to have a non-null [MediaId.track] part.
     */
    override val id: MediaId,

    /**
     * Title of this track.
     */
    override val title: String,

    /**
     * Name of the artist that recorded this audio track.
     */
    val artist: String,

    /**
     * Title of the album (collection of tracks) this track is part of.
     */
    val album: String,

    /**
     * Uri pointing to the audio file this track references, used to start playback.
     * That uri should not be shared with external applications.
     */
    val mediaUri: Uri,

    /**
     * Optional uri pointing to an album artwork.
     * This may be `null` if that album has no artwork or if one is not available.
     */
    override val iconUri: Uri? = null,

    /**
     * Playback duration of this track, in milliseconds.
     */
    val duration: Long,

    /**
     * Some albums (such as compilations) are packaged with multiple discs ;
     * in this case, this is the positive index of that disc in the whole set of discs.
     */
    val disc: Int,

    /**
     * Index of that audio track on its record disc.
     * When a given album has more than one disc, multiple tracks from that album
     * may have the same track number, but always a different disc number.
     */
    val number: Int

) : MediaContent() {

    override val browsable: Boolean
        get() = false

    override val playable: Boolean
        get() = true

    init {
        requireNotNull(id.track) { "Media id should be that of a playable media: $id" }
        require(duration >= 0L) { "Invalid duration for media \"$title\": $duration" }
    }
}

/**
 * Categories are collections of medias that groups them semantically.
 */
data class MediaCategory(

    /**
     * Unique identifier of this media category in the browse hierarchy.
     * The media id of a category should *not* have a [MediaId.track] part.
     */
    override val id: MediaId,

    /**
     * Human-readable name of the category.
     * This should be the type of media it contains,
     * for example the name of an album or an artist.
     */
    override val title: String,

    /**
     * Optional subheading describing the content of this category.
     * This is highly dependent on the nature of the category it describes.
     * For example, an album category might want to display the name of the artist
     * or the number of tracks it contains.
     */
    val subtitle: String? = null,

    /**
     * Optional uri pointing to a graphical representation of this category.
     */
    override val iconUri: Uri? = null,

    /**
     * Whether this category should be marked as both playable and browsable.
     * Selecting a playable category will start playback of all of its children.
     *
     * @see MediaContent.playable
     */
    override val playable: Boolean = false,

    /**
     * The number of children of this category.
     * This is not relevant for all categories, may be `0`.
     */
    val count: Int = 0

) : MediaContent() {

    override val browsable: Boolean
        get() = true

    init {
        require(id.track == null) { "Media id should be that of a browsable media: $id" }
        require(count >= 0) { "Invalid child count for category \"$title\": $count" }
    }
}
