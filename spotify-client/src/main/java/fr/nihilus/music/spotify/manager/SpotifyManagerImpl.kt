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

import fr.nihilus.music.core.collections.associateByLong
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.database.spotify.*
import fr.nihilus.music.core.os.Clock
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.spotify.model.AudioFeature
import fr.nihilus.music.spotify.service.HttpResource
import fr.nihilus.music.spotify.service.SpotifyQuery
import fr.nihilus.music.spotify.service.SpotifyService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

/**
 * Implementation of the [SpotifyManager] that retrieve tracks from [MediaDao]
 * and downloads track metadata from the [Spotify API][SpotifyService].
 */
internal class SpotifyManagerImpl @Inject constructor(
    private val mediaDao: MediaDao,
    private val service: SpotifyService,
    private val localDao: SpotifyDao,
    private val dispatchers: AppDispatchers,
    private val clock: Clock
) : SpotifyManager {

    override suspend fun findTracksHavingFeatures(filters: List<FeatureFilter>): List<Pair<Track, TrackFeature>> {
        val tracks = mediaDao.tracks.first()
        val features = localDao.getLocalizedFeatures()

        val featuresById = features
            .filter { (_, feature) -> filters.all { it.matches(feature) } }
            .associateByLong { it.trackId }

        return tracks.mapNotNull { track ->
            featuresById[track.id]?.let { (_, feature) -> track to feature }
        }
    }

    override suspend fun sync() {
        val unSyncedTracks = coroutineScope {
            // Load tracks and remote links in parallel.
            val allTracks = async { mediaDao.tracks.first() }
            val allLinks = async { localDao.getLinks() }

            // Delete links pointing to a track that does not exist anymore.
            val tracksById = allTracks.await().associateByLong { it.id }
            val (existingLinks, oldLinks) = allLinks.await().partition { tracksById.containsKey(it.trackId) }
            val deletedTrackIds = LongArray(oldLinks.size) { oldLinks[it].trackId }
            localDao.deleteLinks(deletedTrackIds)

            // Compute tracks that have no links.
            val linksPerTrack = existingLinks.associateByLong { it.trackId }
            allTracks.await().filterNot { linksPerTrack.containsKey(it.id) }
        }

        Timber.tag("SpotifySync").d("%s track(s) need to be synced.", unSyncedTracks.size)

        if (unSyncedTracks.isNotEmpty()) {
            val newLinks = mutableListOf<SpotifyLink>()

            for (track in unSyncedTracks) {
                val spotifyTrack = withContext(dispatchers.IO) {
                    val query = buildSearchQueryFor(track)
                    Timber.tag("SpotifySync").v("Searching %s", query)
                    val results = service.search(query).take(1).toList()
                    results.firstOrNull()
                }

                if (spotifyTrack != null) {
                    Timber.tag("SpotifySync").v("Found track %s", spotifyTrack.name)
                    newLinks += SpotifyLink(track.id, spotifyTrack.id, clock.currentEpochTime)

                } else {
                    Timber.tag("SpotifySync").w("Found no result for track %s.", track.title)
                }
            }

            withContext(dispatchers.IO) {
                newLinks.asSequence()
                    .chunked(100)
                    .forEach { links ->
                        val trackIds = links.map { it.spotifyId }
                        when (val resource = service.getSeveralTrackFeatures(trackIds)) {
                            is HttpResource.Loaded -> {
                                for ((index, link) in links.withIndex()) {
                                    val feature = resource.data[index] ?: continue
                                    localDao.saveTrackFeature(link, feature.asLocalFeature())
                                }
                            }

                            is HttpResource.Failed -> {
                                Timber.tag("SpotifySync").e("Unable to fetch tracks features: HTTP error %s (%s)", resource.status, resource.message)
                            }
                        }
                    }
            }
        }
    }

    private fun buildSearchQueryFor(track: Track) = SpotifyQuery.Track(
        title = track.title,
        artist = track.artist
    )

    private fun AudioFeature.asLocalFeature() = TrackFeature(
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