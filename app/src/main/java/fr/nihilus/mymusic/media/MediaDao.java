package fr.nihilus.mymusic.media;

import android.app.Application;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio.Media;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import static android.provider.MediaStore.Audio.AudioColumns.ARTIST_ID;
import static android.provider.MediaStore.Audio.AudioColumns.TITLE_KEY;

@Singleton
class MediaDao {
    private static final String TAG = "MediaDao";
    private static final String[] MEDIA_PROJECTION = {BaseColumns._ID, Media.TITLE, Media.ALBUM,
            Media.ARTIST, Media.DURATION, Media.TRACK, TITLE_KEY, Media.ALBUM_KEY,
            Media.ALBUM_ID, ARTIST_ID, Media.DATA};
    private static final String MEDIA_SELECTION_CLAUSE = Media.IS_MUSIC + " = 1";
    private static final Uri ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart");
    private static final String CUSTOM_META_TITLE_KEY = "title_key";
    private static final String CUSTOM_META_ALBUM_ID = "album_id";
    private static final String CUSTOM_META_ARTIST_ID = "artist_id";

    private final ContentResolver mResolver;

    @Inject
    MediaDao(@NonNull Application app) {
        mResolver = app.getContentResolver();
    }

    List<MediaMetadataCompat> getAllTracks() {
        return loadMetadataFromMediastore();
    }

    private List<MediaMetadataCompat> loadMetadataFromMediastore() {
        final Cursor cursor = mResolver.query(Media.EXTERNAL_CONTENT_URI, MEDIA_PROJECTION,
                MEDIA_SELECTION_CLAUSE, null, TITLE_KEY);

        if (cursor == null) {
            Log.e(TAG, "getTracksMetadata: track metadata query failed (null cursor)");
            return Collections.emptyList();
        }

        // Cursor columns shortcuts
        final int colId = cursor.getColumnIndexOrThrow(BaseColumns._ID);
        final int colTitle = cursor.getColumnIndexOrThrow(Media.TITLE);
        final int colAlbum = cursor.getColumnIndexOrThrow(Media.ALBUM);
        final int colArtist = cursor.getColumnIndexOrThrow(Media.ARTIST);
        final int colDuration = cursor.getColumnIndexOrThrow(Media.DURATION);
        final int colTrackNo = cursor.getColumnIndexOrThrow(Media.TRACK);
        final int colTitleKey = cursor.getColumnIndexOrThrow(Media.TITLE_KEY);
        final int colAlbumId = cursor.getColumnIndexOrThrow(Media.ALBUM_ID);
        final int colArtistId = cursor.getColumnIndexOrThrow(Media.ARTIST_ID);
        //final int colFilePath = cursor.getColumnIndexOrThrow(Media.DATA);

        List<MediaMetadataCompat> allTracks = new ArrayList<>(cursor.getCount());
        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();

        // Fetch data from cursor
        while (cursor.moveToNext()) {
            long musicId = cursor.getLong(colId);
            long albumId = cursor.getLong(colAlbumId);
            Uri artUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId);
            long trackNo = cursor.getLong(colTrackNo);
            Uri mediaUri = ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, musicId);

            builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(musicId))
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
                    .putLong(CUSTOM_META_ARTIST_ID, cursor.getLong(colArtistId));

            final MediaMetadataCompat metadata = builder.build();
            allTracks.add(metadata);
        }

        cursor.close();
        return allTracks;
    }

    void deleteTrack(MediaMetadataCompat track) {
        // TODO Delete from MediaStore and from disk
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
