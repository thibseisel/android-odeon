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

package fr.nihilus.music.service.browser

import fr.nihilus.music.core.database.playlists.Playlist
import fr.nihilus.music.core.test.stub
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.media.repo.ChangeNotification
import fr.nihilus.music.media.repo.MediaRepository
import fr.nihilus.music.media.usage.DisposableTrack
import fr.nihilus.music.media.usage.UsageManager
import io.reactivex.Flowable
import java.util.concurrent.TimeUnit

internal class TestMediaRepository(
    private val tracks: List<Track> = SAMPLE_TRACKS,
    private val albums: List<Album> = SAMPLE_ALBUMS,
    private val artists: List<Artist> = SAMPLE_ARTISTS,
    private val playlists: List<Playlist> = SAMPLE_PLAYLISTS,
    private val tracksPerPlaylist: Map<Long, List<Track>> = SAMPLE_TRACKS_FOR_PLAYLIST,
    override val changeNotifications: Flowable<ChangeNotification> = Flowable.empty()
) : MediaRepository {
    override suspend fun getTracks(): List<Track> = tracks
    override suspend fun getAlbums(): List<Album> = albums
    override suspend fun getArtists(): List<Artist> = artists
    override suspend fun getPlaylists(): List<Playlist> = playlists
    override suspend fun getPlaylistTracks(playlistId: Long): List<Track>? = tracksPerPlaylist[playlistId]
    override suspend fun createPlaylist(newPlaylist: Playlist, trackIds: LongArray) = stub()
    override suspend fun addTracksToPlaylist(playlistId: Long, trackIds: LongArray) = stub()
    override suspend fun deletePlaylist(playlistId: Long) = stub()
    override suspend fun deleteTracks(trackIds: LongArray): Int = stub()
}

internal class TestUsageManager(
    private val mostRatedTracks: List<Track> = SAMPLE_MOST_RATED_TRACKS,
    private val disposableTracks: List<DisposableTrack> = emptyList()
) : UsageManager {
    override suspend fun getMostRatedTracks(): List<Track> = mostRatedTracks
    override suspend fun getPopularTracksSince(period: Long, unit: TimeUnit) = mostRatedTracks
    override suspend fun getDisposableTracks(): List<DisposableTrack> = disposableTracks
    override fun reportCompletion(trackId: Long) = stub()
}

internal object StubUsageManager : UsageManager {
    override suspend fun getMostRatedTracks(): List<Track> = stub()
    override suspend fun getPopularTracksSince(period: Long, unit: TimeUnit) = stub()
    override suspend fun getDisposableTracks(): List<DisposableTrack> = stub()
    override fun reportCompletion(trackId: Long) = stub()
}

internal class StubMediaRepository : MediaRepository {
    override val changeNotifications: Flowable<ChangeNotification> get() = stub()
    override suspend fun getTracks(): List<Track> = stub()
    override suspend fun getAlbums(): List<Album> = stub()
    override suspend fun getArtists(): List<Artist> = stub()
    override suspend fun getPlaylists(): List<Playlist> = stub()
    override suspend fun getPlaylistTracks(playlistId: Long): List<Track>? = stub()
    override suspend fun createPlaylist(newPlaylist: Playlist, trackIds: LongArray) = stub()
    override suspend fun addTracksToPlaylist(playlistId: Long, trackIds: LongArray) = stub()
    override suspend fun deletePlaylist(playlistId: Long) = stub()
    override suspend fun deleteTracks(trackIds: LongArray): Int = stub()
}