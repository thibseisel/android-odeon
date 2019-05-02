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

package fr.nihilus.music.media.repo

import fr.nihilus.music.media.playlists.Playlist
import fr.nihilus.music.media.playlists.PlaylistDao
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.RxMediaDao
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.media.usage.MediaUsageDao
import fr.nihilus.music.media.utils.diffList
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.reactive.openSubscription
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.selects.select

private fun <M : Any> CoroutineScope.syncCache(
    mediaUpdateStream: Flowable<List<M>>,
    onChanged: suspend (original: List<M>, modified: List<M>) -> Unit
): SendChannel<CompletableDeferred<List<M>>> = actor(start = CoroutineStart.LAZY) {
    // Wait until media are requested for the first time before observing.
    val firstRequest = receive()

    // Start observing media to a Channel.
    val mediaUpdates = mediaUpdateStream.openSubscription()
    // Cache the last received media list. Load it for the first time.
    var lastReceivedMediaList = mediaUpdates.receive()

    // Satisfy the first request.
    firstRequest.complete(lastReceivedMediaList)

    try {
        // Process incoming messages until scope is cancelled...
        while (isActive) {
            select<Unit> {
                // When receiving a request to load media from cache...
                onReceive { request ->
                    // Satisfy it immediately.
                    request.complete(lastReceivedMediaList)
                }

                // When receiving an up to date media list...
                if (!mediaUpdates.isClosedForReceive) {
                    mediaUpdates.onReceiveOrNull { upToDateMediaList ->
                        if (upToDateMediaList != null) {
                            // Replace the cached list and notify for the change.
                            val oldMediaList = lastReceivedMediaList
                            lastReceivedMediaList = upToDateMediaList

                            onChanged(oldMediaList, upToDateMediaList)
                        }
                    }
                }
            }
        }

    } finally {
        mediaUpdates.cancel()
    }
}

internal class MediaRepositoryImpl(
    scope: CoroutineScope,
    private val mediaDao: RxMediaDao,
    private val playlistsDao: PlaylistDao,
    private val usageDao: MediaUsageDao
) : MediaRepository {

    private val _mediaChanges = PublishProcessor.create<ChangeNotification>()

    private val tracksCache = scope.syncCache(mediaDao.tracks) { original, modified ->
        // Dispatch update notifications to downstream
        _mediaChanges.onNext(ChangeNotification.AllTracks)

        // Calculate the diff between the old and the new track list.
        val (added, deleted) = diffList(original, modified)
        val modifiedAlbumIds = mutableSetOf<Long>()
        val modifiedArtistIds = mutableSetOf<Long>()

        // Dispatch an update for each album and artist affected by the change.
        added.mapTo(modifiedAlbumIds, Track::albumId)
        added.mapTo(modifiedArtistIds, Track::artistId)
        deleted.mapTo(modifiedAlbumIds, Track::albumId)
        deleted.mapTo(modifiedArtistIds, Track::artistId)

        modifiedAlbumIds.forEach {
            _mediaChanges.onNext(ChangeNotification.Album(it))
        }

        modifiedArtistIds.forEach {
            _mediaChanges.onNext(ChangeNotification.Artist(it))
        }

        val deletedTrackIds = LongArray(deleted.size) { deleted[it].id }
        val affectedPlaylistIds = playlistsDao.getPlaylistsHavingTracks(deletedTrackIds).await()

        withContext(Dispatchers.IO) {
            playlistsDao.deletePlaylistTracks(deletedTrackIds)
        }

        affectedPlaylistIds.forEach {
            _mediaChanges.onNext(ChangeNotification.Playlist(it))
        }
    }

    private val albumsCache = scope.syncCache(mediaDao.albums) { original, modified ->
        // Notify that the list of all albums has changed.
        _mediaChanges.onNext(ChangeNotification.AllAlbums)

        // Calculate the diff between the old and the new album list.
        val (added, deleted) = diffList(original, modified)

        val modifiedArtistIds = mutableSetOf<Long>()
        added.mapTo(modifiedArtistIds, Album::artistId)
        deleted.mapTo(modifiedArtistIds, Album::artistId)

        modifiedArtistIds.forEach { artistId ->
            _mediaChanges.onNext(ChangeNotification.Artist(artistId))
        }
    }

    private val artistsCache = scope.syncCache(mediaDao.artists) { _, _ ->
        _mediaChanges.onNext(ChangeNotification.AllArtists)
    }

    private val playlistsCache = scope.syncCache(playlistsDao.playlistsFlow) { _, _ ->
        _mediaChanges.onNext(ChangeNotification.AllPlaylists)
    }

    override suspend fun getAllTracks(): List<Track> = request(tracksCache)

    override suspend fun getAllAlbums(): List<Album> = request(albumsCache)

    override suspend fun getAllArtists(): List<Artist> = request(artistsCache)

    override suspend fun getAllPlaylists(): List<Playlist> = request(playlistsCache)

    override suspend fun getPlaylistTracks(playlistId: Long): List<Track>? {
        val allTracks = request(tracksCache)
        val playlistMembers = playlistsDao.getPlaylistTracks(playlistId).await()

        val tracksById = allTracks.associateBy(Track::id)
        return playlistMembers.mapNotNull { tracksById[it.trackId] }.takeUnless { it.isEmpty() }
    }

    override suspend fun getMostRatedTracks(): List<Track> {
        val tracksById = request(tracksCache).associateBy { it.id }
        val trackScores = withContext(Dispatchers.IO) {
            usageDao.getMostRatedTracks(25)
        }

        return trackScores.mapNotNull { tracksById[it.trackId] }
    }

    override val changeNotifications: Flowable<ChangeNotification>
        get() = _mediaChanges.onBackpressureBuffer()

    private suspend fun <T> request(cache: SendChannel<CompletableDeferred<List<T>>>): List<T> {
        val mediaRequest = CompletableDeferred<List<T>>()
        cache.send(mediaRequest)
        return mediaRequest.await()
    }
}