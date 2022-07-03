/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.media.provider

import fr.nihilus.music.media.albums.Album
import fr.nihilus.music.media.albums.AlbumRepository
import fr.nihilus.music.media.artists.Artist
import fr.nihilus.music.media.artists.ArtistRepository
import fr.nihilus.music.media.tracks.DeleteTracksResult
import fr.nihilus.music.media.tracks.Track
import fr.nihilus.music.media.tracks.TrackRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Implementation of [MediaDao] that delegates to repositories.
 * This acts as an adapter for classes that still rely on [MediaDao],
 * easing the migration to repositories.
 */
internal class DelegatingMediaDao @Inject constructor(
    private val trackRepository: TrackRepository,
    private val albumRepository: AlbumRepository,
    private val artistRepository: ArtistRepository,
) : MediaDao {

    override val tracks: Flow<List<Track>>
        get() = trackRepository.tracks

    override val albums: Flow<List<Album>>
        get() = albumRepository.albums

    override val artists: Flow<List<Artist>>
        get() = artistRepository.artists

    override suspend fun deleteTracks(ids: LongArray): DeleteTracksResult =
        trackRepository.deleteTracks(ids)
}
