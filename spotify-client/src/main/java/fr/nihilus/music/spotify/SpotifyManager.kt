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

package fr.nihilus.music.spotify

import fr.nihilus.music.core.collections.associateByLong
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.database.spotify.*
import fr.nihilus.music.core.os.Clock
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.media.repo.MediaRepository
import fr.nihilus.music.spotify.model.AudioFeature
import fr.nihilus.music.spotify.model.SpotifyTrack
import fr.nihilus.music.spotify.service.HttpResource
import fr.nihilus.music.spotify.service.SpotifyQuery
import fr.nihilus.music.spotify.service.SpotifyService
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList

interface SpotifyManager {
    suspend fun sync()
}

internal class SpotifyManagerImpl(
    private val repository: MediaRepository,
    private val service: SpotifyService,
    private val localDao: SpotifyDao,
    private val dispatchers: AppDispatchers,
    private val clock: Clock
) : SpotifyManager {

    override suspend fun sync() {
        val unSyncedTracks = findUnSyncedTracks()

        if (unSyncedTracks.isNotEmpty()) {
            val newLinks = mutableListOf<SpotifyLink>()

            for (track in unSyncedTracks) {
                val spotifyTrack = withContext(dispatchers.IO) {
                    val query = buildSearchQueryFor(track)
                    val results = service.search(query).take(1).toList()
                    results.firstOrNull()
                }

                if (spotifyTrack != null) {
                    newLinks += SpotifyLink(track.id, spotifyTrack.id, clock.currentEpochTime)
                }
            }

            withContext(dispatchers.IO) {
                val trackIds = newLinks.map { it.spotifyId }
                when (val resource = service.getSeveralTrackFeatures(trackIds)) {
                    is HttpResource.Loaded -> {
                        for (index in newLinks.indices) {
                            val link = newLinks[index]
                            val feature = resource.data[index] ?: continue
                            localDao.saveTrackFeature(link, feature.asLocalFeature())
                        }
                    }
                }

            }
        }
    }

    private suspend fun findUnSyncedTracks(): List<Track> = coroutineScope {
        val asyncTracks = async { repository.getTracks() }
        val remoteLinks = localDao.getLinks().associateByLong(SpotifyLink::trackId)

        asyncTracks.await().filterNot { remoteLinks.containsKey(it.id) }
    }

    private fun buildSearchQueryFor(track: Track) = SpotifyQuery.Track(
        title = track.title,
        artist = track.artist
    )

    private fun AudioFeature.asLocalFeature(): TrackFeature = TrackFeature(
        id = id,
        key = decodePitch(key),
        mode = decodeMusicalMode(mode),
        tempo = tempo,
        signature = signature,
        loudness = loudness,
        acousticness = acousticness,
        danceability = danceability,
        energy = energy,
        instrumentalness = instrumentalness,
        liveness = liveness,
        speechiness = speechiness,
        valence = valence
    )
}