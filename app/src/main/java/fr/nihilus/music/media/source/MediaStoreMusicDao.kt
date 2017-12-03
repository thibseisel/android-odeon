/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.media.source

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore.Audio.*
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import android.util.LongSparseArray
import fr.nihilus.music.assert
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.utils.MediaID
import fr.nihilus.music.utils.PermissionUtil
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import org.jetbrains.annotations.TestOnly
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A music datasource that fetches its items from the Android mediastore.
 * Items represents files that are stored on the device's external storage.
 *
 * @constructor
 * Instantiate this MusicDao implementation with the provided metadata cache.
 * This constructor should only be used as a way to manually inject cache for testing purposes.
 *
 * @param context The application context
 * @param metadataCache The cache into which this object stores its metadata.
 */
@Singleton
class MediaStoreMusicDao
@TestOnly internal constructor(
        private val context: Context,
        private val metadataCache: LongSparseArray<MediaMetadataCompat>
) : MusicDao {

    /**
     * Instantiate this MusicDao implementation.
     *
     * @param context The application context
     */
    @Inject constructor(context: Context) : this(context, LongSparseArray())

    private val resolver: ContentResolver = context.contentResolver

    /**
     * Translates sortable metadata keys into their MediaStore equivalent.
     */
    private val keyMapper = HashMap<String, String>().apply {
        // Mappings could be added here when sorting with an different key
        put(MediaMetadataCompat.METADATA_KEY_TITLE, Media.TITLE_KEY)
        put(MediaMetadataCompat.METADATA_KEY_ALBUM, Media.ALBUM)
        put(MediaMetadataCompat.METADATA_KEY_ARTIST, Media.ARTIST)
        put(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, Media.TRACK)
        put(MediaMetadataCompat.METADATA_KEY_DURATION, Media.DURATION)
        put(MediaMetadataCompat.METADATA_KEY_YEAR, Albums.LAST_YEAR)
        put(MusicDao.METADATA_KEY_DATE_ADDED, Media.DATE_ADDED)
    }

    /**
     * Given an sorting clause of format "`KEY1 (ASC | DESC), KEY2 (ASC | DESC) ...`",
     * replaces keys expressed as media metadata keys by their MediaStore equivalent, if exists.
     * The result is sanitized and may be used as a SQL ORDER BY clause to query MediaStore.
     *
     * @param sorting A sorting clause of the format "`KEY1 (ASC | DESC), KEY2 (ASC | DESC) ...`",
     * where keys are standard `MediaMetadataCompat.METADATA_KEY_*` keys or `MusicDao.*` ones.
     *
     * @return The corresponding sorting clause to be used in `MediaStore` queries.
     */
    internal fun translateSortingClause(sorting: String): String {
        // Split conditions, and rebuild them sanitized and with keys replaced by media store columns
        return sorting.split(", ").joinToString(", ") { condition ->
            val splitCond = condition.trim().split(' ')
            val metadataKey = splitCond[0].trim()

            val mediaStoreKey = keyMapper[metadataKey]
                    ?: throw UnsupportedOperationException("Unsupported sort key: $metadataKey")

            if (splitCond.size > 1 && splitCond[1].contains("DESC")) {
                mediaStoreKey + " DESC"
            } else mediaStoreKey
        }
    }

    override fun getTracks(criteria: Map<String, Any>?,
                           sorting: String?): Observable<MediaMetadataCompat> {

        // Translate the client's sorting clause to a MediaStore ORDER BY clause.
        val orderByClause = sorting?.let(this::translateSortingClause) ?: Media.DEFAULT_SORT_ORDER

        // Load all metadata from MediaStore and put them all in memory cache for faster reuse
        val trackStream = loadFromMediaStore(orderByClause).doOnNext { metadata ->
            val musicId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID).toLong()
            metadataCache.put(musicId, metadata)
        }

        if (criteria == null) {
            return trackStream
        }

        // Apply selection criteria, if any.
        return trackStream.filter { metadata ->
            criteria.all { (key, value) -> metadata.bundle.get(key) == value }
        }
    }

    private fun loadFromMediaStore(sorting: String): Observable<MediaMetadataCompat> {
        if (!PermissionUtil.hasExternalStoragePermission(context)) {
            Log.i(TAG, "No permission to access external storage.")
            return Observable.empty()
        }

        return Observable.create { emitter ->

            val cursor = resolver.query(Media.EXTERNAL_CONTENT_URI, MEDIA_PROJECTION,
                    MEDIA_SELECTION_CLAUSE, null, sorting)

            cursor?.use {
                // Memorize cursor column indexes for faster lookup
                val colId = it.getColumnIndexOrThrow(BaseColumns._ID)
                val colTitle = it.getColumnIndexOrThrow(Media.TITLE)
                val colAlbum = it.getColumnIndexOrThrow(Media.ALBUM)
                val colArtist = it.getColumnIndexOrThrow(Media.ARTIST)
                val colDuration = it.getColumnIndexOrThrow(Media.DURATION)
                val colTrackNo = it.getColumnIndexOrThrow(Media.TRACK)
                val colTitleKey = it.getColumnIndexOrThrow(Media.TITLE_KEY)
                val colAlbumId = it.getColumnIndexOrThrow(Media.ALBUM_ID)
                val colArtistId = it.getColumnIndexOrThrow(Media.ARTIST_ID)
                val colDateAdded = it.getColumnIndexOrThrow(Media.DATE_ADDED)

                val builder = MediaMetadataCompat.Builder()

                // Fetch data from cursor
                while (cursor.moveToNext() && !emitter.isDisposed) {
                    val musicId = it.getLong(colId)
                    val albumId = it.getLong(colAlbumId)
                    val artUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId)
                    val trackNo = it.getLong(colTrackNo)
                    val mediaUri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, musicId)

                    builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, musicId.toString())
                            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, it.getString(colTitle))
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, it.getString(colAlbum))
                            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, it.getString(colArtist))
                            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, it.getLong(colDuration))
                            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNo % 1000)
                            .putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, trackNo / 1000)
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artUri.toString())
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mediaUri.toString())
                            .putLong(MusicDao.METADATA_KEY_DATE_ADDED, it.getLong(colDateAdded))
                            .putString(MusicDao.METADATA_KEY_TITLE_KEY, it.getString(colTitleKey))
                            .putLong(MusicDao.METADATA_KEY_ALBUM_ID, albumId)
                            .putLong(MusicDao.METADATA_KEY_ARTIST_ID, it.getLong(colArtistId))

                    val metadata = builder.build()
                    emitter.onNext(metadata)
                }

            } ?: Log.e(TAG, "Track metadata query failed: null cursor")

            emitter.onComplete()
        }
    }

    override fun findTrack(musicId: String): Maybe<MediaMetadataCompat> {
        return Maybe.defer {

            if (metadataCache.size() == 0) {

                // Cache is not initialized, fill it before searching for a track
                loadFromMediaStore(Media.DEFAULT_SORT_ORDER).doOnNext { metadata ->
                    val id = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID).toLong()
                    metadataCache.put(id, metadata)
                }
            }

            // Search the value in the cache. if not found, it will not be emitted.
            val cachedMetadata: MediaMetadataCompat? = metadataCache.get(musicId.toLong())
            Maybe.just(cachedMetadata)
        }
    }

    override fun getAlbums(criteria: Map<String, Any>?, sorting: String?): Observable<MediaDescriptionCompat> {
        if (!PermissionUtil.hasExternalStoragePermission(context)) {
            Log.i(TAG, "Could not load albums : no permission to access external storage.")
            return Observable.empty()
        }

        return Observable.create { emitter ->

            // Translate criteria filtering into WHERE clause
            val whereClause = criteria?.keys?.joinToString(", ")
            val whereArgs = criteria?.values?.map(Any::toString)?.toTypedArray()

            // Translate sorting clause to an SQL ORDER BY clause with MediaStore keys
            val orderByClause = if (sorting != null) translateSortingClause(sorting) else
                Albums.DEFAULT_SORT_ORDER

            val cursor = resolver.query(Albums.EXTERNAL_CONTENT_URI, ALBUM_PROJECTION,
                    whereClause, whereArgs, orderByClause)

            cursor?.use {
                // Memorize cursor column indexes for faster lookup
                val colId = it.getColumnIndexOrThrow(Albums._ID)
                val colTitle = it.getColumnIndexOrThrow(Albums.ALBUM)
                val colKey = it.getColumnIndexOrThrow(Albums.ALBUM_KEY)
                val colArtist = it.getColumnIndexOrThrow(Albums.ARTIST)
                val colYear = it.getColumnIndexOrThrow(Albums.LAST_YEAR)
                val colSongCount = it.getColumnIndexOrThrow(Albums.NUMBER_OF_SONGS)

                val builder = MediaDescriptionCompat.Builder()

                while (it.moveToNext() && !emitter.isDisposed) {
                    val albumId = it.getLong(colId)
                    val mediaId = MediaID.createMediaID(null, MediaID.ID_ALBUMS, albumId.toString())
                    val artUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId)

                    val extras = Bundle(3).apply {
                        putString(MediaItems.EXTRA_ALBUM_KEY, it.getString(colKey))
                        putInt(MediaItems.EXTRA_NUMBER_OF_TRACKS, it.getInt(colSongCount))
                        putInt(MediaItems.EXTRA_YEAR, it.getInt(colYear))
                    }

                    builder.setMediaId(mediaId)
                            .setTitle(it.getString(colTitle))
                            .setSubtitle(it.getString(colArtist)) // artist
                            .setIconUri(artUri)
                            .setExtras(extras)

                    emitter.onNext(builder.build())
                }
            } ?: Log.e(TAG, "Album query failed: null cursor.")

            emitter.onComplete()
        }
    }

    override fun getArtists(): Observable<MediaDescriptionCompat> {
        if (!PermissionUtil.hasExternalStoragePermission(context)) {
            Log.i(TAG, "Could not load artists: no permission to access external storage.")
            return Observable.empty()
        }

        // Accumulate artists in a list before emitting them in order to sort results
        return Observable.fromCallable<List<MediaDescriptionCompat>> {
            val artistsCursor = resolver.query(Artists.EXTERNAL_CONTENT_URI, ARTIST_PROJECTION,
                    null, null, Artists.ARTIST)

            val albumsCursor = resolver.query(Albums.EXTERNAL_CONTENT_URI,
                    arrayOf(Albums._ID, Albums.ARTIST, Albums.LAST_YEAR),
                    null, null, ORDER_BY_MOST_RECENT)

            if (artistsCursor == null || albumsCursor == null) {
                Log.e(TAG, "Query for artists failed. Returning an empty list.")
                return@fromCallable emptyList()
            }

            val colId = artistsCursor.getColumnIndexOrThrow(Artists._ID)
            val colArtistName = artistsCursor.getColumnIndexOrThrow(Artists.ARTIST)
            val colArtistKey = artistsCursor.getColumnIndexOrThrow(Artists.ARTIST_KEY)
            val colTrackCount = artistsCursor.getColumnIndexOrThrow(Artists.NUMBER_OF_TRACKS)

            val colArtistAlbum = albumsCursor.getColumnIndexOrThrow(Albums.ARTIST)
            val colAlbumId = albumsCursor.getColumnIndexOrThrow(Albums._ID)

            val artists = ArrayList<MediaDescriptionCompat>(albumsCursor.count)
            val builder = MediaDescriptionCompat.Builder()

            artistsCursor.moveToFirst()
            albumsCursor.moveToFirst()

            // We need to find the most recent album for each artist to display its album art
            while (!artistsCursor.isAfterLast && !albumsCursor.isAfterLast) {
                var artistName = artistsCursor.getString(colArtistName)
                val artistInAlbum = albumsCursor.getString(colArtistAlbum)

                if (artistName < artistInAlbum) {
                    // Albums are ahead of artists. This might happen when no album is associated
                    // to this artist. We add it without an album art and move to the next.

                    val artistId = artistsCursor.getLong(colId)
                    val mediaId = MediaID.createMediaID(null, MediaID.ID_ARTISTS, artistId.toString())

                    val extras = Bundle(2).apply {
                        putString(MediaItems.EXTRA_TITLE_KEY, artistsCursor.getString(colArtistKey))
                        putInt(MediaItems.EXTRA_NUMBER_OF_TRACKS, artistsCursor.getInt(colTrackCount))
                    }

                    builder.setMediaId(mediaId)
                            .setTitle(artistsCursor.getString(colArtistName))
                            .setIconUri(null)
                            .setExtras(extras)

                    artists.add(builder.build())

                    artistsCursor.moveToNext()
                    if (!artistsCursor.isAfterLast) {
                        // Proceed to the next artist only if possible
                        artistName = artistsCursor.getString(colArtistName)
                    }
                }

                if (artistName == artistInAlbum) {
                    // As albums are sorted by descending release year, the first album to match
                    // with the name of the artist is the most recent one.
                    val artistId = artistsCursor.getLong(colId)
                    val albumId = albumsCursor.getLong(colAlbumId)
                    val mediaId = MediaID.createMediaID(null, MediaID.ID_ARTISTS, artistId.toString())

                    val extras = Bundle(2).apply {
                        putString(MediaItems.EXTRA_TITLE_KEY, artistsCursor.getString(colArtistKey))
                        putInt(MediaItems.EXTRA_NUMBER_OF_TRACKS, artistsCursor.getInt(colTrackCount))
                    }

                    builder.setMediaId(mediaId)
                            .setTitle(artistsCursor.getString(colArtistName))
                            .setIconUri(ContentUris.withAppendedId(ALBUM_ART_URI, albumId))
                            .setExtras(extras)

                    artists.add(builder.build())

                    // Look for the next artist
                    artistsCursor.moveToNext()
                }

                // Whether it is matching or not, move to the next album
                albumsCursor.moveToNext()
            }

            assert(artistsCursor.count == artists.size) {
                "Bad number of artists. Expecting ${artistsCursor.count}, found ${artists.size}"
            }

            artistsCursor.close()
            albumsCursor.close()

            artists.sortedBy { it.extras!!.getString(MediaItems.EXTRA_TITLE_KEY) }

        }.flatMap { Observable.fromIterable(it) }
    }

    override fun search(query: String?, extras: Bundle?): Single<List<MediaMetadataCompat>> {
        TODO("not implemented")
    }

    /**
     * Delete the track with the specified [trackId] from the device and from the MediaStore.
     * If no track exist with this id, the operation will terminate without an error.
     */
    override fun deleteTrack(trackId: String): Completable {
        if (!PermissionUtil.hasExternalStoragePermission(context)) {
            Log.i(TAG, "Could not delete track: no permission to access external storage.")
            return Completable.complete()
        }

        return Completable.fromAction {
            val cursor = resolver.query(Media.EXTERNAL_CONTENT_URI, arrayOf(Media.DATA),
                    SELECTION_TRACK_BY_ID, arrayOf(trackId), null)

            if (cursor == null || !cursor.moveToFirst()) {
                Log.w(TAG, "deleteTrack : attempt to delete a non existing track: id = $trackId")
                return@fromAction
            }

            val filepath = cursor.use {
                val colFilePath = cursor.getColumnIndexOrThrow(Media.DATA)
                cursor.getString(colFilePath)
            }

            val file = File(filepath)
            if (!file.exists()) {
                Log.w(TAG, "deleteTrack: attempt to delete a file that does not exist.")
                return@fromAction
            }

            if (file.delete()) {
                val musicId = trackId.toLong()
                // Delete from MediaStore only if the file has been successfully deleted
                val deletedUri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, musicId)
                resolver.delete(deletedUri, null, null)

                // Also remove this track from the cache if successfully deleted
                metadataCache.remove(musicId)
            }
        }
    }

    private companion object {
        const val TAG = "MediaStoreMusicDao"
        const val MEDIA_SELECTION_CLAUSE = "${Media.IS_MUSIC} = 1"
        const val SELECTION_TRACK_BY_ID = "${Media._ID} = ?"

        /**
         * ORDER BY clause to use when querying for albums associated with an artist.
         */
        const val ORDER_BY_MOST_RECENT = "${Albums.ARTIST} ASC, ${Albums.LAST_YEAR} DESC"

        @JvmField
        val ALBUM_ART_URI: Uri = Uri.parse("content://media/external/audio/albumart")

        @JvmField
        val MEDIA_PROJECTION = arrayOf(BaseColumns._ID, Media.TITLE, Media.ALBUM, Media.ARTIST,
                Media.DURATION, Media.TRACK, Media.TITLE_KEY, Media.ALBUM_ID, Media.ARTIST_ID,
                Media.DATE_ADDED)

        @JvmField
        val ALBUM_PROJECTION = arrayOf(Albums._ID, Albums.ALBUM, Albums.ALBUM_KEY, Albums.ARTIST,
                Albums.LAST_YEAR, Albums.NUMBER_OF_SONGS)

        @JvmField
        val ARTIST_PROJECTION = arrayOf(Artists._ID, Artists.ARTIST, Artists.ARTIST_KEY,
                Artists.NUMBER_OF_TRACKS)
    }
}