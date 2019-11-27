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

import fr.nihilus.music.core.os.PermissionDeniedException

/**
 * Interface to perform read and write operations on music media stored locally on the device.
 * This acts as an abstraction layer over the Android MediaStore.
 *
 * Because accessing the external storage requires a runtime permission that can be revoked at anytime,
 * all operations may fail if permission to read/write external storage is denied.
 */
internal interface MediaProvider {

    /**
     * Loads metadata of all tracks that are stored on the device's external storage.
     * Each track metadata corresponds to a media file whose location can be resolved using its [content uri][Track.mediaUri]
     * and is identified by a unique [track identifier][Track.id], an [album][Track.albumId] and an [artist][Track.artistId].
     *
     * @return A list of tracks sorted alphabetically by title, ignoring common english prefixes such as "the", "an" and "a".
     * @throws PermissionDeniedException If permission to read external storage is not granted.
     */
    suspend fun queryTracks(): List<Track>

    /**
     * Loads metadata of all albums whose tracks are stored on the device's external storage.
     * Albums are identified by a unique [album identifier][Album.id] and an [artist][Album.artistId].
     *
     * Unlike tracks, albums does not map directly to a file and are generated from the metadata of existing tracks.
     *
     * @return A list of albums sorted alphabetically by title, ignoring common english prefixes such as "the", "an" and "a".
     * @throws PermissionDeniedException If permission to read external storage is not granted.
     */
    suspend fun queryAlbums(): List<Album>

    /**
     * Load metadata of all artists that produced tracks stored on the device's external storage.
     * Artists are identified by a unique [artist identifier][Artist.id].
     * Unlike tracks, artists does not map directly to a file and are generated from the metadata of existing tracks.
     *
     * @return A list of artists sorted alphabetically by name, ignoring common english prefixes such as "the", "an" and "a".
     * @throws PermissionDeniedException If permission to read external storage is not granted.
     */
    suspend fun queryArtists(): List<Artist>

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
     * @param trackIds The unique identifiers of tracks to be deleted.
     * @return The number of tracks that have been deleted.
     * @throws PermissionDeniedException If permission to write to external storage is not granted.
     */
    suspend fun deleteTracks(trackIds: LongArray): Int

    /**
     * Starts observing changes to the list of media.
     * The registered [observer] will be notified for each change to the list returned by [queryTracks],
     * [queryAlbums] or [queryArtists], depending on the [type][Observer.type] of media it observes.
     * Media lists queried before a change notification is dispatched to an observer are then considered outdated
     * and should be re-queried.
     *
     * Observers should be unregistered via [unregisterObserver] after use.
     * The same observer can only be registered once.
     *
     * @param observer An observer of the desired type to be notified of changes.
     * @throws IllegalStateException If the specified observer has already been registered.
     */
    fun registerObserver(observer: Observer)

    /**
     * Stops dispatching changes to media for the given [observer].
     * That observer's [Observer.onChanged] will no longer be called.
     * If that observer had not been registered with [registerObserver] this does nothing.
     *
     * @param observer An observer that has been previously registered with [registerObserver].
     */
    fun unregisterObserver(observer: Observer)

    /**
     * A receiver for media list change events.
     * An observer can only be notified for changes of one given type at the time.
     *
     * @param type The type of media this observer should be notified for.
     */
    abstract class Observer(val type: MediaType) {

        /**
         * Called immediately after a change to the list of media of the desired [type] occurs.
         * This is only called if this observer has been registered via [MediaProvider.registerObserver].
         */
        abstract fun onChanged()
    }

    /**
     * Enumeration of the types of media that can be queried from a [MediaProvider].
     */
    enum class MediaType {

        /**
         * The type of media that can be queried from [MediaProvider.queryTracks].
         * Maps to the [Track] class.
         */
        TRACKS,
        /**
         * The type of media that can be queried from [MediaProvider.queryAlbums].
         * Maps to the [Album] class.
         */
        ALBUMS,
        /**
         * The type of media that can e queried from [MediaProvider.queryArtists].
         * Maps to the [Artist] class.
         */
        ARTISTS
    }
}