package fr.nihilus.mymusic.media

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
import fr.nihilus.mymusic.utils.MediaID
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import java.io.File
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val TAG = "MediaStoreMusicDao"
private const val MEDIA_SELECTION_CLAUSE = "${Media.IS_MUSIC} = 1"

private const val SELECTION_TRACK_BY_ID = "${Media._ID} = ?"
private const val SELECTION_ALBUM_TRACKS = "${Media.ALBUM_ID} = ?"
private const val SELECTION_ARTIST_TRACKS = "${Media.ARTIST_ID} = ?"
private const val ARTIST_ALBUMS_ORDER = "${Artists.Albums.LAST_YEAR} DESC"

/**
 * ORDER BY clause to use when querying for albums associated with an artist.
 */
private const val ORDER_BY_MOST_RECENT = "${Albums.ARTIST} ASC, ${Albums.LAST_YEAR} DESC"
private val ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart")
private val MEDIA_PROJECTION = arrayOf(BaseColumns._ID, Media.TITLE, Media.ALBUM, Media.ARTIST,
        Media.DURATION, Media.TRACK, Media.TITLE_KEY, Media.ALBUM_KEY, Media.ALBUM_ID,
        Media.ARTIST_ID, Media.DATA)
private val ALBUM_PROJECTION = arrayOf(Albums._ID, Albums.ALBUM, Albums.ALBUM_KEY, Albums.ARTIST,
        Albums.LAST_YEAR, Albums.NUMBER_OF_SONGS)

private val ARTIST_PROJECTION = arrayOf(Artists._ID, Artists.ARTIST, Artists.ARTIST_KEY,
        Artists.NUMBER_OF_TRACKS)

/**
 * A music datasource that fetches its items from the Android mediastore.
 * Items represents files that are stored on the device's external storage.
 */
@Singleton
class MediaStoreMusicDao
@Inject constructor(@Named("Application") context: Context) : MusicDao {
    private val resolver: ContentResolver = context.contentResolver

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
        return Observable.fromCallable { loadMetadata(null, null, Media.TITLE_KEY) }
        //.concatWith(mediaChanges)
    }

    override fun getTrack(musicId: String): Maybe<MediaMetadataCompat> {
        return Maybe.fromCallable {
            val trackList = loadMetadata(SELECTION_TRACK_BY_ID,
                    arrayOf(musicId), Media.DEFAULT_SORT_ORDER)

            if (trackList.isNotEmpty()) trackList[0] else null
        }
    }

    private fun loadMetadata(selection: String?, selectionArgs: Array<String>?,
                             sortOrder: String?): List<MediaMetadataCompat> {
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
                val artistName = artistsCursor.getString(colArtistName)
                val artistInAlbum = albumsCursor.getString(colArtistAlbum)

                if (artistName == artistInAlbum) {
                    // As albums are sorted by descending release year, the first album to match
                    // with the name of the artist is the most recent one.
                    val artistId = artistsCursor.getLong(colId)
                    val albumId = albumsCursor.getLong(colAlbumId)
                    val mediaId = MediaID.createMediaID(null, MediaID.ID_ARTISTS, artistId.toString())

                    val extras = Bundle(2)
                    extras.putString(MediaItems.EXTRA_TITLE_KEY, artistsCursor.getString(colArtistKey))
                    extras.putInt(MediaItems.EXTRA_NUMBER_OF_TRACKS, artistsCursor.getInt(colTrackCount))

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
        return Observable.fromCallable {
            loadMetadata(SELECTION_ARTIST_TRACKS, arrayOf(artistId), Media.TITLE_KEY)
        }
    }

    /**
     * Return an observable dataset of albums that are produced by a given artist.
     * @param artistId unique identifier of the artist
     * @return informations of albums from this artist sorted by descending release date
     */
    override fun getArtistAlbums(artistId: String): Observable<List<MediaDescriptionCompat>> {
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

    /**
     * Delete the track with the specified [trackId] from the device and from the MediaStore.
     * If no track exist with this id, the operation will terminate without an error.
     */
    override fun deleteTrack(trackId: String): Completable {
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
}