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
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore.Audio.*
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import android.util.LongSparseArray
import fr.nihilus.music.asObservable
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
    }

    /**
     * Observe changes in [android.provider.MediaStore] and publish updated metadata when a change occur.
     */
    private val mediaChanges = Observable.create<List<MediaMetadataCompat>> { emitter ->
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                val mediaMetadataList = loadMetadata(null, null, Media.TITLE_KEY)
                Log.d(TAG, "Received ${mediaMetadataList.size} metadata items.")
                emitter.onNext(mediaMetadataList)
            }
        }

        Log.d(TAG, "Start listening for metadata changes...")
        resolver.registerContentObserver(Media.EXTERNAL_CONTENT_URI, true, observer)

        emitter.setCancellable {
            Log.d(TAG, "Disposing metadata change listener.")
            resolver.unregisterContentObserver(observer)
            emitter.onComplete()
        }
    }

    /**
     * Return an observable dataset of tracks stored on the device.
     * When subscribed, the returned Observable will trigger an initial load,
     * then listen for any change to the tracks.
     *
     * When done listening, you should dispose the listener to avoid memory leaks
     * due to observing track changes.
     */
    override fun getAllTracks(): Observable<List<MediaMetadataCompat>> {
        return Observable.fromCallable {
            loadMetadata(null, null, Media.TITLE_KEY)
        }
        //.concatWith(mediaChanges)
    }

    /**
     *
     */
    fun getTracksImpl(criterias: Map<String, String>,
                      sorting: String): Observable<MediaMetadataCompat> {

        // Fetch from the MediaStore only if the memory cache is empty.
        return Observable.defer {
            if (metadataCache.size() == 0) {
                // translate the metadata key to one understood by MediaStore.
                // Throw an exception if no mapping is found to notify the developer
                // that this sorting key should be added to the map.
                val sortKey = keyMapper[sorting]
                        ?: throw UnsupportedOperationException("Untranslated metadata key: $sorting")

                // Load all metadata from MediaStore and put them all in memory cache for faster reuse
                loadFromMediaStore(sortKey).doOnNext { metadata ->
                    val musicId = metadata.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID).toLong()
                    metadataCache.put(musicId, metadata)
                }
            } else {
                metadataCache.asObservable().sorted { a, b ->
                    a.getString(sorting).compareTo(b.getString(sorting))
                }
            }
        }.filter { metadata ->
            criterias.all { (key, value) -> metadata.getString(key) == value }
        }
    }

    private fun loadFromMediaStore(sorting: String): Observable<MediaMetadataCompat> {
        if (!PermissionUtil.hasExternalStoragePermission(context)) {
            Log.i(TAG, "No permission to access external storage.")
            return Observable.empty()
        }

        val cursor = resolver.query(Media.EXTERNAL_CONTENT_URI, MEDIA_PROJECTION,
                MEDIA_SELECTION_CLAUSE, null, sorting)

        if (cursor == null) {
            Log.e(TAG, "getTracksMetadata: track metadata query failed (null cursor)")
            return Observable.empty()
        }

        // Memorize column indexes in cursor for faster lookup
        val colId = cursor.getColumnIndexOrThrow(BaseColumns._ID)
        val colTitle = cursor.getColumnIndexOrThrow(Media.TITLE)
        val colAlbum = cursor.getColumnIndexOrThrow(Media.ALBUM)
        val colArtist = cursor.getColumnIndexOrThrow(Media.ARTIST)
        val colDuration = cursor.getColumnIndexOrThrow(Media.DURATION)
        val colTrackNo = cursor.getColumnIndexOrThrow(Media.TRACK)
        val colTitleKey = cursor.getColumnIndexOrThrow(Media.TITLE_KEY)
        val colAlbumId = cursor.getColumnIndexOrThrow(Media.ALBUM_ID)
        val colArtistId = cursor.getColumnIndexOrThrow(Media.ARTIST_ID)

        val allTracks = ArrayList<MediaMetadataCompat>(cursor.count)
        val builder = MediaMetadataCompat.Builder()

        // Fetch data from cursor
        while (cursor.moveToNext()) {
            val musicId = cursor.getLong(colId)
            val albumId = cursor.getLong(colAlbumId)
            val artUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId)
            val trackNo = cursor.getLong(colTrackNo)
            val mediaUri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, musicId)

            builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, musicId.toString())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, cursor.getString(colTitle))
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, cursor.getString(colAlbum))
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, cursor.getString(colArtist))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, cursor.getLong(colDuration))
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNo % 100)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, trackNo / 100)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artUri.toString())
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mediaUri.toString())
                    .putString(MusicDao.CUSTOM_META_TITLE_KEY, cursor.getString(colTitleKey))
                    .putLong(MusicDao.CUSTOM_META_ALBUM_ID, albumId)
                    .putLong(MusicDao.CUSTOM_META_ARTIST_ID, cursor.getLong(colArtistId))

            val metadata = builder.build()
            allTracks.add(metadata)

            assert(cursor.count == allTracks.size) {
                "Bad number of metadata. Expecting ${cursor.count}, found ${allTracks.size}"
            }
        }

        cursor.close()
        return Observable.fromIterable(allTracks)
    }

    override fun getTracks(whereClause: String?, whereArgs: Array<out String>?,
                           sorting: String?): Observable<List<MediaMetadataCompat>> {
        return Observable.fromCallable {
            loadMetadata(whereClause, whereArgs, sorting)
        }
    }

    override fun findTrack(musicId: String): Maybe<MediaMetadataCompat> {
        return Maybe.fromCallable {
            val trackList = loadMetadata(SELECTION_TRACK_BY_ID,
                    arrayOf(musicId), Media.DEFAULT_SORT_ORDER)

            if (trackList.isNotEmpty()) trackList[0] else null
        }
    }

    private fun loadMetadata(selection: String?, selectionArgs: Array<out String>?,
                             sortOrder: String?): List<MediaMetadataCompat> {

        if (!PermissionUtil.hasExternalStoragePermission(context)) {
            Log.i(TAG, "No permission to access external storage.")
            return emptyList()
        }

        val whereClause = StringBuilder(MEDIA_SELECTION_CLAUSE)
        if (selection != null) {
            whereClause.append(" AND ")
            whereClause.append(selection)
        }

        val cursor = resolver.query(Media.EXTERNAL_CONTENT_URI, MEDIA_PROJECTION,
                whereClause.toString(), selectionArgs, sortOrder)
        if (cursor == null) {
            Log.e(TAG, "getTracksMetadata: track metadata query failed (null cursor)")
            return emptyList()
        }

        // Cursor columns shortcuts
        val colId = cursor.getColumnIndexOrThrow(BaseColumns._ID)
        val colTitle = cursor.getColumnIndexOrThrow(Media.TITLE)
        val colAlbum = cursor.getColumnIndexOrThrow(Media.ALBUM)
        val colArtist = cursor.getColumnIndexOrThrow(Media.ARTIST)
        val colDuration = cursor.getColumnIndexOrThrow(Media.DURATION)
        val colTrackNo = cursor.getColumnIndexOrThrow(Media.TRACK)
        val colTitleKey = cursor.getColumnIndexOrThrow(Media.TITLE_KEY)
        val colAlbumId = cursor.getColumnIndexOrThrow(Media.ALBUM_ID)
        val colArtistId = cursor.getColumnIndexOrThrow(Media.ARTIST_ID)
        //val colFilePath = cursor.getColumnIndexOrThrow(Media.DATA);

        val allTracks = ArrayList<MediaMetadataCompat>(cursor.count)
        val builder = MediaMetadataCompat.Builder()

        // Fetch data from cursor
        while (cursor.moveToNext()) {
            val musicId = cursor.getLong(colId)
            val albumId = cursor.getLong(colAlbumId)
            val artUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId)
            val trackNo = cursor.getLong(colTrackNo)
            val mediaUri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, musicId)

            builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, musicId.toString())
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, cursor.getString(colTitle))
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, cursor.getString(colAlbum))
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, cursor.getString(colArtist))
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, cursor.getLong(colDuration))
                    .putLong(MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER, trackNo % 100)
                    .putLong(MediaMetadataCompat.METADATA_KEY_DISC_NUMBER, trackNo / 100)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI, artUri.toString())
                    .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, mediaUri.toString())
                    .putString(MusicDao.CUSTOM_META_TITLE_KEY, cursor.getString(colTitleKey))
                    .putLong(MusicDao.CUSTOM_META_ALBUM_ID, albumId)
                    .putLong(MusicDao.CUSTOM_META_ARTIST_ID, cursor.getLong(colArtistId))

            val metadata = builder.build()
            allTracks.add(metadata)
        }

        assert(cursor.count == allTracks.size) {
            "Bad number of metadata. Expecting ${cursor.count}, found ${allTracks.size}"
        }

        cursor.close()
        return allTracks
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
    override fun getAlbums(): Observable<List<MediaDescriptionCompat>> {
        if (!PermissionUtil.hasExternalStoragePermission(context)) {
            Log.i(TAG, "Could not load albums : no permission to access external storage.")
            return Observable.just(emptyList())
        }

        return Observable.fromCallable {
            val cursor = resolver.query(Albums.EXTERNAL_CONTENT_URI, ALBUM_PROJECTION,
                    null, null, Albums.DEFAULT_SORT_ORDER)

            if (cursor == null) {
                Log.e(TAG, "Album query failed. Returning an empty list.")
                return@fromCallable emptyList<MediaDescriptionCompat>()
            }

            cursor.use {
                extractAlbums(it)
            }
        }
    }

    private fun extractAlbums(cursor: Cursor): List<MediaDescriptionCompat> {
        val colId = cursor.getColumnIndexOrThrow(Albums._ID)
        val colTitle = cursor.getColumnIndexOrThrow(Albums.ALBUM)
        val colKey = cursor.getColumnIndexOrThrow(Albums.ALBUM_KEY)
        val colArtist = cursor.getColumnIndexOrThrow(Albums.ARTIST)
        val colYear = cursor.getColumnIndexOrThrow(Albums.LAST_YEAR)
        val colSongCount = cursor.getColumnIndexOrThrow(Albums.NUMBER_OF_SONGS)

        val albums = ArrayList<MediaDescriptionCompat>(cursor.count)
        val builder = MediaDescriptionCompat.Builder()

        while (cursor.moveToNext()) {
            val albumId = cursor.getLong(colId)
            val mediaId = MediaID.createMediaID(null, MediaID.ID_ALBUMS, albumId.toString())
            val artUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId)

            val extras = Bundle(3)
            extras.putString(MediaItems.EXTRA_ALBUM_KEY, cursor.getString(colKey))
            extras.putInt(MediaItems.EXTRA_NUMBER_OF_TRACKS, cursor.getInt(colSongCount))
            extras.putInt(MediaItems.EXTRA_YEAR, cursor.getInt(colYear))

            builder.setMediaId(mediaId)
                    .setTitle(cursor.getString(colTitle))
                    .setSubtitle(cursor.getString(colArtist)) // artiste
                    .setIconUri(artUri)
                    .setExtras(extras)

            albums.add(builder.build())
        }

        assert(cursor.count == albums.size) {
            "Bad number of albums. Expecting ${cursor.count}, found ${albums.size}"
        }

        return albums
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

    /**
     * Return an observable dataset of tracks that are part of a given album.
     * @param albumId unique identifier of the album
     * @return track metadatas from this album sorted by track number
     */
    override fun getAlbumTracks(albumId: String): Observable<List<MediaMetadataCompat>> {
        if (!PermissionUtil.hasExternalStoragePermission(context)) {
            Log.i(TAG, "Could not load album tracks: no permission to access external storage.")
            return Observable.just(emptyList())
        }

        return Observable.fromCallable {
            loadMetadata(SELECTION_ALBUM_TRACKS, arrayOf(albumId), Media.TRACK)
        }
    }

    /**
     * Return an observable dataset of tracks that are produced by a given artist.
     * @param artistId unique identifier of the artist
     * @return track metadatas from this artist sorted by track name
     */
    override fun getArtistTracks(artistId: String): Observable<List<MediaMetadataCompat>> {
        if (!PermissionUtil.hasExternalStoragePermission(context)) {
            Log.i(TAG, "Could not load artists tracks: no permission to access external storage.")
            return Observable.just(emptyList())
        }

        return Observable.fromCallable {
            loadMetadata(SELECTION_ARTIST_TRACKS, arrayOf(artistId), Media.TITLE_KEY)
        }
    }

    /**
     * Return an observable data set of albums that are produced by a given artist.
     * @param artistId unique identifier of the artist
     * @return information of albums from this artist sorted by descending release date
     */
    override fun getArtistAlbums(artistId: String): Observable<List<MediaDescriptionCompat>> {
        if (!PermissionUtil.hasExternalStoragePermission(context)) {
            Log.i(TAG, "Could not load artists items: no permission to access external storage.")
            return Observable.just(emptyList())
        }

        return Observable.fromCallable {
            val cursor = resolver.query(Artists.Albums.getContentUri("external", artistId.toLong()),
                    ALBUM_PROJECTION, null, null, ARTIST_ALBUMS_ORDER)

            if (cursor == null) {
                Log.e(TAG, "Failed retrieving albums for artist $artistId.")
                return@fromCallable emptyList<MediaDescriptionCompat>()
            }

            cursor.use {
                extractAlbums(it)
            }
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
                // Delete from MediaStore only if the file has been successfully deleted
                val deletedUri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI,
                        trackId.toLong())
                resolver.delete(deletedUri, null, null)
            }
        }
    }

    private companion object {
        const val TAG = "MediaStoreMusicDao"
        const val MEDIA_SELECTION_CLAUSE = "${Media.IS_MUSIC} = 1"

        const val SELECTION_TRACK_BY_ID = "${Media._ID} = ?"
        const val SELECTION_ALBUM_TRACKS = "${Media.ALBUM_ID} = ?"
        const val SELECTION_ARTIST_TRACKS = "${Media.ARTIST_ID} = ?"
        const val ARTIST_ALBUMS_ORDER = "${Artists.Albums.LAST_YEAR} DESC"

        /**
         * ORDER BY clause to use when querying for albums associated with an artist.
         */
        const val ORDER_BY_MOST_RECENT = "${Albums.ARTIST} ASC, ${Albums.LAST_YEAR} DESC"
        @JvmField
        val ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart")
        @JvmField
        val MEDIA_PROJECTION = arrayOf(BaseColumns._ID, Media.TITLE, Media.ALBUM, Media.ARTIST,
                Media.DURATION, Media.TRACK, Media.TITLE_KEY, Media.ALBUM_KEY, Media.ALBUM_ID,
                Media.ARTIST_ID, Media.DATA)
        @JvmField
        val ALBUM_PROJECTION = arrayOf(Albums._ID, Albums.ALBUM, Albums.ALBUM_KEY, Albums.ARTIST,
                Albums.LAST_YEAR, Albums.NUMBER_OF_SONGS)

        @JvmField
        val ARTIST_PROJECTION = arrayOf(Artists._ID, Artists.ARTIST, Artists.ARTIST_KEY,
                Artists.NUMBER_OF_TRACKS)
    }
}