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

package fr.nihilus.music.service.browser.provider

import androidx.core.net.toUri
import fr.nihilus.music.core.collections.associateByLong
import fr.nihilus.music.core.database.playlists.PlaylistDao
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.service.AudioTrack
import fr.nihilus.music.service.MediaCategory
import fr.nihilus.music.service.MediaContent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

internal class PlaylistChildrenProvider(
    private val mediaDao: MediaDao,
    private val playlistDao: PlaylistDao
) : ChildrenProvider() {

    override fun findChildren(
        parentId: MediaId
    ): Flow<List<MediaContent>> {
        check(parentId.type == TYPE_PLAYLISTS)

        val playlistId = parentId.category?.toLongOrNull()
        return when {
            playlistId != null -> getPlaylistMembers(playlistId)
            else -> getPlaylists()
        }
    }

    private fun getPlaylists(): Flow<List<MediaContent>> = playlistDao.playlists.map { playlists ->
        playlists.map {
            MediaCategory(
                id = MediaId(TYPE_PLAYLISTS, it.id.toString()),
                title = it.title,
                iconUri = it.iconUri
            )
        }
    }

    private fun getPlaylistMembers(playlistId: Long): Flow<List<MediaContent>> {
        val playlistMembersFlow = playlistDao.getPlaylistTracks(playlistId)

        return combine(mediaDao.tracks, playlistMembersFlow) { allTracks, members ->
            val tracksById = allTracks.associateByLong(Track::id)

            members
                .mapNotNull { playlistTrack ->
                    tracksById[playlistTrack.trackId]?.let {
                        AudioTrack(
                            id = MediaId(TYPE_PLAYLISTS, playlistId.toString(), it.id),
                            title = it.title,
                            artist = it.artist,
                            album = it.album,
                            mediaUri = it.mediaUri.toUri(),
                            iconUri = it.albumArtUri?.toUri(),
                            duration = it.duration,
                            disc = it.discNumber,
                            number = it.trackNumber
                        )
                    }
                }
                .takeUnless { it.isEmpty() }
                ?: throw NoSuchElementException("No playlist with id = $playlistId")
        }
    }
}