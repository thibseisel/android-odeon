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
import fr.nihilus.music.inReversedOrder
import fr.nihilus.music.media.MediaItems
import fr.nihilus.music.toArray
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
        put(MusicDao.METADATA_DATE_ADDED, Media.DATE_ADDED)
    }

    /**
     * Metadata sorting supported by this MusicDao implementation.
     */
    private val supportedSorting = HashMap<String, Comparator<MediaMetadataCompat>>().apply {
        put(MediaMetadataCompat.METADATA_KEY_TITLE, SORT_TITLE_KEY)
        put(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, SORT_TRACK_NUMBER)
        put(MusicDao.METADATA_DATE_ADDED, SORT_DATE_ADDED)
        put(MusicDao.CUSTOM_META_TITLE_KEY, SORT_TITLE_KEY)
    }

    override fun getTracks(criteria: Map<String, Any>?,
                           sorting: String?): Observable<MediaMetadataCompat> {

        // Translate the client's sorting instruction to a MediaStore column
        val (sortKey, sortDescending) = translateSorting(sorting, Media.DEFAULT_SORT_ORDER)

        // Fetch from the MediaStore only if the memory cache is empty.
        val allTracks = Observable.defer {
            if (metadataCache.size() == 0) {

                val mediaStoreTrackSorting = if (sortDescending) (sortKey + " DESC") else sortKey

                // Load all metadata from MediaStore and put them all in memory cache for faster reuse
                loadFromMediaStore(mediaStoreTrackSorting).doOnNext { metadata ->
                    val musicId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID).toLong()
                    metadataCache.put(musicId, metadata)
                }
            } else {
                // All metadata are already in memory cache. Sort them according to 'sorting'.

                // Extract the metadata key from the sorting instruction
                val metadataSortingKey = sorting?.let {
                    it.split(' ')[0].trim()
                } ?: MusicDao.CUSTOM_META_TITLE_KEY

                // Create a defensive copy of the cache to allow sorting and prevent threading problems
                val cacheCopy = metadataCache.toArray()
                var sortingAlgorithm = supportedSorting.getOrElse(metadataSortingKey) {
                    throw UnsupportedOperationException("Unsupported sorting key: $metadataSortingKey")
                }

                // Reverse if sorting in descending order
                if (sortDescending) {
                    sortingAlgorithm = sortingAlgorithm.inReversedOrder()
                }

                cacheCopy.sortWith(sortingAlgorithm)
                Observable.fromArray(*cacheCopy)
            }
        }

        return if (criteria == null) allTracks else {
            allTracks.filter { metadata ->
                criteria.all { (key, value) -> metadata.bundle.get(key) == value }
            }
        }
    }

    /**
     * Convert a sorting instruction to a column name from MediaStore.
     * The provided default column name will be used if not mapping exists.
     *
     * @param sorting The sorting instruction issued by the client.
     * It is composed of the name of the metadata on which records should be sorted,
     * plus an optional `ASC` or `DESC` keyword to sort in ascending or descending order.
     * If none of those keywords is specified, this defaults to ASC.
     * @param defaultColumn The default MediaStore column to use for sorting if none is specified
     * or is unsupported.
     *
     * @return The translated key and a boolean whose value is `true`
     * if records should be sorted in descending order.
     */
    private fun translateSorting(sorting: String?, defaultColumn: String): Pair<String, Boolean> {
        if (sorting == null) {
            return defaultColumn to false
        }

        // Separate the key from its optional direction: ASC (ascending) or DESC (descending)
        val splitKey = sorting.split(' ')
        val metadataKey = splitKey[0].trim()
        val isDescending = splitKey.size > 1 && splitKey[1].contains("DESC", ignoreCase = true)

        val translatedKey = keyMapper.getOrElse(metadataKey) {
            Log.w(TAG, "No corresponding key for \"$metadataKey\" has been found. Using default.")
            defaultColumn
        }

        return translatedKey to isDescending
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
                            .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNo % 100)
                            .putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, trackNo / 100)
                            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artUri.toString())
                            .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mediaUri.toString())
                            .putLong(MusicDao.METADATA_DATE_ADDED, it.getLong(colDateAdded))
                            .putString(MusicDao.CUSTOM_META_TITLE_KEY, it.getString(colTitleKey))
                            .putLong(MusicDao.CUSTOM_META_ALBUM_ID, albumId)
                            .putLong(MusicDao.CUSTOM_META_ARTIST_ID, it.getLong(colArtistId))

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

    /**
     * Return an observable dataset of albums featuring music stored on this device.
     *
     * Each album is composed of :
     * - a media id
     * - a title
     * - a subtitle, which is the name of the artist that composed it
     * - a content URI pointing to the album art
     * - the year at which it was released ([MediaItems.EXTRA_YEAR])
     * - the number of songs it featured ([MediaItems.EXTRA_NUMBER_OF_TRACKS])
     * - a key used for alphabetic sorting ([MediaItems.EXTRA_TITLE_KEY]).
     *
     * Albums are sorted by name by default.
     */
    override fun getAlbums(criteria: Map<String, Any>?, sorting: String?): Observable<MediaDescriptionCompat> {
        if (!PermissionUtil.hasExternalStoragePermission(context)) {
            Log.i(TAG, "Could not load albums : no permission to access external storage.")
            return Observable.empty()
        }

        return Observable.create { emitter ->

            val whereClause = criteria?.keys?.joinToString(", ")
            val whereArgs = criteria?.values?.map(Any::toString)?.toTypedArray()

            val (column, sortDescending) = translateSorting(sorting, Albums.DEFAULT_SORT_ORDER)
            val orderBy = if (sortDescending) column + " DESC" else column

            val cursor = resolver.query(Albums.EXTERNAL_CONTENT_URI, ALBUM_PROJECTION,
                    whereClause, whereArgs, orderBy)

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

    /**
     * Return an observable dataset of artists that participated to composing
     * music stored on this device.
     *
     * Each artist is composed of :
     * - a media id
     * - its name
     * - a content URI pointing to the album art of the most recent album
     * - the number of songs it composed ([MediaItems.EXTRA_NUMBER_OF_TRACKS])
     * - a key used for alphabeting sorting ([MediaItems.EXTRA_TITLE_KEY]).
     *
     * Artists are sorted by name by default.
     */
    override fun getArtists(): Observable<List<MediaDescriptionCompat>> {
        if (!PermissionUtil.hasExternalStoragePermission(context)) {
            Log.i(TAG, "Could not load artists: no permission to access external storage.")
            return Observable.just(emptyList())
        }

        return Observable.fromCallable {
            val artistsCursor = resolver.query(Artists.EXTERNAL_CONTENT_URI, ARTIST_PROJECTION,
                    null, null, Artists.ARTIST)

            val albumsCursor = resolver.query(Albums.EXTERNAL_CONTENT_URI,
                    arrayOf(Albums._ID, Albums.ARTIST, Albums.LAST_YEAR),
                    null, null, ORDER_BY_MOST_RECENT)

            if (artistsCursor == null || albumsCursor == null) {
                Log.e(TAG, "Query for artists failed. Returning an empty list.")
                return@fromCallable emptyList<MediaDescriptionCompat>()
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
                    artistName = artistsCursor.getString(colArtistName)
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
        }
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

        @JvmField
        val SORT_TITLE_KEY = Comparator<MediaMetadataCompat> { a, b ->
            a.getString(MusicDao.CUSTOM_META_TITLE_KEY).compareTo(
                    b.getString(MusicDao.CUSTOM_META_TITLE_KEY))
        }

        @JvmField
        val SORT_TRACK_NUMBER = Comparator<MediaMetadataCompat> { a, b ->
            val aTrack = a.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER) * 1000L +
                    a.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)
            val bTrack = b.getLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER) * 1000L +
                    b.getLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER)
            (aTrack - bTrack).toInt()
        }

        @JvmField
        val SORT_DATE_ADDED = Comparator<MediaMetadataCompat> { a, b ->
            (a.getLong(MediaMetadataCompat.METADATA_KEY_DATE) -
                    b.getLong(MediaMetadataCompat.METADATA_KEY_DATE)).toInt()
        }
    }
}