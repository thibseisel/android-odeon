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

package fr.nihilus.music.media.albums

import android.content.ContentResolver
import android.provider.MediaStore.Audio.Albums
import android.provider.MediaStore.Audio.Media
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
 * Reads audio albums stored locally on the device's storage.
 */
internal class AlbumLocalSource @Inject constructor(
    private val resolver: ContentResolver,
    private val permissionRepository: PermissionRepository,
    private val dispatchers: AppDispatchers,
) {
    /**
     * Live list of all albums for tracks stored locally on the device.
     */
    val albums: Flow<List<Album>>
        get() = combine(
            resolver.observeContentChanges(Albums.EXTERNAL_CONTENT_URI).conflate(),
            permissionRepository.permissions
        ) { _, permissions ->
            if (permissions.canReadAudioFiles) queryAlbums() else emptyList()
        }

    private suspend fun queryAlbums(): List<Album> = withContext(dispatchers.IO) {
        val albumColumns = arrayOf(
            Albums._ID,
            Albums.ALBUM,
            Albums.ARTIST,
            Albums.LAST_YEAR,
            Albums.NUMBER_OF_SONGS,
            Media.ARTIST_ID
        )

        resolver.query(
            Albums.EXTERNAL_CONTENT_URI,
            albumColumns,
            null,
            null,
            Albums.DEFAULT_SORT_ORDER
        )?.use { cursor ->
            val colId = cursor.getColumnIndexOrThrow(Albums._ID)
            val colTitle = cursor.getColumnIndexOrThrow(Albums.ALBUM)
            val colArtistId = cursor.getColumnIndexOrThrow(Media.ARTIST_ID)
            val colArtist = cursor.getColumnIndexOrThrow(Albums.ARTIST)
            val colYear = cursor.getColumnIndexOrThrow(Albums.LAST_YEAR)
            val colSongCount = cursor.getColumnIndexOrThrow(Albums.NUMBER_OF_SONGS)

            buildList(cursor.count) {
                while (cursor.moveToNext()) {
                    val albumId = cursor.getLong(colId)

                    this += Album(
                        id = albumId,
                        title = cursor.getString(colTitle),
                        trackCount = cursor.getInt(colSongCount),
                        releaseYear = cursor.getInt(colYear),
                        artistId = cursor.getLong(colArtistId),
                        artist = cursor.getString(colArtist),
                        albumArtUri = MediaStoreInternals.AudioThumbnails.CONTENT_URI
                            .withAppendedId(albumId)
                            .toString()
                    )
                }
            }
        } ?: emptyList()
    }
}
