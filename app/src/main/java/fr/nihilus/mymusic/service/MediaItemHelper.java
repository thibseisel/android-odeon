package fr.nihilus.mymusic.service;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AudioColumns;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;

import java.util.ArrayList;
import java.util.List;

import fr.nihilus.mymusic.utils.MediaID;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;

@SuppressWarnings("WrongConstant")
final class MediaItemHelper {

    @NonNull
    static List<MediaItem> getSongs(List<MediaMetadataCompat> metadataList) {
        List<MediaItem> result = new ArrayList<>();

        final MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder();
        for (MediaMetadataCompat meta : metadataList) {
            String musicId = meta.getString(METADATA_KEY_MEDIA_ID);
            String albumArtUri = meta.getString(METADATA_KEY_ALBUM_ART_URI);
            Bundle extras = new Bundle();
            extras.putString(AudioColumns.TITLE_KEY, meta.getString(MusicProvider.METADATA_TITLE_KEY));

            builder.setMediaId(MediaID.createMediaID(musicId, MediaID.ID_MUSIC))
                    .setExtras(extras)
                    .setTitle(meta.getString(METADATA_KEY_TITLE))
                    .setSubtitle(meta.getString(METADATA_KEY_ARTIST))
                    .setMediaUri(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
                            .appendEncodedPath(musicId)
                            .build());
            if (albumArtUri != null) {
                builder.setIconUri(Uri.parse(albumArtUri));
            }

            result.add(new MediaItem(builder.build(), MediaItem.FLAG_PLAYABLE));
        }
        return result;
    }

    @NonNull
    static List<MediaItem> getAlbums(Cursor cursor) {
        List<MediaItem> result = new ArrayList<>(cursor.getCount());
        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder();

        final int colId = cursor.getColumnIndexOrThrow(BaseColumns._ID);
        final int colTitle = cursor.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ALBUM);
        final int colKey = cursor.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ALBUM_KEY);
        final int colArtist = cursor.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.ARTIST);
        final int colYear = cursor.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.LAST_YEAR);
        final int colSongCount = cursor.getColumnIndexOrThrow(MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS);

        while (cursor.moveToNext()) {
            final long albumId = cursor.getLong(colId);
            final String mediaId = MediaID.createMediaID(null, MediaID.ID_ALBUMS, String.valueOf(albumId));
            final Uri artUri = ContentUris.withAppendedId(MusicProvider.ALBUM_ART_URI, albumId);

            builder.setMediaId(mediaId)
                    .setTitle(cursor.getString(colTitle))
                    .setSubtitle(cursor.getString(colArtist)) // artiste
                    .setIconUri(artUri);
            Bundle extras = new Bundle();
            extras.putString(MediaStore.Audio.AlbumColumns.ALBUM_KEY, cursor.getString(colKey));
            extras.putInt(MediaStore.Audio.AlbumColumns.NUMBER_OF_SONGS, cursor.getInt(colSongCount));
            extras.putInt(MediaStore.Audio.AlbumColumns.LAST_YEAR, cursor.getInt(colYear));
            builder.setExtras(extras);

            result.add(new MediaItem(builder.build(), MediaItem.FLAG_BROWSABLE | MediaItem.FLAG_PLAYABLE));
        }
        return result;
    }
}
