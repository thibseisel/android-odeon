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

import android.Manifest
import fr.nihilus.music.common.os.PermissionDeniedException

/**
 * A test double for [MediaProvider] that simulates the behavior of the Android MediaStore
 * for unit and integration tests without actually querying a database.
 *
 * @constructor Create a stub provider with pre-programmed media lists.
 *
 * @param tracks The set of tracks to be returned by [queryTracks]. Defaults to sample tracks.
 * @param albums The set of albums to be returned by [queryAlbums]. Defaults to sample albums.
 * @param artists The set of artists to be returned by [queryArtists]. Defaults to sample artists.
 */
internal class TestMediaProvider(
    private var tracks: List<Track> = SAMPLE_TRACKS,
    private var albums: List<Album> = SAMPLE_ALBUMS,
    private var artists: List<Artist> = SAMPLE_ARTISTS
) : MediaProvider {
    private val _observers = mutableSetOf<MediaProvider.Observer>()

    /**
     * Whether this provider has permission to access external storage.
     * When set to `false`, any simulated read/write operation will throw [PermissionDeniedException].
     * The default is `true`.
     */
    var hasStoragePermission: Boolean = true

    /**
     * A view of the observers that are actually registered.
     */
    val registeredObservers: Set<MediaProvider.Observer>
        get() = _observers

    override fun queryTracks(): List<Track> =
        if (hasStoragePermission) tracks else throw PermissionDeniedException(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

    override fun queryAlbums(): List<Album> =
        if (hasStoragePermission) albums else throw PermissionDeniedException(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

    override fun queryArtists(): List<Artist> =
        if (hasStoragePermission) artists else throw PermissionDeniedException(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

    override fun deleteTracks(trackIds: LongArray): Int {
        if (!hasStoragePermission) {
            throw PermissionDeniedException(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        // Filter out tracks whose id matches
        val remainingTracks = tracks.filterNot { it.id in trackIds }

        val deletedTrackCount = tracks.size - remainingTracks.size
        if (deletedTrackCount > 0) {
            // Notify that the track list changed.
            tracks = remainingTracks
            notifyChange(MediaProvider.MediaType.TRACKS)

            // If no track remains for some albums, delete those albums.
            val tracksPerAlbum = remainingTracks.groupingBy { it.albumId }.eachCount()
            val remainingAlbums = albums.filter { tracksPerAlbum.containsKey(it.id) }
            if (albums.size - remainingAlbums.size > 0) {
                albums = remainingAlbums
                notifyChange(MediaProvider.MediaType.ALBUMS)
            }

            // if no tracks remains for some artists, delete those artists.
            val tracksPerArtist = remainingTracks.groupingBy { it.artistId }.eachCount()
            val remainingArtists = artists.filter { tracksPerArtist.containsKey(it.id) }
            if (artists.size - remainingArtists.size > 0) {
                artists = remainingArtists
                notifyChange(MediaProvider.MediaType.ARTISTS)
            }
        }

        return deletedTrackCount
    }

    override fun registerObserver(observer: MediaProvider.Observer) {
        check(observer !in _observers) { "Observer is already registered." }
        _observers += observer
    }

    override fun unregisterObserver(observer: MediaProvider.Observer) {
        _observers -= observer
    }

    /**
     * Update the set of tracks available through this provider,
     * notifying registered observers of the change.
     */
    fun notifyTracksChanged(newTracks: List<Track>) {
        tracks = newTracks
        notifyChange(MediaProvider.MediaType.TRACKS)
    }

    /**
     * Update the set of albums available through this provider,
     * notifying registered observers of the change.
     */
    fun notifyAlbumsChanged(newAlbums: List<Album>) {
        albums = newAlbums
        notifyChange(MediaProvider.MediaType.ALBUMS)
    }

    /**
     * Update the set of artists available through this provider,
     * notifying registered observers of the change.
     */
    fun notifyArtistsChanged(newArtists: List<Artist>) {
        artists = newArtists
        notifyChange(MediaProvider.MediaType.ARTISTS)
    }

    /**
     * Notify registered observers that the list of media of the given [type] has changed.
     * This simulates media updates triggered by
     */
    fun notifyChange(type: MediaProvider.MediaType) {
        for (observer in registeredObservers) {
            if (observer.type == type) {
                observer.onChanged()
            }
        }
    }
}