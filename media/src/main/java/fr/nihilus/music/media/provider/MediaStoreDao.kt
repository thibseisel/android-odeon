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

package fr.nihilus.music.media.provider

import android.Manifest
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.*
import android.util.LongSparseArray
import androidx.annotation.RequiresApi
import dagger.Reusable
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.os.FileSystem
import fr.nihilus.music.core.permissions.PermissionRepository
import fr.nihilus.music.media.albums.Album
import fr.nihilus.music.media.artists.Artist
import fr.nihilus.music.media.dagger.SourceDao
import fr.nihilus.music.media.os.MediaStoreDatabase
import fr.nihilus.music.media.tracks.DeleteTracksResult
import fr.nihilus.music.media.tracks.Track
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * An implementation of [MediaDao] that reads and write media from the Android [MediaStore].
 *
 * @param database The MediaStore database that should be queried.
 * @param fileSystem Abstraction over the file system.
 * @param permissionRepository Manager for reading system permissions.
 * @param dispatchers Coroutine threading contexts to execute long running IO operations.
 */
@Reusable
@SourceDao
internal class MediaStoreDao @Inject constructor(
    private val database: MediaStoreDatabase,
    private val fileSystem: FileSystem,
    private val permissionRepository: PermissionRepository,
    private val dispatchers: AppDispatchers
) : MediaDao {

    /**
     * An internal Content Uri for album artworks.
     * It is not officially exposed as [MediaStore] constants, but has the advantage of
     * being available on all Android versions and to external applications without permissions.
     */
    private val albumArtworkUri = Uri.parse("content://media/external/audio/albumart")

    override val tracks: Flow<List<Track>> = combine(
        observeContentUri(Media.EXTERNAL_CONTENT_URI),
        permissionRepository.permissions
    ) { _, permissions ->
        if (permissions.canReadAudioFiles) queryTracks() else emptyList()
    }

    override val albums: Flow<List<Album>> = combine(
        observeContentUri(Albums.EXTERNAL_CONTENT_URI),
        permissionRepository.permissions
    ) { _, permissions ->
        if (permissions.canReadAudioFiles) queryAlbums() else emptyList()
    }


    override val artists: Flow<List<Artist>> = combine(
        observeContentUri(Artists.EXTERNAL_CONTENT_URI),
        permissionRepository.permissions
    ) { _, permissions ->
        if (permissions.canReadAudioFiles) queryArtists() else emptyList()
    }

    override suspend fun deleteTracks(ids: LongArray): DeleteTracksResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteTracksWithScopedStorage(ids)
        } else {
            deleteTracksWithoutScopedStorage(ids)
        }
    }

    private suspend fun deleteTracksWithoutScopedStorage(ids: LongArray): DeleteTracksResult {
        if (!permissionRepository.permissions.value.canWriteAudioFiles) {
            return DeleteTracksResult.RequiresPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        val deleteCount = withContext(dispatchers.IO) {
            var whereClause = buildInTrackIdClause(ids.size)
            var whereArgs = Array(ids.size) { ids[it].toString() }

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
                if (deletedTrackIds.size < ids.size) {
                    whereClause = buildInTrackIdClause(deletedTrackIds.size)
                    whereArgs = Array(deletedTrackIds.size) { deletedTrackIds[it].toString() }
                }

                // Delete track information from the MediaStore.
                // Only tracks whose file have been successfully deleted are removed.
                database.delete(Media.EXTERNAL_CONTENT_URI, whereClause, whereArgs)
            } ?: 0
        }

        return DeleteTracksResult.Deleted(deleteCount)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun deleteTracksWithScopedStorage(ids: LongArray): DeleteTracksResult {
        val intent = database.createDeleteRequest(
            ids.map { trackId -> Media.EXTERNAL_CONTENT_URI.withAppendedId(trackId) }
        )
        return DeleteTracksResult.RequiresUserConsent(intent)
    }

    private fun observeContentUri(contentUri: Uri) = callbackFlow {
        // Trigger initial load.
        send(Unit)

        // Register an observer until flow is cancelled, and reload whenever it is notified.
        val observer = ChannelContentObserver(channel)
        database.registerContentObserver(contentUri, true, observer)

        awaitClose { database.unregisterContentObserver(observer) }
    }.conflate()

    private suspend fun queryTracks(): List<Track> = withContext(dispatchers.IO) {
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
                    val trackUri = Media.EXTERNAL_CONTENT_URI.withAppendedId(trackId).toString()

                    trackList += Track(
                        id = trackId,
                        title = cursor.getString(colTitle),
                        artist = cursor.getString(colArtist),
                        album = cursor.getString(colAlbum),
                        duration = cursor.getLong(colDuration),
                        discNumber = trackNo / 1000,
                        trackNumber = trackNo % 1000,
                        mediaUri = trackUri,
                        albumArtUri = albumArtworkUri.withAppendedId(albumId).toString(),
                        availabilityDate = cursor.getLong(colDateAdded),
                        artistId = cursor.getLong(colArtistId),
                        albumId = albumId,
                        fileSize = cursor.getLong(colFileSize)
                    )
                }
            }
        } ?: emptyList()
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
                        albumArtUri = albumArtworkUri.withAppendedId(albumId).toString(),
                        artistId = cursor.getLong(colArtistId),
                        artist = cursor.getString(colArtist)
                    )
                }
            }
        } ?: emptyList()
    }

    private suspend fun queryArtists(): List<Artist> = withContext(dispatchers.IO) {
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
        } ?: emptyList()
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
                    albumArtPath = albumArtworkUri.withAppendedId(albumId).toString(),
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
            channel.trySend(Unit)
        }
    }

    private data class AlbumArtInfo(
        val artistId: Long,
        val albumArtPath: String?,
        val releaseYear: Int
    )
}
