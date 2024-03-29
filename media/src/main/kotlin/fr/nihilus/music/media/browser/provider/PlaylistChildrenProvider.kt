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

package fr.nihilus.music.media.browser.provider

import androidx.core.net.toUri
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_PLAYLISTS
import fr.nihilus.music.media.AudioTrack
import fr.nihilus.music.media.MediaCategory
import fr.nihilus.music.media.MediaContent
import fr.nihilus.music.media.playlists.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

internal class PlaylistChildrenProvider @Inject constructor(
    private val playlistRepository: PlaylistRepository,
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

    private fun getPlaylists(): Flow<List<MediaContent>> =
        playlistRepository.playlists.map { playlists ->
            playlists.map {
                MediaCategory(
                    id = MediaId(TYPE_PLAYLISTS, it.id.toString()),
                    title = it.title,
                    iconUri = it.iconUri,
                    playable = true
                )
            }
        }

    private fun getPlaylistMembers(playlistId: Long): Flow<List<MediaContent>> =
        playlistRepository.getPlaylistTracks(playlistId).map { tracks ->
            tracks
                .map {
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
                .ifEmpty { throw NoSuchElementException("No playlist with id = $playlistId") }
        }
}
