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

package fr.nihilus.music.media.artists

import android.content.ContentResolver
import android.provider.MediaStore.Audio.Albums
import android.provider.MediaStore.Audio.Artists
import android.provider.MediaStore.Audio.Media
import android.util.LongSparseArray
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.permissions.PermissionRepository
import fr.nihilus.music.media.provider.MediaStoreInternals
import fr.nihilus.music.media.provider.observeContentChanges
import fr.nihilus.music.media.provider.withAppendedId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Reads audio artists stored locally on the device's storage.
 */
internal class ArtistLocalSource @Inject constructor(
    private val resolver: ContentResolver,
    private val permissionRepository: PermissionRepository,
    private val dispatchers: AppDispatchers,
) {
    /**
     * Live list of all artists that contributed to audio files stored on the device.
     */
    val artists: Flow<List<Artist>>
        get() = combine(
            resolver.observeContentChanges(Artists.EXTERNAL_CONTENT_URI).conflate(),
            permissionRepository.permissions
        ) { _, permissions ->
            if (permissions.canReadAudioFiles) queryArtists() else emptyList()
        }

    private suspend fun queryArtists(): List<Artist> = withContext(dispatchers.IO) {
        val albumArtPerArtistId = queryAlbumArtPerArtist()

        val artistColumns = arrayOf(
            Artists._ID,
            Artists.ARTIST,
            Artists.NUMBER_OF_ALBUMS,
            Artists.NUMBER_OF_TRACKS
        )

        resolver.query(
            Artists.EXTERNAL_CONTENT_URI,
            artistColumns,
            null,
            null,
            Artists.DEFAULT_SORT_ORDER
        )?.use { cursor ->
            val colArtistId = cursor.getColumnIndexOrThrow(Artists._ID)
            val colArtistName = cursor.getColumnIndexOrThrow(Artists.ARTIST)
            val colAlbumCount = cursor.getColumnIndexOrThrow(Artists.NUMBER_OF_ALBUMS)
            val colTrackCount = cursor.getColumnIndexOrThrow(Artists.NUMBER_OF_TRACKS)

            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    val artistId = cursor.getLong(colArtistId)

                    this += Artist(
                        artistId,
                        name = cursor.getString(colArtistName),
                        albumCount = cursor.getInt(colAlbumCount),
                        trackCount = cursor.getInt(colTrackCount),
                        iconUri = albumArtPerArtistId[artistId]
                    )
                }
            }
        } ?: emptyList()
    }

    private fun queryAlbumArtPerArtist() = LongSparseArray<String?>().also { albumArtPerArtistId ->
        resolver.query(
            Albums.EXTERNAL_CONTENT_URI,
            arrayOf(Albums._ID, Media.ARTIST_ID, Albums.LAST_YEAR),
            null,
            null,
            "${Media.ARTIST_ID} ASC, ${Albums.LAST_YEAR} DESC"
        )?.use { cursor ->
            val colAlbumId = cursor.getColumnIndexOrThrow(Albums._ID)
            val colArtistId = cursor.getColumnIndexOrThrow(Media.ARTIST_ID)
            val colYear = cursor.getColumnIndexOrThrow(Albums.LAST_YEAR)

            val albumInfo = buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    val albumId = cursor.getLong(colAlbumId)

                    this += AlbumArtInfo(
                        artistId = cursor.getLong(colArtistId),
                        releaseYear = cursor.getInt(colYear),
                        albumArtPath = MediaStoreInternals.AudioThumbnails.CONTENT_URI
                            .withAppendedId(albumId)
                            .toString()
                    )
                }
            }

            albumInfo.groupingBy { it.artistId }
                .reduce { _, mostRecent, info ->
                    when {
                        info.albumArtPath != null && mostRecent.releaseYear <= info.releaseYear -> info
                        mostRecent.albumArtPath == null -> info
                        else -> mostRecent
                    }
                }
                .forEach { (artistId, info) ->
                    albumArtPerArtistId.put(artistId, info.albumArtPath)
                }
        }
    }

    private data class AlbumArtInfo(
        val artistId: Long,
        val albumArtPath: String?,
        val releaseYear: Int
    )
}
