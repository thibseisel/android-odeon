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

package fr.nihilus.music.media.source

import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import fr.nihilus.music.media.MediaItems
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single

/**
 * Provides information on collections of music items such as songs, albums and artists
 * that are available from a given data store.
 * Depending on the implementation, metadata could be retrieved from the network
 * or the device's storage.
 *
 * Because retrieving data can be long running especially from the network,
 * this API use ReactiveX `Observable` classes to represent metadata as a stream of values that
 * are emitted as they become available through callbacks.
 *
 * To improve subsequent loading times, implementations may implement their own caching logic.
 * If that's the case, they are also responsible for refreshing cached data whenever metadata
 * has changed and notify clients.
 */
interface MusicDao {

    /**
     * Retrieve songs from this data store.
     * Each song must be described by a set of metadata.
     * See [findTrack] for a list of metadata properties that implementations
     * should define for each track.
     *
     * Clients may filter results with criteria based on the value of media metadata keys.
     * For example, the criterion `MediaMetadataCompat.METADATA_KEY_ALBUM -> "Thriller"`
     * returns all songs whose album name matches exactly "Thriller".
     *
     * They may also sort results on one or more metadata key by specifying a sorting clause
     * of the given format `KEY_1 (ASC | DESC), KEY_2 (ASC | DESC), ..., KEY_N (ASC | DESC)`
     * where `KEY_N` is a metadata key and `(ASC | DESC)` the optional order in which results
     * are sorted: ascending or descending (if absent, it is assumed ascending).
     *
     * @param criteria A map whose keys are standard media metadata key from
     * `MediaMetadataCompat.METADATA_KEY_*` or custom `MusicDao.METADATA_KEY_*` ones,
     * and whose values must be matched exactly by songs. If `null`, all songs are returned.
     *
     * @param sorting The sorting clause that defines the order in which songs are emitted.
     * If `null`, implementations should use a default sort order they define.
     *
     * @return a stream of songs that match the specified criteria and emitted in order.
     *
     * @throws UnsupportedOperationException If a metadata key that can't be used
     * for filtering or sorting is specified.
     */
    fun getTracks(criteria: Map<String, Any>?, sorting: String?): Observable<MediaMetadataCompat>

    /**
     * Retrieve a single track's metadata from this implementation's data store.
     *
     * Every track should have an unique id allowing clients to pick specific tracks to compose
     * their own playlist.
     * Implementations **must** also define the following properties for each track:
     * - [MediaMetadataCompat.METADATA_KEY_MEDIA_ID]
     * - [MediaMetadataCompat.METADATA_KEY_TITLE]
     * - [MediaMetadataCompat.METADATA_KEY_ALBUM]
     * - [MediaMetadataCompat.METADATA_KEY_ARTIST]
     * - [MediaMetadataCompat.METADATA_KEY_DURATION]
     * - [MediaMetadataCompat.METADATA_KEY_DISC_NUMBER]
     * - [MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER]
     * - [MediaMetadataCompat.METADATA_KEY_MEDIA_URI]
     *
     * The following properties are optional:
     * - [MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI]
     * - [MusicDao.METADATA_KEY_TITLE_KEY]
     * - [MusicDao.METADATA_KEY_ALBUM_ID]
     * - [MusicDao.METADATA_KEY_ARTIST_ID]
     *
     * The returned `Maybe` may complete without returning a value, indicating that the
     * requested track does not exist on this data store.
     *
     * @param musicId the unique identifier of the track to retrieve.
     * The format depends on implementation.
     *
     * @return an observable that emits the requested track
     * or completes without emitting if no track matches the provided id.
     */
    fun findTrack(musicId: String): Maybe<MediaMetadataCompat>

    /**
     * Retrieve albums from this data store.
     *
     * Each album should be composed of :
     * - a media id
     * - a title
     * - a subtitle, which may be the name of the artist that composed it
     * - an URI pointing to the album art
     * - the year at which it was released ([MediaItems.EXTRA_YEAR])
     * - the number of songs it featured ([MediaItems.EXTRA_NUMBER_OF_TRACKS])
     * - a key used for alphabetic sorting ([MediaItems.EXTRA_TITLE_KEY]).
     *
     *
     *
     */
    fun getAlbums(criteria: Map<String, Any>?, sorting: String?): Observable<MediaDescriptionCompat>

    /**
     * Return an observable dataset of artists that participated to composing
     * music stored on this device.
     *
     * Each artist is composed of :
     * - a media id
     * - its name
     * - a content URI pointing to the album art of the most recent album
     * - the number of songs it composed ([MediaItems.EXTRA_NUMBER_OF_TRACKS])
     * - a key used for alphabeting sorting ([MediaItems.EXTRA_TITLE_KEY]).
     *
     * Artists are sorted by name by default.
     */
    fun getArtists(): Observable<List<MediaDescriptionCompat>>

    /**
     * Searches for tracks metadata that matches a given query.
     */
    fun search(query: String?, extras: Bundle?): Single<List<MediaMetadataCompat>>

    /**
     * Delete the track with the specified [trackId] from this implementation's data store.
     * If no track exists with this id, the operation will terminate without an error.
     *
     * @return The deferred deletion task.
     */
    fun deleteTrack(trackId: String): Completable

    companion object {
        const val METADATA_KEY_TITLE_KEY = "fr.nihilus.music.TITLE_KEY"
        const val METADATA_KEY_ALBUM_ID = "fr.nihilus.music.ALBUM_ID"
        const val METADATA_KEY_ARTIST_ID = "fr.nihilus.music.ARTIST_ID"
        const val METADATA_KEY_DATE_ADDED = "fr.nihilus.music.DATE_ADDED"
    }
}