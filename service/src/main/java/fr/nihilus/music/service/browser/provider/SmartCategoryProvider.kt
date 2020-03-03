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
import fr.nihilus.music.core.database.spotify.TrackFeature
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_SMART
import fr.nihilus.music.core.media.MediaId.Builder.encode
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.spotify.manager.FeatureFilter
import fr.nihilus.music.spotify.manager.SpotifyManager

internal class SmartCategoryProvider(
    private val spotifyManager: SpotifyManager
) : ChildrenProvider() {

    override suspend fun findChildren(
        parentId: MediaId,
        fromIndex: Int,
        count: Int
    ): List<MediaItem>? {
        check(parentId.type == TYPE_SMART && parentId.category != null)

        return when (parentId.category) {
            "HAPPY" -> getHappyTracks(fromIndex, count)
            "PARTY" -> getDanceableTracks(fromIndex, count)
            else -> null
        }
    }

    private suspend fun getHappyTracks(startIndex: Int, count: Int): List<MediaItem> {
        val happyFilters = listOf(
            FeatureFilter.OnRange(TrackFeature::valence, 0.65f, 1.0f)
        )

        val builder = MediaDescriptionCompat.Builder()
        return spotifyManager.findTracksHavingFeatures(happyFilters).asSequence()
            .drop(startIndex)
            .take(count)
            .mapTo(mutableListOf()) { (track, _) -> track.toMediaItem(builder) }
            .toList()
    }

    private suspend fun getDanceableTracks(fromIndex: Int, count: Int): List<MediaItem> {
        val partyFilters = listOf(
            FeatureFilter.OnRange(TrackFeature::danceability, 0.7f, 1.0f),
            FeatureFilter.OnRange(TrackFeature::energy, 0.5f, 1.0f)
        )

        val builder = MediaDescriptionCompat.Builder()
        return spotifyManager.findTracksHavingFeatures(partyFilters).asSequence()
            .drop(fromIndex)
            .take(count)
            .mapTo(mutableListOf()) { (track, _) -> track.toMediaItem(builder) }
            .toList()
    }

    private fun Track.toMediaItem(builder: MediaDescriptionCompat.Builder): MediaItem {
        return playable(
            builder,
            id = encode(TYPE_SMART, "HAPPY", id),
            title = title,
            subtitle = artist,
            mediaUri = mediaUri.toUri(),
            iconUri = albumArtUri?.toUri(),
            duration = duration,
            disc = discNumber,
            number = trackNumber
        )
    }
}