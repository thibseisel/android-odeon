package fr.nihilus.mymusic.playback;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.Albums;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import fr.nihilus.mymusic.utils.MediaIDHelper;
import fr.nihilus.mymusic.utils.PermissionUtil;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;

class MusicProvider implements MediaStore.Audio.AudioColumns {

    private static final String[] ALBUM_PROJECTION = {BaseColumns._ID, AlbumColumns.ALBUM,
            AlbumColumns.ALBUM_KEY, AlbumColumns.ARTIST,
            AlbumColumns.LAST_YEAR, AlbumColumns.NUMBER_OF_SONGS};
    private static final int NON_INITIALIZED = 0, INITIALIZING = 1, INITIALIZED = 2;
    private static final Uri ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart");
    private static final String TAG = "MusicProvider";
    private static final String[] PROJECTIONS = {_ID, TITLE, ARTIST, ALBUM, DURATION, ALBUM_ID};
    private final ConcurrentMap<String, MediaMetadataCompat> mMusicListById;
    private volatile int mCurrentState = NON_INITIALIZED;

    MusicProvider() {
        mMusicListById = new ConcurrentHashMap<>();
    }

    void retrieveMetadataAsync(@NonNull final Context context, final MusicReadyCallback callback) {
        Log.d(TAG, "retrieveMetadataAsync");
        if (mCurrentState == INITIALIZED) {
            Log.d(TAG, "retrieveMetadataAsync: music library already loaded.");
            callback.onMusicCatalogReady(true);
            return;
        }

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                retrieveMetadata(context);
                return mCurrentState;
            }

            @Override
            protected void onPostExecute(Integer state) {
                if (callback != null) {
                    Log.d(TAG, "onPostExecute: finished preparing music library.");
                    callback.onMusicCatalogReady(state == INITIALIZED);
                }
            }
        }.execute();
    }

    private synchronized void retrieveMetadata(@NonNull Context context) {
        if (mCurrentState == NON_INITIALIZED) {
            mCurrentState = INITIALIZING;

            if (!PermissionUtil.hasExternalStoragePermission(context)) {
                Log.w(TAG, "retrieveMetadata: trying to access external storage without permission.");
                return;
            }

            final Cursor cursor = context.getContentResolver().query(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, PROJECTIONS,
                    MediaStore.Audio.Media.IS_MUSIC + "=1", null,
                    MediaStore.Audio.Media.TITLE_KEY);

            if (cursor == null) {
                Log.w(TAG, "retrieveMetadata: cursor with media metadata is null.");
                return;
            }

            final int colID = cursor.getColumnIndexOrThrow(_ID);
            final int colTitle = cursor.getColumnIndexOrThrow(TITLE);
            final int colArtist = cursor.getColumnIndexOrThrow(ARTIST);
            final int colAlbum = cursor.getColumnIndexOrThrow(ALBUM);
            final int colDuration = cursor.getColumnIndexOrThrow(DURATION);
            final int colAlbumId = cursor.getColumnIndexOrThrow(ALBUM_ID);

            MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
            while (cursor.moveToNext()) {
                final String mediaID = cursor.getString(colID);
                final Uri artUri = ContentUris.withAppendedId(ALBUM_ART_URI, cursor.getLong(colAlbumId));

                Uri fileUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(colID));

                //noinspection WrongConstant
                builder.putString(METADATA_KEY_MEDIA_ID, mediaID)
                        .putString(METADATA_KEY_TITLE, cursor.getString(colTitle))
                        .putString(METADATA_KEY_ARTIST, cursor.getString(colArtist))
                        .putString(METADATA_KEY_ALBUM, cursor.getString(colAlbum))
                        .putLong(METADATA_KEY_DURATION, cursor.getLong(colDuration))
                        .putString(METADATA_KEY_ALBUM_ART_URI, artUri.toString());

                final MediaMetadataCompat metadata = builder.build();
                mMusicListById.put(mediaID, metadata);
            }
            cursor.close();
            mCurrentState = INITIALIZED;
        }
    }

    MediaMetadataCompat getMusic(String musicId) {
        return mMusicListById.containsKey(musicId)
                ? mMusicListById.get(musicId) : null;
    }

    void retrieveAlbumsAsync(@NonNull final Context context,
                             @NonNull final Callback<List<MediaItem>> callback) {
        new AsyncTask<Void, Void, List<MediaItem>>() {

            @Override
            protected List<MediaItem> doInBackground(Void... voids) {
                return retrieveAlbums(context);
            }

            @Override
            protected void onPostExecute(List<MediaItem> mediaItems) {
                callback.onReady(mediaItems);
            }
        }.execute();
    }

    private List<MediaItem> retrieveAlbums(@NonNull Context context) {
        if (!PermissionUtil.hasExternalStoragePermission(context)) {
            Log.w(TAG, "retrieveAlbums: missing External Storage permission.");
            return Collections.emptyList();
        }

        Cursor cursor = context.getContentResolver()
                .query(Albums.EXTERNAL_CONTENT_URI, ALBUM_PROJECTION,
                        null, null, Albums.DEFAULT_SORT_ORDER);

        if (cursor == null) {
            return Collections.emptyList();
        }

        List<MediaItem> albumList = new ArrayList<>();
        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder();

        final int colId = cursor.getColumnIndexOrThrow(BaseColumns._ID);
        final int colTitle = cursor.getColumnIndexOrThrow(AlbumColumns.ALBUM);
        final int colKey = cursor.getColumnIndexOrThrow(AlbumColumns.ALBUM_KEY);
        final int colArtist = cursor.getColumnIndexOrThrow(AlbumColumns.ARTIST);
        final int colYear = cursor.getColumnIndexOrThrow(AlbumColumns.LAST_YEAR);
        final int colSongCount = cursor.getColumnIndexOrThrow(AlbumColumns.NUMBER_OF_SONGS);

        while (cursor.moveToNext()) {
            final long albumId = cursor.getLong(colId);
            final String mediaId = MediaIDHelper.createMediaID(String.valueOf(albumId), MediaIDHelper.MEDIA_ID_ALBUMS);
            final Uri artUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId);

            builder.setMediaId(mediaId)
                    .setTitle(cursor.getString(colTitle))
                    .setSubtitle(cursor.getString(colArtist)) // artiste
                    .setIconUri(artUri);
            Bundle extras = new Bundle();
            extras.putString(AlbumColumns.ALBUM_KEY, cursor.getString(colKey));
            extras.putInt(AlbumColumns.NUMBER_OF_SONGS, cursor.getInt(colSongCount));
            extras.putInt(AlbumColumns.LAST_YEAR, cursor.getInt(colYear));
            builder.setExtras(extras);

            albumList.add(new MediaItem(builder.build(), MediaItem.FLAG_BROWSABLE));
        }

        cursor.close();
        return albumList;
    }

    boolean isInitialized() {
        return mCurrentState == INITIALIZED;
    }

    Collection<MediaMetadataCompat> getAllMusic() {
        if (mCurrentState != INITIALIZED) {
            return Collections.emptyList();
        }
        return mMusicListById.values();
    }

    interface MusicReadyCallback {
        void onMusicCatalogReady(boolean success);
    }

    interface Callback<T> {
        void onReady(T value);
    }
}
