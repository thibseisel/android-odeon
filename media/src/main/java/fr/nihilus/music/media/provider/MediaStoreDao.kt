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

package fr.nihilus.music.media.provider

import android.Manifest
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.*
import android.util.LongSparseArray
import dagger.Reusable
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.os.FileSystem
import fr.nihilus.music.core.os.PermissionDeniedException
import fr.nihilus.music.core.os.RuntimePermissions
import fr.nihilus.music.media.os.MediaStoreDatabase
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * An implementation of [MediaDao] that reads and write media from the Android [MediaStore].
 *
 * @param database The MediaStore database that should be queried.
 * @param fileSystem Abstraction over the file system.
 * @param permissions Manager for reading system permissions.
 * @param dispatchers Coroutine threading contexts to execute long running IO operations.
 */
@Reusable
internal class MediaStoreDao @Inject constructor(
    private val database: MediaStoreDatabase,
    private val fileSystem: FileSystem,
    private val permissions: RuntimePermissions,
    private val dispatchers: AppDispatchers
) : MediaDao {

    /**
     * An internal Content Uri for album artworks.
     * It is not officially exposed as [MediaStore] constants, but has the advantage of
     * being available on all Android versions and to external applications without permissions.
     */
    private val albumArtworkUri = Uri.parse("content://media/external/audio/albumart")

    override val tracks: Flow<List<Track>> =
        mediaUpdateFlow(Media.EXTERNAL_CONTENT_URI).mapLatest {
            queryTracks()
        }

    override val albums: Flow<List<Album>> =
        mediaUpdateFlow(Albums.EXTERNAL_CONTENT_URI).mapLatest {
            queryAlbums()
        }

    override val artists: Flow<List<Artist>> =
        mediaUpdateFlow(Artists.EXTERNAL_CONTENT_URI).mapLatest {
            queryArtists()
        }

    override suspend fun deleteTracks(trackIds: LongArray): Int {
        requireWritePermission()

        return withContext(dispatchers.IO) {
            var whereClause = buildInTrackIdClause(trackIds.size)
            var whereArgs = Array(trackIds.size) { trackIds[it].toString() }

            database.query(
                Media.EXTERNAL_CONTENT_URI,
                arrayOf(Media._ID, Media.DATA),
                whereClause,
                whereArgs,
                Media._ID
            )?.use { cursor ->
                val colId = cursor.getColumnIndexOrThrow(Media._ID)
                val colFilepath = cursor.getColumnIndexOrThrow(Media.DATA)

                val deletedTrackIds = mutableListOf<Long>()
                while (cursor.moveToNext()) {
                    val trackId = cursor.getLong(colId)
                    val path = cursor.getString(colFilepath)

                    // Attempt to delete each file.
                    if (fileSystem.deleteFile(path)) {
                        deletedTrackIds += trackId
                    }
                }

                // if some tracks have not been deleted, rewrite delete clause.
                if (deletedTrackIds.size < trackIds.size) {
                    whereClause = buildInTrackIdClause(deletedTrackIds.size)
                    whereArgs = Array(deletedTrackIds.size) { deletedTrackIds[it].toString() }
                }

                // Delete track information from the MediaStore.
                // Only tracks whose file have been successfully deleted are removed.
                database.delete(Media.EXTERNAL_CONTENT_URI, whereClause, whereArgs)
            } ?: 0
        }
    }

    private fun mediaUpdateFlow(mediaUri: Uri) = callbackFlow<Unit> {
        // Trigger initial load.
        offer(Unit)

        // Register an observer until flow is cancelled, and reload whenever it is notified.
        val observer = ChannelContentObserver(channel)
        database.registerContentObserver(mediaUri, true, observer)

        awaitClose { database.unregisterContentObserver(observer) }
    }.conflate()

    private suspend fun queryTracks(): List<Track> {
        requireReadPermission()

        return withContext(dispatchers.IO) {
            val trackColumns = arrayOf(
                BaseColumns._ID,
                Media.TITLE,
                Media.ALBUM,
                Media.ARTIST,
                Media.DURATION,
                Media.TRACK,
                Media.ALBUM_ID,
                Media.ARTIST_ID,
                Media.DATE_ADDED,
                Media.SIZE
            )

            database.query(
                Media.EXTERNAL_CONTENT_URI,
                trackColumns,
                "${Media.IS_MUSIC} = 1",
                null,
                Media.DEFAULT_SORT_ORDER
            )?.use { cursor ->
                // Memorize cursor column indexes for faster lookup
                val colId = cursor.getColumnIndexOrThrow(BaseColumns._ID)
                val colTitle = cursor.getColumnIndexOrThrow(Media.TITLE)
                val colAlbum = cursor.getColumnIndexOrThrow(Media.ALBUM)
                val colArtist = cursor.getColumnIndexOrThrow(Media.ARTIST)
                val colDuration = cursor.getColumnIndexOrThrow(Media.DURATION)
                val colTrackNo = cursor.getColumnIndexOrThrow(Media.TRACK)
                val colAlbumId = cursor.getColumnIndexOrThrow(Media.ALBUM_ID)
                val colArtistId = cursor.getColumnIndexOrThrow(Media.ARTIST_ID)
                val colDateAdded = cursor.getColumnIndexOrThrow(Media.DATE_ADDED)
                val colFileSize = cursor.getColumnIndexOrThrow(Media.SIZE)

                ArrayList<Track>(cursor.count).also { trackList ->
                    while (cursor.moveToNext()) {
                        val trackId = cursor.getLong(colId)
                        val albumId = cursor.getLong(colAlbumId)
                        val trackNo = cursor.getInt(colTrackNo)
                        val trackUri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, trackId).toString()

                        trackList += Track(
                            id = trackId,
                            title = cursor.getString(colTitle),
                            artist = cursor.getString(colArtist),
                            album = cursor.getString(colAlbum),
                            duration = cursor.getLong(colDuration),
                            discNumber = trackNo / 1000,
                            trackNumber = trackNo % 1000,
                            mediaUri = trackUri,
                            albumArtUri = ContentUris.withAppendedId(albumArtworkUri, albumId).toString(),
                            availabilityDate = cursor.getLong(colDateAdded),
                            artistId = cursor.getLong(colArtistId),
                            albumId = albumId,
                            fileSize = cursor.getLong(colFileSize)
                        )
                    }
                }
            } ?: emptyList<Track>()
        }
    }

    private suspend fun queryAlbums(): List<Album> {
        requireReadPermission()

        return withContext(dispatchers.IO) {

            val albumColumns = arrayOf(
                Albums._ID,
                Albums.ALBUM,
                Albums.ARTIST,
                Albums.LAST_YEAR,
                Albums.NUMBER_OF_SONGS,
                Media.ARTIST_ID
            )

            database.query(
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

                ArrayList<Album>(cursor.count).also { albumList ->
                    while (cursor.moveToNext()) {
                        val albumId = cursor.getLong(colId)
                        
                        albumList += Album(
                            id = albumId,
                            title = cursor.getString(colTitle),
                            trackCount = cursor.getInt(colSongCount),
                            releaseYear = cursor.getInt(colYear),
                            albumArtUri = ContentUris.withAppendedId(albumArtworkUri, albumId).toString(),
                            artistId = cursor.getLong(colArtistId),
                            artist = cursor.getString(colArtist)
                        )
                    }
                }

            } ?: emptyList<Album>()
        }
    }

    private suspend fun queryArtists(): List<Artist> {
        requireReadPermission()

        return withContext(dispatchers.IO) {
            val albumArtPerArtistId = queryAlbumArtPerArtist()

            val artistColumns = arrayOf(
                Artists._ID,
                Artists.ARTIST,
                Artists.NUMBER_OF_ALBUMS,
                Artists.NUMBER_OF_TRACKS
            )

            database.query(
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

                ArrayList<Artist>(cursor.count).also { artistList ->
                    while (cursor.moveToNext()) {
                        val artistId = cursor.getLong(colArtistId)
                        artistList += Artist(
                            artistId,
                            name = cursor.getString(colArtistName),
                            albumCount = cursor.getInt(colAlbumCount),
                            trackCount = cursor.getInt(colTrackCount),
                            iconUri = albumArtPerArtistId[artistId]
                        )
                    }
                }
            } ?: emptyList<Artist>()
        }
    }

    private fun queryAlbumArtPerArtist() = LongSparseArray<String?>().also { albumArtPerArtistId ->
        database.query(
            Albums.EXTERNAL_CONTENT_URI,
            arrayOf(Albums._ID, Media.ARTIST_ID, Albums.LAST_YEAR),
            null,
            null,
            "${Media.ARTIST_ID} ASC, ${Albums.LAST_YEAR} DESC"
        )?.use { cursor ->
            val colAlbumId = cursor.getColumnIndexOrThrow(Albums._ID)
            val colArtistId = cursor.getColumnIndexOrThrow(Media.ARTIST_ID)
            val colYear = cursor.getColumnIndexOrThrow(Albums.LAST_YEAR)

            val albumInfo = ArrayList<AlbumArtInfo>(cursor.count)
            while (cursor.moveToNext()) {
                val albumId = cursor.getLong(colAlbumId)

                albumInfo += AlbumArtInfo(
                    artistId = cursor.getLong(colArtistId),
                    albumArtPath = ContentUris.withAppendedId(albumArtworkUri, albumId).toString(),
                    releaseYear = cursor.getInt(colYear)
                )
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

    private fun requireReadPermission() {
        if (!permissions.canReadExternalStorage) {
            throw PermissionDeniedException(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun requireWritePermission() {
        if (!permissions.canWriteToExternalStorage) {
            throw PermissionDeniedException(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun buildInTrackIdClause(paramCount: Int) = buildString {
        append(Media._ID).append(" IN ")
        CharArray(paramCount) { '?' }.joinTo(this, ",", "(", ")")
    }

    private class ChannelContentObserver(
        private val channel: SendChannel<Unit>
    ) : ContentObserver(null) {

        override fun deliverSelfNotifications(): Boolean = false

        override fun onChange(selfChange: Boolean, uri: Uri?) = onChange(selfChange)

        override fun onChange(selfChange: Boolean) {
            channel.offer(Unit)
        }
    }

    private data class AlbumArtInfo(
        val artistId: Long,
        val albumArtPath: String?,
        val releaseYear: Int
    )
}