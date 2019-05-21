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

import android.os.Bundle
import android.provider.MediaStore
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import androidx.core.net.toUri
import fr.nihilus.music.media.extensions.*

/**
 * Convert this [MediaMetadataCompat] into its [MediaDescriptionCompat] equivalent.
 * @param categories the Media ID to use a prefix for this item's Media ID
 * @param builder an optional builder for reuse
 * @return a media description created from this track metadata
 */
@Deprecated(
    "MediaMetadataCompat are no longer transformed into MediaDescriptions. " +
    "In fact that's the over way around now: MediaDescription is extracted to MediaMetadata to update MediaSession."
)
internal fun MediaMetadataCompat.asMediaDescription(
    builder: MediaDescriptionCompat.Builder,
    vararg categories: String
): MediaDescriptionCompat {
    val musicId = this.id
    val extras = Bundle(4)
    extras.putString(MediaItems.EXTRA_TITLE_KEY, this.titleKey)
    extras.putLong(MediaItems.EXTRA_DURATION, this.duration)
    extras.putLong(MediaItems.EXTRA_TRACK_NUMBER, this.trackNumber)
    extras.putLong(MediaItems.EXTRA_DISC_NUMBER, this.discNumber)

    val mediaId = mediaIdOf(*categories, musicId = musicId.toLong())
    builder.setMediaId(mediaId)
        .setTitle(this.title)
        .setSubtitle(this.artist)
        .setIconBitmap(this.albumArt)
        .setIconUri(this.albumArtUri?.toUri())
        .setMediaUri(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
                .appendEncodedPath(musicId)
                .build()
        )
        .setExtras(extras)

    return builder.build()
}