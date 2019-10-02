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

import fr.nihilus.music.common.collections.diffList
import fr.nihilus.music.database.playlists.Playlist
import fr.nihilus.music.database.playlists.PlaylistDao
import fr.nihilus.music.database.usage.UsageDao
import fr.nihilus.music.media.di.ServiceScoped
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.provider.Track
import io.reactivex.Flowable
import io.reactivex.processors.PublishProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.consume
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.broadcastIn
import kotlinx.coroutines.flow.scanReduce
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject

@ServiceScoped
internal class MediaRepositoryImpl
@Inject constructor(
    scope: CoroutineScope,
    private val mediaDao: MediaDao,
    private val playlistsDao: PlaylistDao,
    private val usageDao: UsageDao
) : MediaRepository {

    private val scope = CoroutineScope(scope.coroutineContext + SupervisorJob())
    private val _mediaChanges = PublishProcessor.create<ChangeNotification>()

    @Volatile private var tracksCache = trackSyncCache()
    @Volatile private var albumsCache = albumSyncCache()
    @Volatile private var artistsCache = artistSyncCache()
    @Volatile private var playlistsCache = playlistSyncCache()

    override suspend fun getTracks(): List<Track> {
        if (tracksCache.isClosedForSend) {
            tracksCache = trackSyncCache()
        }

        return request(tracksCache)
    }

    override suspend fun getAlbums(): List<Album> {
        if (albumsCache.isClosedForSend) {
            albumsCache = albumSyncCache()
        }

        return request(albumsCache)
    }

    override suspend fun getArtists(): List<Artist> {
        if (artistsCache.isClosedForSend) {
            artistsCache = artistSyncCache()
        }

        return request(artistsCache)
    }

    override suspend fun getPlaylists(): List<Playlist> {
        if (playlistsCache.isClosedForSend) {
            playlistsCache = playlistSyncCache()
        }

        return request(playlistsCache)
    }

    override suspend fun getPlaylistTracks(playlistId: Long): List<Track>? {
        val allTracks = getTracks()
        val playlistMembers = playlistsDao.getPlaylistTracks(playlistId)

        val tracksById = allTracks.associateBy(Track::id)
        return playlistMembers.mapNotNull { tracksById[it.trackId] }.takeUnless { it.isEmpty() }
    }

    override val changeNotifications: Flowable<ChangeNotification>
        get() = _mediaChanges.onBackpressureBuffer()

    private suspend fun <T> request(cache: BroadcastChannel<List<T>>): List<T> = cache.consume {
        receive()
    }

    private fun trackSyncCache() = mediaDao.tracks.asFlow().cacheMedia { original, modified ->
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
        val affectedPlaylistIds = playlistsDao.getPlaylistsHavingTracks(deletedTrackIds)
        playlistsDao.deletePlaylistTracks(deletedTrackIds)
        usageDao.deleteEventsForTracks(deletedTrackIds)

        affectedPlaylistIds.forEach {
            _mediaChanges.onNext(ChangeNotification.Playlist(it))
        }
    }

    private fun albumSyncCache() = mediaDao.albums.asFlow().cacheMedia { original, modified ->
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

    private fun artistSyncCache() = mediaDao.artists.asFlow().cacheMedia { _, _ ->
        _mediaChanges.onNext(ChangeNotification.AllArtists)
    }

    private fun playlistSyncCache() = playlistsDao.playlists.cacheMedia { _, _ ->
        _mediaChanges.onNext(ChangeNotification.AllPlaylists)
    }

    private fun <M : Any> Flow<List<M>>.cacheMedia(
        onChanged: suspend (original: List<M>, modified: List<M>) -> Unit
    ): BroadcastChannel<List<M>> =
        scanReduce { original, modified ->
            onChanged(original, modified)
            modified
        }
        .broadcastIn(scope)
}