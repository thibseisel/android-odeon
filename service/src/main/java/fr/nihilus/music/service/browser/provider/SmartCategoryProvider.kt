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
import fr.nihilus.music.core.database.spotify.TrackFeature
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_SMART
import fr.nihilus.music.media.tracks.Track
import fr.nihilus.music.service.MediaContent
import fr.nihilus.music.service.AudioTrack
import fr.nihilus.music.spotify.manager.FeatureFilter
import fr.nihilus.music.spotify.manager.SpotifyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine

internal class SmartCategoryProvider(
    private val spotifyManager: SpotifyManager
) : ChildrenProvider() {

    override fun findChildren(
        parentId: MediaId
    ): Flow<List<MediaContent>> {
        check(parentId.type == TYPE_SMART && parentId.category != null)

        return when (parentId.category) {
            "HAPPY" -> getHappyTracks()
            "PARTY" -> getDanceableTracks()
            else -> throw NoSuchElementException("No such parent: $parentId")
        }
    }

    private fun getHappyTracks(): Flow<List<AudioTrack>> = flow {
        val happyFilters = listOf(
            FeatureFilter.OnRange(TrackFeature::valence, 0.65f, 1.0f)
        )

        val happyTracks = spotifyManager.findTracksHavingFeatures(happyFilters)
            .map { (track, _) -> track.toPlayableMedia() }

        emit(happyTracks)
        suspendCancellableCoroutine<Nothing> {}
    }

    private fun getDanceableTracks(): Flow<List<AudioTrack>> = flow {
        val partyFilters = listOf(
            FeatureFilter.OnRange(TrackFeature::danceability, 0.7f, 1.0f),
            FeatureFilter.OnRange(TrackFeature::energy, 0.5f, 1.0f)
        )

        val partyTracks = spotifyManager.findTracksHavingFeatures(partyFilters)
            .map { (track, _) -> track.toPlayableMedia() }

        emit(partyTracks)
        suspendCancellableCoroutine<Nothing> {}
    }

    private fun Track.toPlayableMedia(): AudioTrack {
        return AudioTrack(
            id = MediaId(TYPE_SMART, "HAPPY", id),
            title = title,
            artist = artist,
            album = album,
            mediaUri = mediaUri.toUri(),
            iconUri = albumArtUri?.toUri(),
            duration = duration,
            disc = discNumber,
            number = trackNumber
        )
    }
}