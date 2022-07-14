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
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.tracks.Track
import fr.nihilus.music.media.usage.UsageManager
import fr.nihilus.music.service.MediaContent
import fr.nihilus.music.service.AudioTrack
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject

internal class TrackChildrenProvider @Inject constructor(
    private val mediaDao: MediaDao,
    private val usageManager: UsageManager
) : ChildrenProvider() {

    override fun findChildren(parentId: MediaId): Flow<List<MediaContent>> {
        check(parentId.type == TYPE_TRACKS && parentId.category != null)

        return when (parentId.category) {
            MediaId.CATEGORY_ALL -> getAllTracks()
            MediaId.CATEGORY_MOST_RATED -> getMostRatedTracks()
            MediaId.CATEGORY_POPULAR -> getMonthPopularTracks()
            MediaId.CATEGORY_RECENTLY_ADDED -> getRecentlyAddedTracks()
            else -> flow { throw NoSuchElementException("No such parent: $parentId") }
        }
    }

    private fun getAllTracks(): Flow<List<AudioTrack>> = mediaDao.tracks.map { tracks ->
        tracks.map { it.toPlayableMedia(MediaId.CATEGORY_ALL) }
    }

    private fun getMostRatedTracks(): Flow<List<AudioTrack>> = usageManager.getMostRatedTracks().map { mostRated ->
        mostRated.map { it.toPlayableMedia(MediaId.CATEGORY_MOST_RATED) }
    }

    private fun getRecentlyAddedTracks(): Flow<List<AudioTrack>> = mediaDao.tracks.map { tracks ->
        tracks.asSequence()
            .sortedByDescending { it.availabilityDate }
            .take(25)
            .map { it.toPlayableMedia(MediaId.CATEGORY_RECENTLY_ADDED) }
            .toList()
    }

    private fun getMonthPopularTracks(): Flow<List<AudioTrack>> =
        usageManager.getPopularTracksSince(30, TimeUnit.DAYS).map { popularTracks ->
            popularTracks.map { it.toPlayableMedia(MediaId.CATEGORY_POPULAR) }
        }

    private fun Track.toPlayableMedia(
        category: String
    ) = AudioTrack(
        id = MediaId(TYPE_TRACKS, category, id),
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
