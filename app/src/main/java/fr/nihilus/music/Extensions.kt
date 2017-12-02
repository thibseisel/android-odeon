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

package fr.nihilus.music

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.annotation.LayoutRes
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.LongSparseArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.media.source.MusicDao
import fr.nihilus.music.utils.MediaID
import java.lang.ref.WeakReference

/**
 * Convert this [MediaMetadataCompat] into its [MediaDescriptionCompat] equivalent.
 * @param categories the Media ID to use a prefix for this item's Media ID
 * @param builder an optional builder for reuse
 * @return a media description created from this track metadatas
 */
fun MediaMetadataCompat.asMediaDescription(
        builder: MediaDescriptionCompat.Builder,
        vararg categories: String
): MediaDescriptionCompat {
    val musicId = getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID)
    val extras = Bundle(4)
    extras.putString(MediaItems.EXTRA_TITLE_KEY, getString(MusicDao.METADATA_KEY_TITLE_KEY))
    extras.putLong(MediaItems.EXTRA_DURATION, getLong(MediaMetadataCompat.METADATA_KEY_DURATION))
    extras.putLong(MediaItems.EXTRA_TRACK_NUMBER, getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER))
    extras.putLong(MediaItems.EXTRA_DISC_NUMBER, getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER))
    val artUri = getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
    val bitmapArt = getBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART)

    val mediaId = MediaID.createMediaId(categories, musicId)
    builder.setMediaId(mediaId)
            .setTitle(getString(MediaMetadataCompat.METADATA_KEY_TITLE))
            .setSubtitle(getString(MediaMetadataCompat.METADATA_KEY_ARTIST))
            .setIconBitmap(bitmapArt)
            .setMediaUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
                    .appendEncodedPath(musicId)
                    .build())
            .setExtras(extras)
    artUri?.let { builder.setIconUri(Uri.parse(it)) }

    return builder.build()
}

/**
 * Inflate the given layout as a child of this view group.
 *
 * @receiver the view parent in which inflate this layout
 * @param resource id for an XML resource to load
 * @param attach whether the inflated layout should be attached to this view group.
 * If false, the view group will be used to create the correct layout params.
 * @return the root view of the inflated hierarchy. If [attach] is `true`,
 * this will be this view group, otherwise the root of the inflated XML file.
 */
fun ViewGroup.inflate(@LayoutRes resource: Int, attach: Boolean = false): View =
        LayoutInflater.from(context).inflate(resource, this, attach)

/**
 * Helper extension function to execute a block of instructions only if
 * weak reference to an object is still valid.
 */
inline fun <T> WeakReference<T>.doIfPresent(action: (T) -> Unit) {
    get()?.let(action)
}

/**
 * Copy values from this sparse array to an array.
 * Items are inserted in the array in ascending key order.
 *
 * @return a copy of values from the sparse array as an array.
 */
inline fun <reified T> LongSparseArray<T>.toArray() = Array(this.size(), this::valueAt)

/**
 * Returns a comparator that imposes the reverse ordering of this comparator.
 * @return a comparator that imposes the reverse ordering of this comparator.
 */
fun <T> Comparator<T>.inReversedOrder() = Comparator<T> { a, b -> this.compare(b, a) }