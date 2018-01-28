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

package fr.nihilus.music.media.repo

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import io.reactivex.Observable
import io.reactivex.Single

/**
 * An object that represents the main entry point to access and modify music tracks.
 *
 * Track informations are queried using a unique, hierarchical identifier called the _media id_.
 * Media ids are composed of multiple categories (album, album number, artist...),
 * and if the media id points to a playable item (a music track), it also contains the unique
 * identifier of that track.
 *
 * Depending on implementations, a music repository may use a memory or a disc cache to speed-up
 * access to common or recently used queries.
 */
interface MusicRepository {

    /**
     * Build a set of items suitable for display composed of children of a given Media ID.
     *
     * The returned [Observable] will emit the requested items or an [UnsupportedOperationException]
     * if [parentMediaId] is unsupported.
     *
     * @param parentMediaId the media id that identifies the requested medias
     * @return an observable list of items suitable for display
     */
    fun getMediaItems(parentMediaId: String): Single<List<MediaBrowserCompat.MediaItem>>

    /**
     * Retrieve a given track's metadata from this repository.
     *
     * If the track does not exist, the observable will emit an error notification
     * with an [NoSuchElementException].
     *
     * @param musicId the unique numeric identifier of searched track
     * @return a single metadata item with the specified music id
     */
    fun getMetadata(musicId: String): Single<MediaMetadataCompat>

    /**
     * Release all references to objects loaded by this repository.
     * Implementations that use a memory cache should clear all entries.
     */
    fun clear()
}