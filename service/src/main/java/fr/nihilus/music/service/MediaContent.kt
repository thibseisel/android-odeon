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

package fr.nihilus.music.service

import android.net.Uri
import fr.nihilus.music.core.media.MediaId

/**
 * A content that is part of the media browser's tree-structure.
 * There are 2 kinds of media content:
 * - [Audio tracks][AudioTrack] that directly maps to an audio file.
 * - [Media categories][MediaCategory] that logically groups multiple tracks.
 *
 * @property id The unique identifier of this media in the browsable tree structure.
 * @property title A title that is suitable for display to the user.
 * @property subtitle A subtitle that is suitable for display to the user.
 * @property iconUri A content Uri pointing to a visual representation of this media.
 * `null` if there is no icon available.
 */
internal sealed class MediaContent(
    val id: MediaId,
    val title: String,
    val subtitle: String?,
    val iconUri: Uri?
)

/**
 * Define a group of audio tracks that share common features,
 * for example tracks that are part of the same album, produced by the same artist
 * or have similar acoustic properties.
 *
 * Media categories are browsable and their children are either playable tracks or other categories.
 * @property trackCount The number of tracks in this category.
 * @property isPlayable Whether children of this category should be played when requesting
 * to play this category.
 */
internal class MediaCategory(
    id: MediaId,
    title: String,
    subtitle: String? = null,
    iconUri: Uri? = null,
    val trackCount: Int = -1,
    val isPlayable: Boolean = false
) : MediaContent(id, title, subtitle, iconUri)

/**
 * Metadata associated with an audio file.
 *
 * @property album The title of the album this track is part of.
 * @property artist The name of the artist that produced this track.
 * @property discNumber If the album this track is part of has multiple discs,
 * this is the number of the disc, otherwise it is `1`.
 * @property trackNumber The position of this track on its corresponding album disc.
 * @property duration The duration of this track in milliseconds.
 * @property mediaUri An Uri pointing to the audio file.
 */
internal class AudioTrack(
    id: MediaId,
    title: String,
    subtitle: String?,
    val album: String,
    val artist: String,
    val discNumber: Int,
    val trackNumber: Int,
    val duration: Long,
    val mediaUri: Uri,
    iconUri: Uri?
) : MediaContent(id, title, subtitle, iconUri)