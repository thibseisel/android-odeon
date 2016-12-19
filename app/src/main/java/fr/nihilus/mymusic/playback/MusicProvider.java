package fr.nihilus.mymusic.playback;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.Media;
import android.support.annotation.NonNull;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.util.LongSparseArray;
import android.text.format.DateUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.SortedSet;

import fr.nihilus.mymusic.utils.MediaID;
import fr.nihilus.mymusic.utils.MetadataStore;
import fr.nihilus.mymusic.utils.PermissionUtil;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISC_NUMBER;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER;

class MusicProvider implements MediaStore.Audio.AudioColumns {

    private static final String METADATA_TITLE_KEY = "title_key";

    private static final String TAG = "MusicProvider";
    private static final String[] ALBUM_PROJECTION = {BaseColumns._ID, AlbumColumns.ALBUM,
            AlbumColumns.ALBUM_KEY, AlbumColumns.ARTIST,
            AlbumColumns.LAST_YEAR, AlbumColumns.NUMBER_OF_SONGS};
    private static final int NON_INITIALIZED = 0, INITIALIZING = 1, INITIALIZED = 2;
    private static final Uri ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart");
    private static final String[] MEDIA_PROJECTION = {_ID, TITLE, ALBUM, ARTIST, DURATION, TRACK,
            TITLE_KEY, ALBUM_KEY, ALBUM_ID, ARTIST_ID};

    private volatile int mCurrentState = NON_INITIALIZED;

    private final LongSparseArray<MediaMetadataCompat> mMusicById;
    private final MetadataStore mMusicByAlbum;
    private final List<MediaMetadataCompat> mMusicAlpha;
    private final MetadataStore mMusicByArtist;

    MusicProvider() {
        mMusicById = new LongSparseArray<>();
        mMusicAlpha = new ArrayList<>();
        mMusicByAlbum = new MetadataStore();
        mMusicByArtist = new MetadataStore();
    }

    /**
     * Get the metadata of a music from the music library.
     *
     * @param musicId id of the searched music
     */
    MediaMetadataCompat getMusic(String musicId) {
        long id = Long.parseLong(musicId);
        return mMusicById.get(id, null);
    }

    boolean isInitialized() {
        return mCurrentState == INITIALIZED;
    }

    /**
     * Retrieve all songs metadata from the storage.
     * You must call this method before querying song-related MediaItems.
     */
    @SuppressWarnings("WrongConstant")
    void loadMetadata(Context context) {
        if (!isInitialized()) {
            mCurrentState = INITIALIZING;
        }

        if (!PermissionUtil.hasExternalStoragePermission(context)) {
            Log.w(TAG, "loadMetadata: application doesn't have external storage permissions.");
            mCurrentState = NON_INITIALIZED;
            return;
        }

        final Cursor cursor = context.getContentResolver().query(Media.EXTERNAL_CONTENT_URI,
                MEDIA_PROJECTION, IS_MUSIC + "=1", null, Media.DEFAULT_SORT_ORDER);

        if (cursor == null) {
            Log.w(TAG, "loadMetadata: no media found. Cursor is empty.");
            return;
        }

        final int colId = cursor.getColumnIndexOrThrow(_ID);
        final int colTitle = cursor.getColumnIndexOrThrow(TITLE);
        final int colAlbum = cursor.getColumnIndexOrThrow(ALBUM);
        final int colArtist = cursor.getColumnIndexOrThrow(ARTIST);
        final int colDuration = cursor.getColumnIndexOrThrow(DURATION);
        final int colTrackNo = cursor.getColumnIndexOrThrow(TRACK);
        final int colTitleKey = cursor.getColumnIndexOrThrow(TITLE_KEY);
        //final int colAlbumKey = cursor.getColumnIndexOrThrow(ALBUM_KEY);
        final int colAlbumId = cursor.getColumnIndexOrThrow(ALBUM_ID);
        final int colArtistId = cursor.getColumnIndexOrThrow(ARTIST_ID);

        mMusicById.clear();
        mMusicByAlbum.clear();
        mMusicByArtist.clear();

        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();

        while (cursor.moveToNext()) {
            long musicId = cursor.getLong(colId);
            Uri artUri = ContentUris.withAppendedId(ALBUM_ART_URI, cursor.getLong(colAlbumId));
            long trackNo = cursor.getLong(colTrackNo);

            builder.putString(METADATA_KEY_MEDIA_ID, String.valueOf(musicId))
                    .putString(METADATA_KEY_TITLE, cursor.getString(colTitle))
                    .putString(METADATA_KEY_ALBUM, cursor.getString(colAlbum))
                    .putString(METADATA_KEY_ARTIST, cursor.getString(colArtist))
                    .putLong(METADATA_KEY_DURATION, cursor.getLong(colDuration))
                    .putLong(METADATA_KEY_TRACK_NUMBER, trackNo % 100)
                    .putLong(METADATA_KEY_DISC_NUMBER, trackNo / 100)
                    .putString(METADATA_KEY_ALBUM_ART_URI, artUri.toString())
                    .putString(METADATA_TITLE_KEY, cursor.getString(colTitleKey));

            final MediaMetadataCompat metadata = builder.build();
            mMusicById.put(musicId, metadata);
            mMusicAlpha.add(metadata);
            mMusicByAlbum.put(cursor.getLong(colAlbumId), metadata);
            mMusicByArtist.put(cursor.getLong(colArtistId), metadata);
        }
        cursor.close();
        mCurrentState = INITIALIZED;
    }

    List<MediaMetadataCompat> getTracks(String albumMediaId) {
        if (!isInitialized()) {
            Log.w(TAG, "getTracks: music library is not initialized yet.");
            return Collections.emptyList();
        }

        String categoryValue = MediaID.extractBrowseCategoryValueFromMediaID(albumMediaId);
        List<MediaMetadataCompat> tracks = new ArrayList<>();
        Set<MediaMetadataCompat> set = mMusicByAlbum.get(Long.parseLong(categoryValue));
        if (set != null) {
            tracks.addAll(set);
        }
        return tracks;
    }

    /**
     * Retrieve the whole music library as a list of items suitable for display.
     *
     * @return list of all songs, alphabetically sorted
     */
    @SuppressWarnings("WrongConstant")
    List<MediaItem> getMusicItems() {
        if (!isInitialized()) {
            Log.w(TAG, "getMusicItems: music library is not initialized yet.");
            return Collections.emptyList();
        }

        List<MediaItem> result = new ArrayList<>();

        final MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder();
        for (MediaMetadataCompat meta : mMusicAlpha) {
            String musicId = meta.getString(METADATA_KEY_MEDIA_ID);
            String albumArtUri = meta.getString(METADATA_KEY_ALBUM_ART_URI);

            builder.setMediaId(MediaID.createMediaID(musicId, MediaID.ID_MUSIC, MediaID.ID_MUSIC))
                    .setTitle(meta.getString(METADATA_KEY_TITLE))
                    .setSubtitle(meta.getString(METADATA_KEY_ARTIST));
            if (albumArtUri != null) {
                builder.setIconUri(Uri.parse(albumArtUri));
            }

            result.add(new MediaItem(builder.build(), MediaItem.FLAG_PLAYABLE));
        }
        return result;
    }

    /**
     * Retrieve all albums as a list of items suitable for display.
     *
     * @return list of all albums, alphabetically sorted
     */
    List<MediaItem> getAlbumItems(@NonNull Context context) {
        if (!PermissionUtil.hasExternalStoragePermission(context)) {
            Log.w(TAG, "loadMetadata: application doesn't have external storage permissions.");
            return Collections.emptyList();
        }

        Cursor cursor = context.getContentResolver()
                .query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ALBUM_PROJECTION,
                        null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);

        if (cursor == null) {
            return Collections.emptyList();
        }

        List<MediaItem> result = new ArrayList<>();
        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder();

        final int colId = cursor.getColumnIndexOrThrow(BaseColumns._ID);
        final int colTitle = cursor.getColumnIndexOrThrow(AlbumColumns.ALBUM);
        final int colKey = cursor.getColumnIndexOrThrow(AlbumColumns.ALBUM_KEY);
        final int colArtist = cursor.getColumnIndexOrThrow(AlbumColumns.ARTIST);
        final int colYear = cursor.getColumnIndexOrThrow(AlbumColumns.LAST_YEAR);
        final int colSongCount = cursor.getColumnIndexOrThrow(AlbumColumns.NUMBER_OF_SONGS);

        while (cursor.moveToNext()) {
            final long albumId = cursor.getLong(colId);
            final String mediaId = MediaID.ID_ALBUMS + "/" + String.valueOf(albumId);
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

            result.add(new MediaItem(builder.build(), MediaItem.FLAG_BROWSABLE | MediaItem.FLAG_PLAYABLE));
        }

        cursor.close();
        return result;
    }

    /**
     * Retrieve all tracks released in a particular album as a list of items suitable for display.
     *
     * @param albumMediaId mediaId the album from which get the list of song
     * @return list of songs released in this album
     */
    List<MediaItem> getTracksItems(String albumMediaId) {
        if (!isInitialized()) {
            Log.w(TAG, "getTracksItems: music library is not initialized yet.");
            return Collections.emptyList();
        }

        long albumId = Long.parseLong(albumMediaId.split("/")[1]);
        SortedSet<MediaMetadataCompat> tracks = mMusicByAlbum.get(albumId);

        if (tracks == null) {
            Log.w(TAG, "getTracksItems: no album with mediaId=" + albumMediaId);
            return Collections.emptyList();
        }

        List<MediaItem> result = new ArrayList<>();
        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder();

        for (MediaMetadataCompat meta : tracks) {
            long trackId = Long.parseLong(meta.getString(METADATA_KEY_MEDIA_ID));
            String mediaId = albumMediaId + "|" + trackId;
            long duration = meta.getLong(METADATA_KEY_DURATION);
            Bundle extras = new Bundle();
            extras.putLong(METADATA_KEY_DISC_NUMBER, meta.getLong(METADATA_KEY_DISC_NUMBER));
            extras.putLong(METADATA_KEY_TRACK_NUMBER, meta.getLong(METADATA_KEY_TRACK_NUMBER));

            builder.setMediaId(mediaId)
                    .setTitle(meta.getString(METADATA_KEY_TITLE))
                    .setSubtitle(DateUtils.formatElapsedTime(duration / 1000))
                    .setExtras(extras);
            result.add(new MediaItem(builder.build(), MediaItem.FLAG_PLAYABLE));
        }
        Log.d(TAG, "getTracksItems: loaded " + result.size() + " items");
        return result;
    }

    List<MediaMetadataCompat> getAllMusic() {
        return mMusicAlpha;
    }

    List<MediaItem> getArtistItems() {
        throw new UnsupportedOperationException("Not yet implemented.");
    }

    /**
     * @return a random song from the music library as a MediaItem.
     */
    MediaItem getRandomMusicItem() {
        if (!isInitialized()) {
            Log.w(TAG, "getRandomMusicItem: music library not initialized yet.");
            return null;
        }

        Random rand = new Random();
        int index = rand.nextInt(mMusicById.size());
        MediaMetadataCompat meta = mMusicById.valueAt(index);

        String mediaId = MediaID.ID_DAILY + "|" + meta.getString(METADATA_KEY_MEDIA_ID);
        String albumArtUri = meta.getString(METADATA_KEY_ALBUM_ART_URI);

        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder()
                .setMediaId(mediaId)
                .setTitle(meta.getString(METADATA_KEY_TITLE))
                .setSubtitle(meta.getString(METADATA_KEY_ARTIST));
        if (albumArtUri != null) {
            builder.setIconUri(Uri.parse(albumArtUri));
        }

        return new MediaItem(builder.build(), MediaItem.FLAG_PLAYABLE);
    }
}
