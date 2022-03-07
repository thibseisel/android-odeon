/*
 * Copyright 2021 Thibault Seisel
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

package fr.nihilus.music.service.browser

import fr.nihilus.music.core.database.playlists.Playlist
import fr.nihilus.music.core.database.playlists.PlaylistDao
import fr.nihilus.music.core.database.playlists.PlaylistTrack
import fr.nihilus.music.core.database.spotify.TrackFeature
import fr.nihilus.music.core.test.coroutines.flow.infiniteFlowOf
import fr.nihilus.music.core.test.stub
import fr.nihilus.music.media.provider.*
import fr.nihilus.music.media.usage.DisposableTrack
import fr.nihilus.music.media.usage.UsageManager
import fr.nihilus.music.spotify.manager.FeatureFilter
import fr.nihilus.music.spotify.manager.SpotifyManager
import fr.nihilus.music.spotify.manager.SyncProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit

internal class TestMediaDao(
    artists: List<Artist> = SAMPLE_ARTISTS,
    albums: List<Album> = SAMPLE_ALBUMS,
    tracks: List<Track> = SAMPLE_TRACKS
) : MediaDao {

    override val artists: Flow<List<Artist>> = initialEventFlow(artists)
    override val albums: Flow<List<Album>> = initialEventFlow(albums)
    override val tracks: Flow<List<Track>> = initialEventFlow(tracks)

    override suspend fun deleteTracks(ids: LongArray): DeleteTracksResult = stub()

    private fun <T> initialEventFlow(initialValue: T) = flow {
        emit(initialValue)
        suspendCancellableCoroutine<Nothing> {}
    }
}

internal class TestPlaylistDao(
    override val playlists: Flow<List<Playlist>> = infiniteFlowOf(SAMPLE_PLAYLISTS),
    private val playlistMembers: List<PlaylistTrack> = SAMPLE_MEMBERS
) : PlaylistDao() {

    override fun getPlaylistTracks(playlistId: Long): Flow<List<PlaylistTrack>> = flow {
        emit(playlistMembers.filter { it.playlistId == playlistId })
        suspendCancellableCoroutine<Nothing> {}
    }

    override suspend fun findPlaylist(playlistId: Long): Playlist? = stub()
    override suspend fun savePlaylist(playlist: Playlist): Long = stub()
    override suspend fun addTracks(tracks: List<PlaylistTrack>): Unit = stub()
    override suspend fun deletePlaylist(playlistId: Long): Unit = stub()
    override suspend fun deletePlaylistTracks(trackIds: LongArray): Unit = stub()
}

internal class TestUsageManager(
    private val mostRatedTracks: List<Track> = SAMPLE_MOST_RATED_TRACKS,
    private val disposableTracks: List<DisposableTrack> = emptyList()
) : UsageManager {
    override fun getMostRatedTracks() = infiniteFlowOf(mostRatedTracks)
    override fun getPopularTracksSince(period: Long, unit: TimeUnit) = infiniteFlowOf(mostRatedTracks)
    override fun getDisposableTracks() = infiniteFlowOf(disposableTracks)
    override suspend fun reportCompletion(trackId: Long) = stub()
}

internal class TestSpotifyManager(
    private val trackFeatures: List<Pair<Track, TrackFeature>> = emptyList()
) : SpotifyManager {

    override suspend fun findTracksHavingFeatures(filters: List<FeatureFilter>): List<Pair<Track, TrackFeature>> {
        return trackFeatures.filter { (_, features) ->
            filters.all { filter -> filter.matches(features) }
        }
    }

    override suspend fun listUnlinkedTracks(): List<Track> = stub()

    override fun sync(): Flow<SyncProgress> = stub()

}

internal object StubUsageManager : UsageManager {
    override fun getMostRatedTracks(): Flow<List<Track>> = stub()
    override fun getPopularTracksSince(period: Long, unit: TimeUnit) = stub()
    override fun getDisposableTracks(): Flow<List<DisposableTrack>> = stub()
    override suspend fun reportCompletion(trackId: Long) = stub()
}

internal object StubSpotifyManager : SpotifyManager {

    override suspend fun findTracksHavingFeatures(
        filters: List<FeatureFilter>
    ): List<Pair<Track, TrackFeature>> = stub()

    override suspend fun listUnlinkedTracks(): List<Track> = stub()

    override fun sync(): Flow<SyncProgress> = stub()
}

internal object StubPlaylistDao : PlaylistDao() {
    override val playlists: Flow<List<Playlist>> get() = stub()
    override suspend fun findPlaylist(playlistId: Long): Playlist? = stub()
    override fun getPlaylistTracks(playlistId: Long): Flow<List<PlaylistTrack>> = stub()
    override suspend fun savePlaylist(playlist: Playlist): Long = stub()
    override suspend fun addTracks(tracks: List<PlaylistTrack>): Unit = stub()
    override suspend fun deletePlaylist(playlistId: Long): Unit = stub()
    override suspend fun deletePlaylistTracks(trackIds: LongArray): Unit = stub()
}
