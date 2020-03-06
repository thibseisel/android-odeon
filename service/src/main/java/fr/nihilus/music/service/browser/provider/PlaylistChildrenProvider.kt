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

import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.net.toUri
import fr.nihilus.music.core.collections.associateByLong
import fr.nihilus.music.core.database.playlists.Playlist
import fr.nihilus.music.core.database.playlists.PlaylistDao
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.provider.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class PlaylistChildrenProvider(
    private val mediaDao: MediaDao,
    private val playlistDao: PlaylistDao
) : ChildrenProvider() {

    override fun findChildren(
        parentId: MediaId,
        fromIndex: Int,
        count: Int
    ): Flow<List<MediaItem>> {
        check(parentId.type == TYPE_PLAYLISTS)

        val playlistId = parentId.category?.toLongOrNull()
        return when {
            playlistId != null -> getPlaylistMembers(playlistId, fromIndex, count)
            else -> getPlaylists(fromIndex, count)
        }
    }

    private fun getPlaylists(
        fromIndex: Int,
        count: Int
    ): Flow<List<MediaItem>> = playlistDao.playlists.map { playlists ->
        val builder = MediaDescriptionCompat.Builder()

        playlists.asSequence()
            .drop(fromIndex)
            .take(count)
            .map { it.toMediaItem(builder) }
            .toList()
    }

    private fun getPlaylistMembers(
        playlistId: Long,
        fromIndex: Int,
        count: Int
    ): Flow<List<MediaItem>> = mediaDao.tracks.map { allTracks ->
        val builder = MediaDescriptionCompat.Builder()
        val tracksById = allTracks.associateByLong(Track::id)

        playlistDao.getPlaylistTracks(playlistId).asSequence()
            .drop(fromIndex)
            .take(count)
            .mapNotNullTo(mutableListOf()) { tracksById[it.trackId]?.toMediaItem(playlistId, builder) }
            .toList()
            .takeUnless { it.isEmpty() }
            ?: throw NoSuchElementException("No playlist with id = $playlistId")
    }

    private fun Playlist.toMediaItem(builder: MediaDescriptionCompat.Builder): MediaItem = browsable(
        builder,
        id = MediaId.encode(TYPE_PLAYLISTS, id.toString()),
        title = title,
        iconUri = iconUri
    )

    private fun Track.toMediaItem(
        playlistId: Long,
        builder: MediaDescriptionCompat.Builder
    ): MediaItem = playable(
        builder,
        id = MediaId.encode(TYPE_PLAYLISTS, playlistId.toString(), id),
        title = title,
        subtitle = artist,
        mediaUri = mediaUri.toUri(),
        iconUri = albumArtUri?.toUri(),
        duration = duration,
        disc = discNumber,
        number = trackNumber
    )
}