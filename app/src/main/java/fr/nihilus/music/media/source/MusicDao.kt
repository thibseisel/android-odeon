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
 * are available at a later time through callbacks.
 *
 * To improve subsequent loading times, implementations may implement its own caching logic.
 * If that's the case, they are also responsible for refreshing cached data whenever metadata
 * has changed and notify clients.
 */
interface MusicDao {

    /**
     * Retrieve all tracks from a given datastore.
     * See [findTrack] for a list of metadata properties that implementations
     * should define for each track.
     * @return an observable that emits a list of all tracks
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
     * - [MusicDao.CUSTOM_META_TITLE_KEY]
     * - [MusicDao.CUSTOM_META_ALBUM_ID]
     * - [MusicDao.CUSTOM_META_ARTIST_ID]
     *
     * The returned `Maybe` may complete without returning a value, indicating that the
     * requested track does not exist on this implementation's data store.
     *
     * @param musicId the unique identifier of the track to retrieve
     * @return an observable that emits the requested item
     * or completes without emitting if it does not exist.
     */
    fun findTrack(musicId: String): Maybe<MediaMetadataCompat>

    /**
     * Return an observable dataset of albums featuring music stored on this device.
     *
     * Each album is composed of :
     * - a media id
     * - a title
     * - a subtitle, which is the name of the artist that composed it
     * - a content URI pointing to the album art
     * - the year at which it was released ([MediaItems.EXTRA_YEAR])
     * - the number of songs it featured ([MediaItems.EXTRA_NUMBER_OF_TRACKS])
     * - a key used for alphabetic sorting ([MediaItems.EXTRA_TITLE_KEY]).
     *
     * Albums are sorted by name by default.
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
        const val CUSTOM_META_TITLE_KEY = "title_key"
        const val CUSTOM_META_ALBUM_ID = "album_id"
        const val CUSTOM_META_ARTIST_ID = "artist_id"
        const val METADATA_DATE_ADDED = "fr.nihilus.music.DATE_ADDED"
    }
}