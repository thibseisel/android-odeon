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

package fr.nihilus.music.spotify.manager

import fr.nihilus.music.core.test.coroutines.flow.NeverFlow
import fr.nihilus.music.core.test.stub
import fr.nihilus.music.media.provider.Album
import fr.nihilus.music.media.provider.Artist
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.provider.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class FakeMediaDao(
    vararg tracks: Track
) : MediaDao {
    private val _tracks: List<Track> = tracks.toList()

    override val tracks: Flow<List<Track>>
        get() = flow { emit(_tracks) }

    override val albums: Flow<List<Album>>
        get() = NeverFlow

    override val artists: Flow<List<Artist>>
        get() = NeverFlow

    override suspend fun deleteTracks(ids: LongArray) = stub()
}
