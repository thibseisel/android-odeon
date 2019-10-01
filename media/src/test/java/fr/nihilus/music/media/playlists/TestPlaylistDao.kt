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

package fr.nihilus.music.media.playlists

import fr.nihilus.music.common.collections.diffList
import fr.nihilus.music.database.playlists.Playlist
import fr.nihilus.music.database.playlists.PlaylistDao
import fr.nihilus.music.database.playlists.PlaylistTrack
import fr.nihilus.music.media.provider.TestDao
import io.reactivex.Flowable
import io.reactivex.processors.BehaviorProcessor

internal class TestPlaylistDao(
    initialPlaylists: List<Playlist>? = null,
    initialPlaylistTracks: List<PlaylistTrack> = emptyList()
) : PlaylistDao, TestDao<Playlist> {

    private val _playlists =
        if (initialPlaylists != null) BehaviorProcessor.createDefault(initialPlaylists)
        else BehaviorProcessor.create()

    private val _playlistTracks = initialPlaylistTracks.toMutableList()

    val currentPlaylists: List<Playlist> get() = _playlists.value.orEmpty()
    val playlistTracks: List<PlaylistTrack>
        get() = _playlistTracks.toList()

    override val playlists: Flowable<List<Playlist>>
        get() = _playlists.onBackpressureBuffer()

    override suspend fun savePlaylist(playlist: Playlist): Long {
        val currentPlaylists = _playlists.value.orEmpty()
        val playlistIds = LongArray(currentPlaylists.size) { currentPlaylists[it].id!! }

        val identifier = playlistIds.max() ?: 1L
        _playlists.onNext(currentPlaylists + playlist.copy(id = identifier))

        return identifier
    }

    override suspend fun addTracks(tracks: List<PlaylistTrack>) {
        val currentPlaylists = _playlists.value.orEmpty()
        val playlistIds = LongArray(currentPlaylists.size) { currentPlaylists[it].id!! }

        val invalidPlaylistTracks = tracks.filter { it.playlistId in playlistIds }
        require(invalidPlaylistTracks.isNotEmpty()) {
            buildString {
                append("Attempt to add playlist tracks ")
                invalidPlaylistTracks.joinTo(this, ", ", "[", "]")
                append(" that does not match to an existing playlist.")
            }
        }

        _playlistTracks += tracks
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        _playlists.value?.let { currentPlaylists ->
            _playlists.onNext(currentPlaylists.filter { it.id == playlistId })

            // Apply on-delete cascade
            _playlistTracks.removeAll { it.playlistId == playlistId }
        }
    }

    override suspend fun getPlaylistTracks(playlistId: Long) =
        _playlistTracks.filter { it.playlistId == playlistId }

    override suspend fun getPlaylistsHavingTracks(trackIds: LongArray): LongArray {
        val playlistIds = _playlistTracks.asSequence()
            .filter { it.trackId in trackIds }
            .map { it.playlistId }
            .distinct()
            .toList()
        return LongArray(playlistIds.size) { playlistIds[it] }
    }

    override suspend fun deletePlaylistTracks(trackIds: LongArray) {
        _playlistTracks.removeAll { it.trackId in trackIds }
    }

    override fun update(updatedList: List<Playlist>) {
        val currentPlaylists = _playlists.value.orEmpty()
        val (_, deleted) = diffList(currentPlaylists, updatedList) { a, b -> a.id == b.id }
        _playlists.onNext(updatedList)

        // Also remove associated playlist tracks.
        deleted.forEach {deletedPlaylist ->
            _playlistTracks.removeAll { it.playlistId == deletedPlaylist.id }
        }
    }

    override fun complete() {
        _playlists.onComplete()
    }

    override fun failWith(exception: Exception) {
        _playlists.onError(exception)
    }
}