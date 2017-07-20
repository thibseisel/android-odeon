package fr.nihilus.mymusic.media

import android.app.Application
import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns
import android.provider.MediaStore.Audio.AudioColumns.ARTIST_ID
import android.provider.MediaStore.Audio.AudioColumns.TITLE_KEY
import android.provider.MediaStore.Audio.Media
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MediaDao
@Inject constructor(app: Application) {

    private val mResolver: ContentResolver = app.contentResolver

    val allTracks: List<MediaMetadataCompat>
        get() = loadMetadataFromMediastore()

    private fun loadMetadataFromMediastore(): List<MediaMetadataCompat> {
        val cursor: Cursor? = mResolver.query(Media.EXTERNAL_CONTENT_URI, MEDIA_PROJECTION,
                MEDIA_SELECTION_CLAUSE, null, TITLE_KEY)

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
                    .putText(MediaMetadataCompat.METADATA_KEY_TITLE, cursor.getString(colTitle))
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

    fun deleteTrack(track: MediaMetadataCompat) {
        // TODO Delete from MediaStore and from disk
        throw UnsupportedOperationException("Not yet implemented")
    }

    companion object {
        private const val TAG = "MediaDao"
        private const val MEDIA_SELECTION_CLAUSE = Media.IS_MUSIC + " = 1"
        private const val CUSTOM_META_TITLE_KEY = "title_key"
        private const val CUSTOM_META_ALBUM_ID = "album_id"
        private const val CUSTOM_META_ARTIST_ID = "artist_id"

        private val ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart")
        private val MEDIA_PROJECTION = arrayOf(BaseColumns._ID, Media.TITLE, Media.ALBUM,
                Media.ARTIST, Media.DURATION, Media.TRACK, TITLE_KEY, Media.ALBUM_KEY,
                Media.ALBUM_ID, ARTIST_ID, Media.DATA)
    }
}
