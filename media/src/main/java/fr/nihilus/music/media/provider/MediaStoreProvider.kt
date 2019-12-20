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

package fr.nihilus.music.media.provider

import android.Manifest
import android.content.ContentUris
import android.database.ContentObserver
import android.net.Uri
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.*
import android.util.LongSparseArray
import fr.nihilus.music.core.context.AppDispatchers
import fr.nihilus.music.core.os.FileSystem
import fr.nihilus.music.core.os.PermissionDeniedException
import fr.nihilus.music.core.os.RuntimePermissions
import fr.nihilus.music.media.dagger.ServiceScoped
import fr.nihilus.music.media.os.MediaStoreDatabase
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Retrieve media information from the Android [MediaStore].
 *
 * @param resolver The content resolver used to access the MediaStore.
 * @param fileSystem Manager for reading and writing to the file system.
 * @param permissions Manager for reading system permissions.
 */
@ServiceScoped
internal class MediaStoreProvider
@Inject constructor(
    private val resolver: MediaStoreDatabase,
    private val fileSystem: FileSystem,
    private val permissions: RuntimePermissions,
    private val dispatchers: AppDispatchers
) : MediaProvider {

    private val trackColumns = arrayOf(
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

    private val albumColumns = arrayOf(
        Albums._ID,
        Albums.ALBUM,
        Albums.ARTIST,
        Albums.LAST_YEAR,
        Albums.NUMBER_OF_SONGS,
        Albums.ALBUM_ART,
        Media.ARTIST_ID
    )

    private val artistColumns = arrayOf(
        Artists._ID,
        Artists.ARTIST,
        Artists.NUMBER_OF_ALBUMS,
        Artists.NUMBER_OF_TRACKS
    )

    private val observers = mutableSetOf<PlatformObserverDelegate>()

    private data class AlbumArtInfo(
        val artistId: Long,
        val albumArtPath: String?,
        val releaseYear: Int
    )

    override suspend fun queryTracks(): List<Track> {
        requireReadPermission()

        return withContext(dispatchers.IO) {
            // Preload art Uris for each album to associate them with tracks.
            val artUriPerAlbum = queryAlbumArtUris()

            resolver.query(
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
                            albumArtUri = artUriPerAlbum[albumId],
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

    override suspend fun queryAlbums(): List<Album> {
        requireReadPermission()

        return withContext(dispatchers.IO) {
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
                val colAlbumArt = cursor.getColumnIndexOrThrow(Albums.ALBUM_ART)

                ArrayList<Album>(cursor.count).also { albumList ->
                    while (cursor.moveToNext()) {
                        albumList += Album(
                            id = cursor.getLong(colId),
                            title = cursor.getString(colTitle),
                            trackCount = cursor.getInt(colSongCount),
                            releaseYear = cursor.getInt(colYear),
                            albumArtUri = cursor.getString(colAlbumArt)?.let { albumArtFilepath ->
                                fileSystem.makeSharedContentUri(albumArtFilepath)?.toString()
                            },
                            artistId = cursor.getLong(colArtistId),
                            artist = cursor.getString(colArtist)
                        )
                    }
                }

            } ?: emptyList<Album>()
        }
    }

    override suspend fun queryArtists(): List<Artist> {
        requireReadPermission()

        return withContext(dispatchers.IO) {
            val albumArtPerArtistId = queryAlbumArtPerArtist()

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
        resolver.query(
            Albums.EXTERNAL_CONTENT_URI,
            arrayOf(Media.ARTIST_ID, Albums.ALBUM_ART, Albums.LAST_YEAR),
            null,
            null,
            "${Media.ARTIST_ID} ASC, ${Albums.LAST_YEAR} DESC"
        )?.use { cursor ->
            val colArtistId = cursor.getColumnIndexOrThrow(Media.ARTIST_ID)
            val colArtPath = cursor.getColumnIndexOrThrow(Albums.ALBUM_ART)
            val colYear = cursor.getColumnIndexOrThrow(Albums.LAST_YEAR)

            val albumInfo = ArrayList<AlbumArtInfo>(cursor.count)
            while (cursor.moveToNext()) {
                albumInfo += AlbumArtInfo(
                    artistId = cursor.getLong(colArtistId),
                    albumArtPath = cursor.getString(colArtPath)?.let { albumArtFilepath ->
                        fileSystem.makeSharedContentUri(albumArtFilepath)?.toString()
                    },
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

    override suspend fun deleteTracks(trackIds: LongArray): Int {
        requireWritePermission()

        return withContext(dispatchers.IO) {
            var whereClause = buildInClause(trackIds.size)
            var whereArgs = Array(trackIds.size) { trackIds[it].toString() }

            resolver.query(
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
                    whereClause = buildInClause(deletedTrackIds.size)
                    whereArgs = Array(deletedTrackIds.size) { deletedTrackIds[it].toString() }
                }

                // Delete track information from the MediaStore.
                // Only tracks whose file have been successfully deleted are removed.
                resolver.delete(Media.EXTERNAL_CONTENT_URI, whereClause, whereArgs)
            } ?: 0
        }
    }

    override fun registerObserver(observer: MediaProvider.Observer) {
        synchronized(observers) {
            if (observers.none { it.observer === observer }) {
                val observedUri = getContentUriForMediaType(observer.type)

                val platformObserver = PlatformObserverDelegate(observer)
                resolver.registerContentObserver(observedUri, true, platformObserver)
                observers += platformObserver
            }
        }
    }

    override fun unregisterObserver(observer: MediaProvider.Observer) {
        synchronized(observers) {
            val platformObserver = observers.find { it.observer === observer } ?: return
            observers -= platformObserver
            resolver.unregisterContentObserver(platformObserver)
        }
    }

    /**
     * Wraps a [MediaProvider.Observer] to listen for changes to content from the [android.provider.MediaStore].
     * Any received change notification for content of the specified
     * [media type][MediaProvider.Observer.type] are forwarded to the [observer].
     *
     * @param observer The observer that should be notified when content changes.
     */
    private class PlatformObserverDelegate(
        val observer: MediaProvider.Observer
    ) : ContentObserver(null) {

        override fun deliverSelfNotifications(): Boolean = false

        override fun onChange(selfChange: Boolean, uri: Uri?) = onChange(selfChange)
        override fun onChange(selfChange: Boolean) {
            observer.onChanged()
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

    private fun queryAlbumArtUris() = LongSparseArray<String>().also { artPathPerAlbumId ->
        resolver.query(
            Albums.EXTERNAL_CONTENT_URI, arrayOf(Albums._ID, Albums.ALBUM_ART),
            null, null, Albums.DEFAULT_SORT_ORDER
        )?.use { cursor ->
            val colAlbumId = cursor.getColumnIndexOrThrow(Albums._ID)
            val colFilepath = cursor.getColumnIndexOrThrow(Albums.ALBUM_ART)

            while (cursor.moveToNext()) {
                cursor.getString(colFilepath)?.let { filepath ->
                    val albumId = cursor.getLong(colAlbumId)
                    artPathPerAlbumId.put(
                        albumId,
                        fileSystem.makeSharedContentUri(filepath)?.toString()
                    )
                }
            }
        }
    }

    private fun buildInClause(paramCount: Int) = buildString {
        append(Media._ID).append(" IN ")
        CharArray(paramCount) { '?' }.joinTo(this, ",", "(", ")")
    }

    private fun getContentUriForMediaType(type: MediaProvider.MediaType) = when (type) {
        MediaProvider.MediaType.TRACKS -> Media.EXTERNAL_CONTENT_URI
        MediaProvider.MediaType.ALBUMS -> Albums.EXTERNAL_CONTENT_URI
        MediaProvider.MediaType.ARTISTS -> Artists.EXTERNAL_CONTENT_URI
    }
}