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

package fr.nihilus.music.media.exclusion

import fr.nihilus.music.core.database.exclusion.TrackExclusion
import fr.nihilus.music.core.database.exclusion.TrackExclusionDao
import fr.nihilus.music.core.test.coroutines.flow.infiniteFlowOf
import fr.nihilus.music.media.albums.Album
import fr.nihilus.music.media.artists.Artist
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.tracks.DeleteTracksResult
import fr.nihilus.music.media.tracks.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object StubMediaDao : MediaDao {

    override val tracks: Flow<List<Track>>
        get() = unchangedListOf(ALGORITHM, DIRTY_WATER, MATTER_OF_TIME, RUN)

    override val albums: Flow<List<Album>>
        get() = unchangedListOf(CONCRETE_GOLD, SIMULATION_THEORY, WASTING_LIGHT)

    override val artists: Flow<List<Artist>>
        get() = unchangedListOf(FOO_FIGHTERS, MUSE)

    override suspend fun deleteTracks(ids: LongArray): DeleteTracksResult =
        DeleteTracksResult.Deleted(0)

    private fun <M> unchangedListOf(vararg media: M) = infiniteFlowOf(media.toList())
}

internal class FakeExclusionDao(vararg initialExclusions: Long) : TrackExclusionDao {
    private val _exclusions: MutableStateFlow<List<TrackExclusion>>

    init {
        val tracks = initialExclusions.map { TrackExclusion(it, 0) }
        _exclusions = MutableStateFlow(tracks)
    }

    override val trackExclusions: Flow<List<TrackExclusion>>
        get() = _exclusions.asStateFlow()

    override suspend fun exclude(track: TrackExclusion) {
        _exclusions.value += track
    }

    override suspend fun allow(trackId: Long) {
        _exclusions.value = _exclusions.value.filterNot { it.trackId == trackId }
    }
}

private fun artist(id: Long, name: String, albumCount: Int, trackCount: Int) =
    Artist(id, name, albumCount, trackCount, null)

private fun album(id: Long, title: String, artist: Artist, trackCount: Int) =
    Album(id, title, artist.name, trackCount, 0, null, artist.id)

private fun track(id: Long, title: String, artist: Artist, album: Album, number: Int) =
    Track(
        id = id,
        title = title,
        artistId = artist.id,
        artist = artist.name,
        albumId = album.id,
        album = album.title,
        duration = 0,
        discNumber = 0,
        trackNumber = number,
        mediaUri = "",
        albumArtUri = null,
        availabilityDate = 0,
        fileSize = 0,
        exclusionTime = null
    )

internal val MUSE = artist(18, "Muse", 1, 1)
internal val SIMULATION_THEORY = album(98, "Simulation Theory", MUSE, 1)
internal val ALGORITHM = track(865, "Algorithm", MUSE, SIMULATION_THEORY, 1)

internal val FOO_FIGHTERS = artist(13, "Foo Fighters", 2, 3)
internal val CONCRETE_GOLD = album(102, "Concrete and Gold", FOO_FIGHTERS, 2)
internal val WASTING_LIGHT = album(26, "Wasting Light", FOO_FIGHTERS, 1)
internal val RUN = track(477, "Run", FOO_FIGHTERS, CONCRETE_GOLD, 2)
internal val DIRTY_WATER = track(481, "Dirty Water", FOO_FIGHTERS, CONCRETE_GOLD, 6)
internal val MATTER_OF_TIME = track(219, "A Matter Of Time", FOO_FIGHTERS, WASTING_LIGHT, 8)
