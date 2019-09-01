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

package fr.nihilus.music.media.provider

import fr.nihilus.music.media.permissions.PermissionDeniedException
import io.reactivex.Completable
import io.reactivex.Flowable

/**
 * Provides an entry point for performing read and write operations on media stored on the device's external storage.
 * This acts as a reactive layer over the Android MediaStore as an alternative to [MediaProvider].
 * Each set of media, namely [tracks], [albums] and [artists], are available as infinite data streams
 * whose latest emitted element is the most up-to-date media list.
 *
 * All background operations are performed on a dedicated Thread, thus preventing from blocking the Main Thread.
 *
 * Because accessing the external storage requires a runtime permission that can be revoked at anytime,
 * all operations may fail if permission to read/write external storage is denied.
 */
internal interface MediaDao {

    /**
     * The list of all tracks that are stored on the device's external storage, sorted by title.
     * Each track corresponds to a media file whose location can be resolved using its [content uri][Track.mediaUri]
     * and is identified by a unique [track identifier][Track.id], an [album][Track.albumId] and an [artist][Track.artistId].
     *
     * Subscribing to this stream will always load the currently available tracks, then emit a new track list
     * whenever it is updated.
     *
     * This may fail with [PermissionDeniedException] if permission is not granted or revoked while subscribed.
     */
    val tracks: Flowable<List<Track>>

    /**
     * The list of all albums that are stored on the device's external storage, sorted by title.
     * Albums are identified by a unique [album identifier][Album.id] and an [artist][Album.artistId].
     * Unlike tracks, albums does not map directly to a file and are generated from the metadata of existing tracks.
     *
     * Subscribing to this stream will always load the currently available albums, then emit a new album list
     * whenever it is updated.
     *
     * This may fail with [PermissionDeniedException] if permission is not granted or revoked while subscribed.
     */
    val albums: Flowable<List<Album>>

    /**
     * The list of all artists that are stored on the device's external storage, sorted by name.
     * Albums are identified by a unique [album identifier][Album.id] and an [artist][Album.artistId].
     * Unlike tracks, artists does not map directly to a file and are generated from the metadata of existing tracks.
     *
     * Subscribing to this stream will always load the currently available artists, then emit a new artist list
     * whenever it is updated.
     *
     * This may fail with [PermissionDeniedException] if permission is not granted or revoked while subscribed.
     */
    val artists: Flowable<List<Artist>>

    /**
     * Deletes a set of tracks identified by their [unique id][Track.id] from the device's external storage.
     * Media files of tracks matching the specified ids will be definitely removed,
     * and any further attempt to access those files via [Track.mediaUri] will fail.
     *
     * If some of the specified ids does not match an existing track then those ids are ignored,
     * deleting only the tracks whose id matches without failing with an exception.
     *
     * As metadata of artists and albums are generated from the track ones, if no track remains
     * for a given album or artist then that album or that artist is also removed.
     *
     * If permission to write to external storage is not granted,
     * this operation will fail with [PermissionDeniedException].
     *
     * @param trackIds The unique identifiers of tracks to be deleted.
     */
    fun deleteTracks(trackIds: LongArray): Completable
}