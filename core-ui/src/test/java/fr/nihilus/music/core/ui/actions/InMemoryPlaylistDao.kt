/*
 * Copyright 2020 Thibault Seisel
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

package fr.nihilus.music.core.ui.actions

import fr.nihilus.music.core.database.playlists.Playlist
import fr.nihilus.music.core.database.playlists.PlaylistDao
import fr.nihilus.music.core.database.playlists.PlaylistTrack
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import java.util.*

/**
 * A [PlaylistDao] that stores playlists and their members tracks in memory,
 * simulating the behavior of a real database-backed implementation.
 *
 * @param initialPlaylists The set of playlists that are initially stored.
 * @param initialMembers The set of tracks that are part of the initial playlists.
 */
internal class InMemoryPlaylistDao(
    initialPlaylists: Collection<Playlist> = emptyList(),
    initialMembers: Collection<PlaylistTrack> = emptyList()
) : PlaylistDao() {

    private val _savedPlaylists = TreeSet(compareBy(Playlist::title))
    private val _playlistTracks = mutableSetOf<PlaylistTrack>()

    private val _playlists = ConflatedBroadcastChannel(initialPlaylists.toList())
    private val _members = ConflatedBroadcastChannel(initialMembers.toList())

    /**
     * A readonly view of the currently saved set of playlists.
     */
    val savedPlaylists: List<Playlist>
        get() = _savedPlaylists.toList()

    /**
     * A readonly view of the currently saved list of tracks per playlist.
     */
    val savedTracks: List<PlaylistTrack>
        get() = _playlistTracks.toList()

    override val playlists: Flow<List<Playlist>>
        get() = _playlists.asFlow()

    override fun getPlaylistTracks(playlistId: Long): Flow<List<PlaylistTrack>> {
        return _members.asFlow().map { members ->
            members.filter { it.playlistId == playlistId }
        }
    }

    override suspend fun savePlaylist(playlist: Playlist): Long {
        require(playlist.id == null) { "Unsaved playlists should not have an id" }

        val greatestId = _savedPlaylists.fold(0L) { max, it ->
            it.id?.let { id -> maxOf(max, id) } ?: max
        }

        val assignedId = greatestId + 1L
        _savedPlaylists += playlist.copy(id = assignedId)
        _playlists.offer(_savedPlaylists.toList())

        return assignedId
    }

    override suspend fun addTracks(tracks: List<PlaylistTrack>) {
        for (track in tracks) {
            if (!playlistExists(track.playlistId)) {
                throw IllegalStateException("Attempt to add $track to a non-existing playlist")
            }
        }

        _playlistTracks += tracks
        _members.offer(_playlistTracks.toList())
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        val playlistHasBeenDeleted = _savedPlaylists.removeAll { it.id == playlistId }
        if (playlistHasBeenDeleted) {
            _playlists.offer(_savedPlaylists.toList())
            _playlistTracks.removeAll { it.playlistId == playlistId }
            _members.offer(_playlistTracks.toList())
        }
    }

    override suspend fun deletePlaylistTracks(trackIds: LongArray) {
        val tracksHaveBeenDeleted = _playlistTracks.removeAll { it.trackId in trackIds }
        if (tracksHaveBeenDeleted) {
            _members.offer(_playlistTracks.toList())
        }
    }

    private fun playlistExists(id: Long): Boolean = _savedPlaylists.find { it.id == id } != null
}