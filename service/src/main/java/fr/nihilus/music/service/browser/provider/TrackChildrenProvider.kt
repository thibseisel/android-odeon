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

import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat.MediaItem
import android.support.v4.media.MediaDescriptionCompat
import androidx.core.net.toUri
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.core.media.MediaItems
import fr.nihilus.music.media.provider.MediaDao
import fr.nihilus.music.media.provider.Track
import fr.nihilus.music.media.usage.UsageManager
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

internal class TrackChildrenProvider(
    private val mediaDao: MediaDao,
    private val usageManager: UsageManager
) : ChildrenProvider() {

    override suspend fun findChildren(
        parentId: MediaId,
        fromIndex: Int,
        count: Int
    ): List<MediaItem> {
        check(parentId.type == TYPE_TRACKS && parentId.category != null)

        return when (parentId.category) {
            MediaId.CATEGORY_ALL -> getAllTracks(fromIndex, count)
            MediaId.CATEGORY_MOST_RATED -> getMostRatedTracks(fromIndex, count)
            MediaId.CATEGORY_POPULAR -> getMonthPopularTracks(fromIndex, count)
            MediaId.CATEGORY_RECENTLY_ADDED -> getRecentlyAddedTracks(fromIndex, count)
            MediaId.CATEGORY_DISPOSABLE -> getDisposableTracks(fromIndex, count)
            else -> throw NoSuchElementException("No such parent: $parentId")
        }
    }

    private suspend fun getAllTracks(
        fromIndex: Int,
        count: Int
    ): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return mediaDao.tracks.first().asSequence()
            .drop(fromIndex)
            .take(count)
            .map { it.toMediaItem(MediaId.CATEGORY_ALL, builder) }
            .toList()
    }

    private suspend fun getMostRatedTracks(fromIndex: Int, count: Int): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return usageManager.getMostRatedTracks().asSequence()
            .drop(fromIndex)
            .take(count)
            .map { it.toMediaItem(MediaId.CATEGORY_MOST_RATED, builder) }
            .toList()
    }

    private suspend fun getRecentlyAddedTracks(fromIndex: Int, count: Int): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return mediaDao.tracks.first().asSequence()
            .sortedByDescending { it.availabilityDate }
            .take(25)
            .drop(fromIndex)
            .take(count)
            .map { it.toMediaItem(MediaId.CATEGORY_RECENTLY_ADDED, builder) }
            .toList()
    }

    private suspend fun getMonthPopularTracks(fromIndex: Int, count: Int): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return usageManager.getPopularTracksSince(30, TimeUnit.DAYS)
            .asSequence()
            .drop(fromIndex)
            .take(count)
            .map { it.toMediaItem(MediaId.CATEGORY_POPULAR, builder) }
            .toList()
    }

    private suspend fun getDisposableTracks(fromIndex: Int, count: Int): List<MediaItem> {
        val builder = MediaDescriptionCompat.Builder()

        return usageManager.getDisposableTracks().asSequence()
            .drop(fromIndex)
            .take(count)
            .map { track ->
                val mediaId = MediaId(TYPE_TRACKS, MediaId.CATEGORY_DISPOSABLE, track.trackId)
                val description = builder.setMediaId(mediaId.encoded)
                    .setTitle(track.title)
                    .setExtras(Bundle().apply {
                        putLong(MediaItems.EXTRA_FILE_SIZE, track.fileSizeBytes)
                        track.lastPlayedTime?.let { lastPlayedTime ->
                            putLong(MediaItems.EXTRA_LAST_PLAYED_TIME, lastPlayedTime)
                        }
                    }).build()
                MediaItem(description, MediaItem.FLAG_PLAYABLE)
            }
            .toList()
    }

    private fun Track.toMediaItem(
        category: String,
        builder: MediaDescriptionCompat.Builder
    ): MediaItem = playable(
        builder,
        id = MediaId.encode(TYPE_TRACKS, category, id),
        title = title,
        subtitle = artist,
        mediaUri = mediaUri.toUri(),
        iconUri = albumArtUri?.toUri(),
        duration = duration,
        disc = discNumber,
        number = trackNumber
    )
}