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

import android.Manifest
import fr.nihilus.music.core.os.PermissionDeniedException
import fr.nihilus.music.core.test.stub
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.provider.Track
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.onEach

/**
 * A [MediaDao] that only stores audio [Track]s in memory,
 * simulating the behavior of the real implementation.
 *
 * @param initial The set of tracks that are initially stored.
 * @property permissionGranted Whether runtime read/write permission has been granted.
 * When set to `false`, all operations fails with [PermissionDeniedException].
 * This is `true` by default.
 */
internal class InMemoryTrackDao(
    initial: List<Track> = emptyList(),
    var permissionGranted: Boolean = true
) : MediaDao {

    private val savedTracks = initial.toSortedSet(compareBy(Track::title))
    private val _tracks = ConflatedBroadcastChannel(initial.toList())

    override val tracks: Flow<List<Track>>
        get() = _tracks.asFlow().onEach {
            if (!permissionGranted) {
                throw PermissionDeniedException(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

    override val albums: Flow<List<Album>>
        get() = stub()

    override val artists: Flow<List<Artist>>
        get() = stub()

    override suspend fun deleteTracks(trackIds: LongArray): Int {
        if (!permissionGranted) {
            throw PermissionDeniedException(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val sizeBeforeDelete = savedTracks.size
        val tracksHaveBeenDeleted = savedTracks.removeAll { it.id in trackIds }

        if (tracksHaveBeenDeleted) {
            _tracks.offer(savedTracks.toList())
        }

        return sizeBeforeDelete - savedTracks.size
    }
}