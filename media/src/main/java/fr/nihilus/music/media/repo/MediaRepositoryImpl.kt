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

import dagger.Reusable
import fr.nihilus.music.core.collections.diffList
import fr.nihilus.music.core.database.playlists.Playlist
import fr.nihilus.music.core.database.playlists.PlaylistDao
import fr.nihilus.music.core.database.playlists.PlaylistTrack
import fr.nihilus.music.core.database.usage.UsageDao
import fr.nihilus.music.core.os.PermissionDeniedException
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.provider.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.selects.select
import javax.inject.Inject
import kotlinx.coroutines.channels.onReceiveOrNull as onReceiveOrNullExt

@Reusable
internal class MediaRepositoryImpl
@Inject constructor(
    private val scope: CoroutineScope,
    private val mediaDao: MediaDao,
    private val playlistsDao: PlaylistDao,
    private val usageDao: UsageDao
) : MediaRepository {

    private val _mediaChanges = BroadcastChannel<ChangeNotification>(Channel.BUFFERED)

    @Volatile private var tracksCache = scope.trackSyncCache()
    @Volatile private var albumsCache = scope.albumSyncCache()
    @Volatile private var artistsCache = scope.artistSyncCache()
    @Volatile private var playlistsCache = scope.playlistSyncCache()

    override suspend fun getTracks(): List<Track> {
        if (tracksCache.isClosedForSend) {
            tracksCache = scope.trackSyncCache()
        }

        return request(tracksCache)
    }

    override suspend fun getAlbums(): List<Album> {
        if (albumsCache.isClosedForSend) {
            albumsCache = scope.albumSyncCache()
        }

        return request(albumsCache)
    }

    override suspend fun getArtists(): List<Artist> {
        if (artistsCache.isClosedForSend) {
            artistsCache = scope.artistSyncCache()
        }

        return request(artistsCache)
    }

    override suspend fun getPlaylists(): List<Playlist> {
        if (playlistsCache.isClosedForSend) {
            playlistsCache = scope.playlistSyncCache()
        }

        return request(playlistsCache)
    }

    override suspend fun getPlaylistTracks(playlistId: Long): List<Track>? {
        val allTracks = getTracks()
        val playlistMembers = playlistsDao.getPlaylistTracks(playlistId)

        val tracksById = allTracks.associateBy(Track::id)
        return playlistMembers.mapNotNull { tracksById[it.trackId] }.takeUnless { it.isEmpty() }
    }

    override suspend fun createPlaylist(newPlaylist: Playlist, trackIds: LongArray) {
        playlistsDao.createPlaylist(newPlaylist, trackIds)
    }

    override suspend fun addTracksToPlaylist(playlistId: Long, trackIds: LongArray) {
        val tracks = trackIds.map { trackId -> PlaylistTrack(playlistId, trackId) }
        playlistsDao.addTracks(tracks)
    }

    override suspend fun deletePlaylist(playlistId: Long) = playlistsDao.deletePlaylist(playlistId)

    override suspend fun deleteTracks(trackIds: LongArray): Int = mediaDao.deleteTracks(trackIds)

    override val changeNotifications: Flow<ChangeNotification>
        get() = _mediaChanges.asFlow()

    private suspend fun <T> request(cache: SendChannel<CompletableDeferred<List<T>>>): List<T> {
        val mediaRequest = CompletableDeferred<List<T>>()
        cache.send(mediaRequest)
        return mediaRequest.await()
    }

    private fun CoroutineScope.trackSyncCache() = syncCache(mediaDao.tracks) { original, modified ->
        // Dispatch update notifications to downstream
        _mediaChanges.send(ChangeNotification.AllTracks)

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
            _mediaChanges.send(ChangeNotification.Album(it))
        }

        modifiedArtistIds.forEach {
            _mediaChanges.send(ChangeNotification.Artist(it))
        }

        val deletedTrackIds = LongArray(deleted.size) { deleted[it].id }
        val affectedPlaylistIds = playlistsDao.getPlaylistsHavingTracks(deletedTrackIds)
        playlistsDao.deletePlaylistTracks(deletedTrackIds)
        usageDao.deleteEventsForTracks(deletedTrackIds)

        affectedPlaylistIds.forEach {
            _mediaChanges.send(ChangeNotification.Playlist(it))
        }
    }

    private fun CoroutineScope.albumSyncCache() = syncCache(mediaDao.albums) { original, modified ->
        // Notify that the list of all albums has changed.
        _mediaChanges.send(ChangeNotification.AllAlbums)

        // Calculate the diff between the old and the new album list.
        val (added, deleted) = diffList(original, modified)

        val modifiedArtistIds = mutableSetOf<Long>()
        added.mapTo(modifiedArtistIds, Album::artistId)
        deleted.mapTo(modifiedArtistIds, Album::artistId)

        modifiedArtistIds.forEach { artistId ->
            _mediaChanges.send(ChangeNotification.Artist(artistId))
        }
    }

    private fun CoroutineScope.artistSyncCache() = syncCache(mediaDao.artists) { _, _ ->
        _mediaChanges.send(ChangeNotification.AllArtists)
    }

    private fun CoroutineScope.playlistSyncCache() = syncCache(playlistsDao.playlists) { _, _ ->
        _mediaChanges.send(ChangeNotification.AllPlaylists)
    }

    private fun <M : Any> CoroutineScope.syncCache(
        mediaUpdateStream: Flow<List<M>>,
        onChanged: suspend (original: List<M>, modified: List<M>) -> Unit
    ): SendChannel<CompletableDeferred<List<M>>> = actor(start = CoroutineStart.LAZY) {
        // Wait until media are requested for the first time before observing.
        val firstRequest = receive()

        // Start observing media to a Channel.
        val mediaUpdates = mediaUpdateStream.produceIn(this + SupervisorJob())

        // Cache the last received media list.
        var lastReceivedMediaList: List<M>

        try {
            // Receive the current media list.
            // This fails if permission is denied.
            lastReceivedMediaList = mediaUpdates.receive()

            // Satisfy the first request.
            firstRequest.complete(lastReceivedMediaList)

        } catch (e: PermissionDeniedException) {
            firstRequest.completeExceptionally(e)
            throw CancellationException()
        }

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
                        mediaUpdates.onReceiveOrNullExt().invoke { upToDateMediaList ->
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
}