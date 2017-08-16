package fr.nihilus.mymusic.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.CursorJoiner
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.*
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import fr.nihilus.mymusic.utils.MediaID
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
open class MediaDao
@Inject constructor(@Named("Application") context: Context) {
    private val resolver: ContentResolver = context.contentResolver

    /**
     * Observe changes in [android.provider.MediaStore] and publish updated metadata when a change occur.
     */
    private val mediaChanges = Observable.create<List<MediaMetadataCompat>> { emitter ->
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                val mediaMetadataList = loadMetadataFromMediastore()
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
    open fun getAllTracks(): Observable<List<MediaMetadataCompat>> {
        return Observable.fromCallable(this::loadMetadataFromMediastore)
                .timeout(1L, TimeUnit.SECONDS)
                .concatWith(mediaChanges)
    }

    private fun loadMetadataFromMediastore(): List<MediaMetadataCompat> {
        val cursor = resolver.query(Media.EXTERNAL_CONTENT_URI, MEDIA_PROJECTION,
                MEDIA_SELECTION_CLAUSE, null, Media.TITLE_KEY)

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
                    .putString(CUSTOM_META_TITLE_KEY, cursor.getString(colTitleKey))
                    .putLong(CUSTOM_META_ALBUM_ID, albumId)
                    .putLong(CUSTOM_META_ARTIST_ID, cursor.getLong(colArtistId))

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
    open fun getAlbums(): Observable<List<MediaDescriptionCompat>> {
        return Observable.fromCallable {
            val cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ALBUM_PROJECTION,
                    null, null, Albums.DEFAULT_SORT_ORDER)

            if (cursor == null) {
                Log.e(TAG, "Album query failed. Returning an empty list.")
                return@fromCallable emptyList<MediaDescriptionCompat>()
            }

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

            cursor.close()
            albums
        }.timeout(1L, TimeUnit.SECONDS)
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
    fun getArtists(): Observable<List<MediaDescriptionCompat>> {
        return Observable.fromCallable {
            val artistsCursor = resolver.query(Artists.EXTERNAL_CONTENT_URI, ARTIST_PROJECTION,
                    null, null, Artists.ARTIST)
            val albumsCursor = resolver.query(Albums.EXTERNAL_CONTENT_URI,
                    arrayOf(Albums._ID, Albums.ARTIST, MAX_LAST_YEAR),
                    GROUP_BY_ARTIST, null, Albums.ARTIST)

            if (artistsCursor == null || albumsCursor == null) {
                Log.e(TAG, "Query for artists failed. Returning an empty list.")
                return@fromCallable emptyList<MediaDescriptionCompat>()
            }

            val colId = artistsCursor.getColumnIndexOrThrow(Artists._ID)
            val colArtistName = artistsCursor.getColumnIndexOrThrow(Artists.ARTIST)
            val colArtistKey = artistsCursor.getColumnIndexOrThrow(Artists.ARTIST_KEY)
            val colTrackCount = artistsCursor.getColumnIndexOrThrow(Artists.NUMBER_OF_TRACKS)
            val colAlbumId = albumsCursor.getColumnIndexOrThrow(Albums._ID)

            val artists = ArrayList<MediaDescriptionCompat>(albumsCursor.count)
            val builder = MediaDescriptionCompat.Builder()

            val joiner = CursorJoiner(artistsCursor, arrayOf(Artists.ARTIST),
                    albumsCursor, arrayOf(Albums.ARTIST))

            for (result in joiner) {
                if (result == CursorJoiner.Result.BOTH) {
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
                }
            }

            artistsCursor.close()
            albumsCursor.close()

            artists.sortedBy { it.extras!!.getString(MediaItems.EXTRA_TITLE_KEY) }
        }.timeout(1L, TimeUnit.SECONDS)
    }

    open fun deleteTrack(track: MediaMetadataCompat) {
        // TODO("Delete from MediaStore and from disk")
    }

    internal companion object {
        private const val TAG = "MediaDao"
        private const val MEDIA_SELECTION_CLAUSE = "${Media.IS_MUSIC} = 1"
        internal const val CUSTOM_META_TITLE_KEY = "title_key"
        internal const val CUSTOM_META_ALBUM_ID = "album_id"
        internal const val CUSTOM_META_ARTIST_ID = "artist_id"

        private const val MAX_LAST_YEAR = "max(${Albums.LAST_YEAR})"
        private const val GROUP_BY_ARTIST = "1 = 1 GROUP BY ${Albums.ARTIST}"

        private val ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart")
        private val MEDIA_PROJECTION = arrayOf(BaseColumns._ID, Media.TITLE, Media.ALBUM,
                Media.ARTIST, Media.DURATION, Media.TRACK, Media.TITLE_KEY, Media.ALBUM_KEY,
                Media.ALBUM_ID, Media.ARTIST_ID, Media.DATA)
        private val ALBUM_PROJECTION = arrayOf(Albums._ID, Albums.ALBUM, Albums.ALBUM_KEY,
                Albums.ARTIST, Albums.LAST_YEAR, Albums.NUMBER_OF_SONGS)
        private val ARTIST_PROJECTION = arrayOf(Artists._ID, Artists.ARTIST, Artists.ARTIST_KEY,
                Artists.NUMBER_OF_TRACKS)
    }
}
