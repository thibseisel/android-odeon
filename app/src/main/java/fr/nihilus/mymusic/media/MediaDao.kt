package fr.nihilus.mymusic.media

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Bundle
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AlbumColumns.ALBUM
import android.provider.MediaStore.Audio.Albums
import android.provider.MediaStore.Audio.Media
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import fr.nihilus.mymusic.utils.MediaID
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
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
        var disposed = false
        val observer = object : ContentObserver(null) {
            override fun onChange(selfChange: Boolean, uri: Uri?) {
                val mediaMetadataList = loadMetadataFromMediastore()
                Log.d(TAG, "Received ${mediaMetadataList.size} metadata items.")
                emitter.onNext(mediaMetadataList)
            }
        }

        Log.d(TAG, "Start listening for metadata changes...")
        resolver.registerContentObserver(Media.EXTERNAL_CONTENT_URI, true, observer)
        emitter.setDisposable(object : Disposable {
            override fun isDisposed() = disposed

            override fun dispose() {
                if (!disposed) {
                    Log.d(TAG, "Disposing metadata change listener.")
                    resolver.unregisterContentObserver(observer)
                    emitter.onComplete()
                    disposed = true
                }
            }
        })
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

    open fun getAlbums(): Observable<List<MediaDescriptionCompat>> {
        return Observable.fromCallable(this::loadAlbumsFromMediaStore)
                .timeout(1000L, TimeUnit.MILLISECONDS)
    }

    private fun loadAlbumsFromMediaStore(): List<MediaDescriptionCompat> {
        val cursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ALBUM_PROJECTION,
                null, null, Albums.DEFAULT_SORT_ORDER)

        if (cursor == null) {
            Log.e(TAG, "Album query failed. Cursor is null.")
            return emptyList()
        }

        val colId = cursor.getColumnIndexOrThrow(BaseColumns._ID)
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
        return albums
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

        private val ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart")
        private val MEDIA_PROJECTION = arrayOf(BaseColumns._ID, Media.TITLE, Media.ALBUM,
                Media.ARTIST, Media.DURATION, Media.TRACK, Media.TITLE_KEY, Media.ALBUM_KEY,
                Media.ALBUM_ID, Media.ARTIST_ID, Media.DATA)
        private val ALBUM_PROJECTION = arrayOf(BaseColumns._ID, ALBUM, Albums.ALBUM_KEY,
                Albums.ARTIST, Albums.LAST_YEAR, Albums.NUMBER_OF_SONGS)
    }
}
