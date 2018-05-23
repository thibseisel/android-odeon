/*
 * Copyright 2018 Thibault Seisel
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

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.BaseColumns
import android.provider.MediaStore.Audio.*
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.LongSparseArray
import fr.nihilus.music.assert
import fr.nihilus.music.di.ServiceScoped
import fr.nihilus.music.media.*
import fr.nihilus.music.utils.requirePermission
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.ObservableEmitter
import io.reactivex.Single
import org.jetbrains.annotations.TestOnly
import timber.log.Timber
import javax.inject.Inject

/**
 * A data source that fetches its items from the Android MediaStore.
 * Items represents files that are stored on the device's external storage.
 *
 * @constructor
 * Instantiate this MusicDao implementation with the provided metadata cache.
 * This constructor should only be used as a way to manually inject cache for testing purposes.
 *
 * @param context The application context
 * @param metadataCache The cache into which this object stores its metadata.
 */
@ServiceScoped
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
        put(MusicDao.METADATA_KEY_DATE, Media.DATE_ADDED)
        put(MusicDao.METADATA_KEY_ARTIST_ID, Media.ARTIST_ID)
        put(MusicDao.METADATA_KEY_ALBUM_ID, Media.ALBUM_ID)
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
    private fun translateSortingClause(sorting: String): String {
        // Split conditions, and rebuild them sanitized and with keys replaced by media store columns
        return sorting.split(", ").joinToString(", ") { condition ->
            val splitCond = condition.trim().split(' ')
            val metadataKey = splitCond[0].trim()

            val mediaStoreKey = keyMapper[metadataKey]
                    ?: throw UnsupportedOperationException("Unsupported sort key: $metadataKey")

            if (splitCond.size > 1 && splitCond[1].contains("DESC")) {
                "$mediaStoreKey DESC"
            } else mediaStoreKey
        }
    }

    override fun getTracks(
        criteria: Map<String, Any>?,
        sorting: String?
    ): Observable<MediaMetadataCompat> {

        // Translate criteria filtering into WHERE clause with ? parameters
        val whereArgs = criteria?.values?.map(Any::toString)?.toTypedArray()

        val whereClause = criteria?.keys?.joinToString(", ") { key ->
            val mediaStoreKey = keyMapper[key]
                    ?: throw UnsupportedOperationException("Unsupported filter key: $key")
            "$mediaStoreKey = ?"
        }

        // Translate the client's sorting clause to a MediaStore ORDER BY clause.
        val orderByClause = sorting?.let(this::translateSortingClause) ?: Media.DEFAULT_SORT_ORDER

        // Load all metadata from MediaStore and put them all in memory cache for faster reuse
        return loadFromMediaStore(whereClause, whereArgs, orderByClause).doOnNext { metadata ->
            val musicId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID).toLong()
            metadataCache.put(musicId, metadata)
        }
    }

    private fun loadFromMediaStore(
        whereClause: String?,
        whereArgs: Array<String>?,
        sorting: String?
    ): Observable<MediaMetadataCompat> = Observable.create { emitter ->

        context.requirePermission(Manifest.permission.READ_EXTERNAL_STORAGE)

        // Restricts SQL WHERE clause to only music
        val clause = if (whereClause != null) {
            "$whereClause AND $MEDIA_SELECTION_CLAUSE"
        } else MEDIA_SELECTION_CLAUSE

        // Preload art Uri for each album to associate them with tracks
        val iconUris = LongSparseArray<String>()
        resolver.query(
            Albums.EXTERNAL_CONTENT_URI, arrayOf(Albums._ID, Albums.ALBUM_ART),
            null, null, Albums.DEFAULT_SORT_ORDER
        )?.use { cursor ->

            val colAlbumId = cursor.getColumnIndexOrThrow(Albums._ID)
            val colFilePath = cursor.getColumnIndexOrThrow(Albums.ALBUM_ART)

            while (cursor.moveToNext()) {
                cursor.getString(colFilePath)?.let { filepath ->
                    val albumId = cursor.getLong(colAlbumId)
                    iconUris.put(albumId, "file://$filepath")
                }
            }
        }

        val cursor = resolver.query(
            Media.EXTERNAL_CONTENT_URI, MEDIA_PROJECTION,
            clause, whereArgs, sorting
        )

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
                val trackNo = it.getLong(colTrackNo)
                val mediaUri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, musicId)
                val iconUri = iconUris[albumId]

                builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, musicId.toString())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, it.getString(colTitle))
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, it.getString(colAlbum))
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, it.getString(colArtist))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, it.getLong(colDuration))
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNo % 1000)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, trackNo / 1000)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, iconUri)
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mediaUri.toString())
                    .putLong(MusicDao.METADATA_KEY_DATE, it.getLong(colDateAdded))
                    .putString(MusicDao.METADATA_KEY_TITLE_KEY, it.getString(colTitleKey))
                    .putLong(MusicDao.METADATA_KEY_ALBUM_ID, albumId)
                    .putLong(MusicDao.METADATA_KEY_ARTIST_ID, it.getLong(colArtistId))

                val metadata = builder.build()
                emitter.onNext(metadata)
            }

        } ?: Timber.e("Track metadata query failed: null cursor")

        emitter.onComplete()
    }

    override fun findTrack(musicId: String): Maybe<MediaMetadataCompat> = Maybe.defer {
        // Search the value in the cache. If found emit it immediately.
        // Otherwise, read it from MediaStore and cache it.
        // Note that cache lookups are deferred until subscription to always have the latest data.
        val cachedMetadata: MediaMetadataCompat? = metadataCache.get(musicId.toLong())

        if (cachedMetadata != null) {
            Maybe.just(cachedMetadata)
        } else {
            loadFromMediaStore(WHERE_TRACK_BY_ID, arrayOf(musicId), null)
                .firstElement()
                .doOnSuccess { track ->
                    metadataCache.put(musicId.toLong(), track)
                }
        }
    }

    override fun getAlbums(
        criteria: Map<String, Any>?,
        sorting: String?
    ): Observable<MediaDescriptionCompat> = Observable.create { emitter ->

        context.requirePermission(Manifest.permission.READ_EXTERNAL_STORAGE)

        // Translate criteria filtering into WHERE clause with ? parameters
        val whereArgs = criteria?.values?.map(Any::toString)?.toTypedArray()

        val whereClause = criteria?.keys?.joinToString(", ") { key ->
            val mediaStoreKey = keyMapper[key]
                    ?: throw UnsupportedOperationException("Unsupported filter key: $key")
            "$mediaStoreKey = ?"
        }

        // Translate sorting clause to an SQL ORDER BY clause with MediaStore keys
        val orderByClause = if (sorting != null) translateSortingClause(sorting) else
            Albums.DEFAULT_SORT_ORDER

        val cursor = resolver.query(
            Albums.EXTERNAL_CONTENT_URI, ALBUM_PROJECTION,
            whereClause, whereArgs, orderByClause
        )

        cursor?.use {
            // Memorize cursor column indexes for faster lookup
            val colId = it.getColumnIndexOrThrow(Albums._ID)
            val colTitle = it.getColumnIndexOrThrow(Albums.ALBUM)
            val colKey = it.getColumnIndexOrThrow(Albums.ALBUM_KEY)
            val colArtist = it.getColumnIndexOrThrow(Albums.ARTIST)
            val colYear = it.getColumnIndexOrThrow(Albums.LAST_YEAR)
            val colSongCount = it.getColumnIndexOrThrow(Albums.NUMBER_OF_SONGS)
            val colAlbumArt = it.getColumnIndexOrThrow(Albums.ALBUM_ART)

            val builder = MediaDescriptionCompat.Builder()

            while (it.moveToNext() && !emitter.isDisposed) {
                val albumId = it.getLong(colId)
                val mediaId = mediaIdOf(CATEGORY_ALBUMS, albumId.toString())
                val artUri = it.getString(colAlbumArt)?.let { filepath ->
                    Uri.parse("file://$filepath")
                }

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
        } ?: Timber.e("Album query failed: null cursor.")

        emitter.onComplete()
    }

    override fun getArtists(): Observable<MediaDescriptionCompat> {
        // Accumulate results in a list before emitting.
        return Observable.fromCallable<List<MediaDescriptionCompat>> {
            context.requirePermission(Manifest.permission.READ_EXTERNAL_STORAGE)

            val artistsCursor = resolver.query(
                Artists.EXTERNAL_CONTENT_URI, ARTIST_PROJECTION,
                null, null, Artists.ARTIST
            )

            val albumsCursor = resolver.query(
                Albums.EXTERNAL_CONTENT_URI,
                arrayOf(Albums.ARTIST, Albums.ALBUM_ART),
                null, null, ORDER_BY_MOST_RECENT
            )

            if (artistsCursor == null || albumsCursor == null) {
                Timber.e("Query for artists failed. Returning an empty list.")
                return@fromCallable emptyList()
            }

            val colId = artistsCursor.getColumnIndexOrThrow(Artists._ID)
            val colArtistName = artistsCursor.getColumnIndexOrThrow(Artists.ARTIST)
            val colArtistKey = artistsCursor.getColumnIndexOrThrow(Artists.ARTIST_KEY)
            val colTrackCount = artistsCursor.getColumnIndexOrThrow(Artists.NUMBER_OF_TRACKS)

            val colArtistAlbum = albumsCursor.getColumnIndexOrThrow(Albums.ARTIST)
            val colAlbumArt = albumsCursor.getColumnIndexOrThrow(Albums.ALBUM_ART)

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
                    val mediaId =
                        mediaIdOf(CATEGORY_ARTISTS, artistId.toString())

                    val extras = Bundle(2).apply {
                        putString(MediaItems.EXTRA_TITLE_KEY, artistsCursor.getString(colArtistKey))
                        putInt(
                            MediaItems.EXTRA_NUMBER_OF_TRACKS,
                            artistsCursor.getInt(colTrackCount)
                        )
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
                    val mediaId =
                        mediaIdOf(CATEGORY_ARTISTS, artistId.toString())

                    val artistIconUri = albumsCursor.getString(colAlbumArt)?.let { iconPath ->
                        Uri.parse("file://$iconPath")
                    }

                    val extras = Bundle(2).apply {
                        putString(MediaItems.EXTRA_TITLE_KEY, artistsCursor.getString(colArtistKey))
                        putInt(
                            MediaItems.EXTRA_NUMBER_OF_TRACKS,
                            artistsCursor.getInt(colTrackCount)
                        )
                    }

                    builder.setMediaId(mediaId)
                        .setTitle(artistsCursor.getString(colArtistName))
                        .setIconUri(artistIconUri)
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
     * Delete all tracks whose id matches one in the specified [trackIds]
     * from the device and from the MediaStore.
     */
    override fun deleteTracks(trackIds: LongArray): Single<Int> = Single.fromCallable {
        context.requirePermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        // Delete those tracks from the MediaStore.
        resolver.delete(
            Media.EXTERNAL_CONTENT_URI,
            WHERE_IN_TRACK_IDS,
            arrayOf(trackIds.joinToString(prefix = "(", separator = ",", postfix = ")"))
        ).also {
            // Remove those tracks from the cache if successfully deleted
            trackIds.forEach(metadataCache::delete)
        }
    }

    override fun getMediaChanges(): Observable<String> = Observable.create<String> {
        val observerThread = HandlerThread("MediaStoreChangeListener")
        observerThread.start()

        val observerHandler = Handler(observerThread.looper)
        val observer = MediaChangesObserver(it, observerHandler)
        resolver.registerContentObserver(Media.EXTERNAL_CONTENT_URI, true, observer)

        it.setCancellable {
            resolver.unregisterContentObserver(observer)
            observerThread.quit()
        }
    }

    private inner class MediaChangesObserver(
        private val emitter: ObservableEmitter<String>,
        handler: Handler
    ) : ContentObserver(handler) {

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            Timber.d("Media change detected: uri=$uri")

            val lastSegment = uri?.lastPathSegment ?: return
            lastSegment.toLongOrNull()?.let(this::dispatchTrackSpecificChange)
                    ?: dispatchGenericChange()
        }

        /**
         * Notify observers that tracks in the MediaStore has changed in some way.
         */
        private fun dispatchGenericChange() {
            metadataCache.clear()
            emitter.onNext(CATEGORY_MUSIC)
        }

        /**
         * Notify observers that a specific track in the MediaStore has been added or updated.
         *
         * @param musicId The id of the track that has changed in the MediaStore.
         */
        private fun dispatchTrackSpecificChange(musicId: Long) {
            val metadata = loadFromMediaStore(
                WHERE_TRACK_BY_ID,
                arrayOf(musicId.toString()),
                Media._ID
            ).blockingFirst()

            val relatedAlbumId = metadata.getLong(MusicDao.METADATA_KEY_ALBUM_ID)
            val relatedArtistId = metadata.getLong(MusicDao.METADATA_KEY_ARTIST_ID)

            // Notify changes for the related artist and album
            val changedArtistId = mediaIdOf(CATEGORY_ARTISTS, relatedArtistId.toString())
            val changedAlbumId = mediaIdOf(CATEGORY_ALBUMS, relatedAlbumId.toString())
            emitter.onNext(changedArtistId)
            emitter.onNext(changedAlbumId)

            // Notify changes for the track itself
            val changedTrackId = mediaIdOf(CATEGORY_MUSIC, musicId = musicId)
            emitter.onNext(changedTrackId)

            // Add/replace the metadata in the local cache
            metadataCache.put(musicId, metadata)
        }
    }

    private companion object {
        const val MEDIA_SELECTION_CLAUSE = "${Media.IS_MUSIC} = 1"
        const val WHERE_TRACK_BY_ID = "${Media._ID} = ?"
        const val WHERE_IN_TRACK_IDS = "${Media._ID} IN ?"

        /**
         * ORDER BY clause to use when querying for albums associated with an artist.
         */
        private const val ORDER_BY_MOST_RECENT = "${Albums.ARTIST} ASC, ${Albums.LAST_YEAR} DESC"

        @JvmField
        val MEDIA_PROJECTION = arrayOf(
            BaseColumns._ID, Media.TITLE, Media.ALBUM, Media.ARTIST,
            Media.DURATION, Media.TRACK, Media.TITLE_KEY, Media.ALBUM_ID, Media.ARTIST_ID,
            Media.DATE_ADDED
        )

        @JvmField
        val ALBUM_PROJECTION = arrayOf(
            Albums._ID, Albums.ALBUM, Albums.ALBUM_KEY, Albums.ARTIST,
            Albums.LAST_YEAR, Albums.NUMBER_OF_SONGS, Albums.ALBUM_ART
        )

        @JvmField
        val ARTIST_PROJECTION = arrayOf(
            Artists._ID, Artists.ARTIST, Artists.ARTIST_KEY,
            Artists.NUMBER_OF_TRACKS
        )
    }
}