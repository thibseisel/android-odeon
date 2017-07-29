package fr.nihilus.mymusic.service;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.provider.MediaStore;
import android.provider.MediaStore.Audio.AlbumColumns;
import android.provider.MediaStore.Audio.ArtistColumns;
import android.provider.MediaStore.Audio.Artists;
import android.provider.MediaStore.Audio.AudioColumns;
import android.provider.MediaStore.Audio.Media;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import javax.inject.Inject;
import javax.inject.Named;

import fr.nihilus.mymusic.di.MusicServiceScope;
import fr.nihilus.mymusic.provider.Playlists;
import fr.nihilus.mymusic.settings.Prefs;
import fr.nihilus.mymusic.utils.MediaID;
import fr.nihilus.mymusic.utils.PermissionUtil;

import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ARTIST;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DISC_NUMBER;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_DURATION;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_URI;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TITLE;
import static android.support.v4.media.MediaMetadataCompat.METADATA_KEY_TRACK_NUMBER;

@MusicServiceScope
class MusicProvider implements AudioColumns {

    static final String METADATA_TITLE_KEY = "title_key";
    static final String METADATA_SOURCE = "file_path";
    static final Uri ALBUM_ART_URI = Uri.parse("content://media/external/audio/albumart");
    private static final String TAG = "MusicProvider";
    private static final String[] ALBUM_PROJECTION = {BaseColumns._ID, AlbumColumns.ALBUM,
            AlbumColumns.ALBUM_KEY, AlbumColumns.ARTIST,
            AlbumColumns.LAST_YEAR, AlbumColumns.NUMBER_OF_SONGS};
    private static final int NON_INITIALIZED = 0, INITIALIZING = 1, INITIALIZED = 2;
    private static final String[] MEDIA_PROJECTION = {_ID, TITLE, ALBUM, ARTIST, DURATION, TRACK,
            TITLE_KEY, ALBUM_KEY, ALBUM_ID, ARTIST_ID, DATA};
    private static final String[] ARTIST_PROJECTION = {BaseColumns._ID, ArtistColumns.ARTIST,
            ArtistColumns.NUMBER_OF_ALBUMS, ArtistColumns.NUMBER_OF_TRACKS};
    private final LongSparseArray<MediaMetadataCompat> mMusicById;
    private final MetadataStore mMusicByAlbum;
    private final List<MediaMetadataCompat> mMusicAlpha;
    private final MetadataStore mMusicByArtist;
    private final LongSparseArray<Uri> mArtistAlbumCache;
    private final MetadataStore mMusicByPlaylist;
    private volatile int mCurrentState = NON_INITIALIZED;
    private final Context mContext;

    @Inject
    MusicProvider(@NonNull @Named("Application") Context context) {
        mContext = context;
        mMusicById = new LongSparseArray<>();
        mMusicAlpha = new ArrayList<>();
        mMusicByAlbum = new MetadataStore(MetadataStore.SORT_TRACKNO);
        mMusicByArtist = new MetadataStore(MetadataStore.SORT_TITLE);
        mMusicByPlaylist = new MetadataStore(MetadataStore.SORT_TITLE);
        mArtistAlbumCache = new LongSparseArray<>();
    }

    /**
     * Get the metadata of a music from the music library.
     *
     * @param musicId id of the searched music
     * @return the associated music metadata, or null if no music is associated with this id
     */
    @Nullable
    MediaMetadataCompat getMusic(String musicId) {
        long id = Long.parseLong(musicId);
        return mMusicById.get(id, null);
    }

    boolean isNotInitialized() {
        return mCurrentState != INITIALIZED;
    }

    /**
     * Retrieve all songs metadata from the storage.
     * You must call this method before querying song-related MediaItems.
     */
    @SuppressWarnings("WrongConstant")
    void loadMetadata() {
        if (isNotInitialized()) {
            mCurrentState = INITIALIZING;
        }

        if (!PermissionUtil.hasExternalStoragePermission(mContext)) {
            Log.w(TAG, "loadMetadata: application doesn't have external storage permissions.");
            mCurrentState = NON_INITIALIZED;
            return;
        }

        final Cursor cursor = mContext.getContentResolver().query(Media.EXTERNAL_CONTENT_URI,
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
        final int colAlbumId = cursor.getColumnIndexOrThrow(ALBUM_ID);
        final int colArtistId = cursor.getColumnIndexOrThrow(ARTIST_ID);
        final int colFilePath = cursor.getColumnIndexOrThrow(DATA);

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
                    .putString(METADATA_TITLE_KEY, cursor.getString(colTitleKey))
                    .putString(METADATA_SOURCE, cursor.getString(colFilePath))
                    .putString(METADATA_KEY_MEDIA_URI, ContentUris.withAppendedId(
                            Media.EXTERNAL_CONTENT_URI, musicId).toString());

            final MediaMetadataCompat metadata = builder.build();
            mMusicById.put(musicId, metadata);
            mMusicAlpha.add(metadata);
            mMusicByAlbum.put(cursor.getLong(colAlbumId), metadata);
            mMusicByArtist.put(cursor.getLong(colArtistId), metadata);
        }
        cursor.close();

        loadPlaylists();

        mCurrentState = INITIALIZED;
    }

    private void loadPlaylists() {

        if (!PermissionUtil.hasExternalStoragePermission(mContext)) {
            Log.w(TAG, "loadMetadata: application doesn't have external storage permissions.");
            mCurrentState = NON_INITIALIZED;
            return;
        }

        final Cursor cursor = mContext.getContentResolver().query(Playlists.Members.CONTENT_URI_ALL,
                null, null, null, null);

        if (cursor == null) {
            Log.w(TAG, "loadPlaylists: query failed. Aborting.");
            return;
        }

        final int colPlaylist = cursor.getColumnIndexOrThrow(Playlists.Members.PLAYLIST);
        final int colMusicId = cursor.getColumnIndexOrThrow(Playlists.Members.MUSIC);

        while (cursor.moveToNext()) {
            long playlistId = cursor.getLong(colPlaylist);
            long musicId = cursor.getLong(colMusicId);
            MediaMetadataCompat meta = mMusicById.get(musicId, null);
            if (meta == null) {
                throw new IllegalStateException("Music library must be loaded to retrive playlists.");
            }
            mMusicByPlaylist.put(playlistId, meta);
        }
        cursor.close();
    }

    List<MediaMetadataCompat> getAlbumTracks(String albumId) {
        if (isNotInitialized()) {
            Log.w(TAG, "getAlbumTracks: music library is not initialized yet.");
            return Collections.emptyList();
        }

        List<MediaMetadataCompat> tracks = new ArrayList<>();
        Set<MediaMetadataCompat> set = mMusicByAlbum.get(Long.parseLong(albumId));
        if (set != null) {
            tracks.addAll(set);
        }
        return tracks;
    }

    List<MediaMetadataCompat> getArtistTracks(String artistId) {
        if (isNotInitialized()) {
            Log.w(TAG, "getArtistTracks: music library is not initialized yet.");
            return Collections.emptyList();
        }

        List<MediaMetadataCompat> tracks = new ArrayList<>();
        Set<MediaMetadataCompat> set = mMusicByArtist.get(Long.parseLong(artistId));
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
        if (isNotInitialized()) {
            Log.w(TAG, "getSongs: music library is not initialized yet.");
            return Collections.emptyList();
        }
        return MediaItemHelper.getSongs(mMusicAlpha);
    }

    /**
     * Retrieve all albums as a list of items suitable for display.
     *
     * @return list of all albums, alphabetically sorted
     */
    List<MediaItem> getAlbumItems() {
        if (!PermissionUtil.hasExternalStoragePermission(mContext)) {
            Log.w(TAG, "loadMetadata: application doesn't have external storage permissions.");
            return Collections.emptyList();
        }

        Cursor cursor = mContext.getContentResolver()
                .query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, ALBUM_PROJECTION,
                        null, null, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);

        if (cursor == null) {
            return Collections.emptyList();
        }

        List<MediaItem> result = MediaItemHelper.getAlbums(cursor);
        cursor.close();
        return result;
    }

    /**
     * Retrieve all tracks released in a particular album as a list of items suitable for display.
     *
     * @param albumMediaId mediaId the album from which get the list of song
     * @return list of songs released in this album
     */
    List<MediaItem> getAlbumTracksItems(String albumMediaId) {
        if (isNotInitialized()) {
            Log.w(TAG, "getAlbumTracksItems: music library is not initialized yet.");
            return Collections.emptyList();
        }

        long albumId = Long.parseLong(albumMediaId.split("/")[1]);
        SortedSet<MediaMetadataCompat> tracks = mMusicByAlbum.get(albumId);

        if (tracks == null) {
            Log.w(TAG, "getAlbumTracksItems: no album with mediaId=" + albumMediaId);
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
        return result;
    }

    List<MediaMetadataCompat> getAllMusic() {
        return mMusicAlpha;
    }

    List<MediaItem> getArtistItems() {
        if (!PermissionUtil.hasExternalStoragePermission(mContext)) {
            Log.w(TAG, "loadMetadata: application doesn't have external storage permissions.");
            return Collections.emptyList();
        }

        Cursor cursor = mContext.getContentResolver()
                .query(Artists.EXTERNAL_CONTENT_URI, ARTIST_PROJECTION,
                        null, null, Artists.DEFAULT_SORT_ORDER);

        if (cursor == null) {
            Log.w(TAG, "getArtistItems: aborting. No artist found.");
            return Collections.emptyList();
        }

        List<MediaItem> result = new ArrayList<>();
        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder();

        final int colId = cursor.getColumnIndexOrThrow(BaseColumns._ID);
        final int name = cursor.getColumnIndexOrThrow(ArtistColumns.ARTIST);
        final int albumCount = cursor.getColumnIndexOrThrow(ArtistColumns.NUMBER_OF_ALBUMS);
        final int trackCount = cursor.getColumnIndexOrThrow(ArtistColumns.NUMBER_OF_TRACKS);

        while (cursor.moveToNext()) {
            final long artistId = cursor.getLong(colId);
            final String mediaId = MediaID.createMediaID(null, MediaID.ID_ARTISTS, String.valueOf(artistId));

            builder.setMediaId(mediaId)
                    .setTitle(cursor.getString(name));
            Bundle extras = new Bundle();
            extras.putInt(ArtistColumns.NUMBER_OF_ALBUMS, cursor.getInt(albumCount));
            extras.putInt(ArtistColumns.NUMBER_OF_TRACKS, cursor.getInt(trackCount));
            builder.setExtras(extras);

            Uri mostRecentAlbumArtUri = mArtistAlbumCache.get(artistId);
            if (mostRecentAlbumArtUri == null) {
                final Cursor c = mContext.getContentResolver()
                        .query(Artists.Albums.getContentUri("external", artistId),
                                new String[]{BaseColumns._ID}, null, null,
                                AudioColumns.YEAR + " DESC");
                if (c != null && c.moveToFirst()) {
                    long albumId = c.getLong(c.getColumnIndex(BaseColumns._ID));
                    mostRecentAlbumArtUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId);
                    mArtistAlbumCache.put(artistId, mostRecentAlbumArtUri);
                    c.close();
                }
            }
            builder.setIconUri(mostRecentAlbumArtUri);
            result.add(new MediaItem(builder.build(), MediaItem.FLAG_BROWSABLE));
        }

        cursor.close();
        return result;
    }

    List<MediaItem> getArtistChildren(@NonNull String artistId) {
        if (!PermissionUtil.hasExternalStoragePermission(mContext)) {
            Log.w(TAG, "loadMetadata: application doesn't have external storage permissions.");
            return Collections.emptyList();
        }

        long longId = Long.parseLong(artistId);
        Cursor cursor = mContext.getContentResolver()
                .query(Artists.Albums.getContentUri("external", longId), ALBUM_PROJECTION,
                        null, null, null);
        if (cursor == null) {
            Log.e(TAG, "getArtistChildren: artist albums query failed. Aborting.");
            return Collections.emptyList();
        }

        List<MediaItem> result = new ArrayList<>(cursor.getCount());
        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder();

        final int colId = cursor.getColumnIndexOrThrow(BaseColumns._ID);
        final int colTitle = cursor.getColumnIndexOrThrow(AlbumColumns.ALBUM);
        final int colKey = cursor.getColumnIndexOrThrow(AlbumColumns.ALBUM_KEY);
        final int colArtist = cursor.getColumnIndexOrThrow(AlbumColumns.ARTIST);
        final int colYear = cursor.getColumnIndexOrThrow(AlbumColumns.LAST_YEAR);
        final int colSongCount = cursor.getColumnIndexOrThrow(AlbumColumns.NUMBER_OF_SONGS);

        while (cursor.moveToNext()) {
            final long albumId = cursor.getLong(colId);
            final String mediaId = MediaID.createMediaID(null, MediaID.ID_ALBUMS, String.valueOf(albumId));
            final Uri artUri = ContentUris.withAppendedId(ALBUM_ART_URI, albumId);

            builder.setMediaId(mediaId)
                    .setTitle(cursor.getString(colTitle))
                    .setSubtitle(cursor.getString(colArtist))
                    .setIconUri(artUri);
            Bundle extras = new Bundle(3);
            extras.putString(AlbumColumns.ALBUM_KEY, cursor.getString(colKey));
            extras.putInt(AlbumColumns.NUMBER_OF_SONGS, cursor.getInt(colSongCount));
            extras.putInt(AlbumColumns.LAST_YEAR, cursor.getInt(colYear));
            builder.setExtras(extras);

            result.add(new MediaItem(builder.build(), MediaItem.FLAG_BROWSABLE | MediaItem.FLAG_PLAYABLE));
        }
        cursor.close();

        SortedSet<MediaMetadataCompat> trackSet = mMusicByArtist.get(longId);
        if (trackSet != null) {
            for (MediaMetadataCompat meta : trackSet) {
                long trackId = Long.parseLong(meta.getString(METADATA_KEY_MEDIA_ID));
                String mediaId = MediaID.createMediaID(String.valueOf(trackId), MediaID.ID_ARTISTS, artistId);
                long duration = meta.getLong(METADATA_KEY_DURATION);
                String albumArtUri = meta.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI);
                Bundle extras = new Bundle(2);
                extras.putLong(METADATA_KEY_DISC_NUMBER, meta.getLong(METADATA_KEY_DISC_NUMBER));
                extras.putLong(METADATA_KEY_TRACK_NUMBER, meta.getLong(METADATA_KEY_TRACK_NUMBER));

                builder.setMediaId(mediaId)
                        .setTitle(meta.getString(METADATA_KEY_TITLE))
                        .setSubtitle(DateUtils.formatElapsedTime(duration / 1000))
                        .setExtras(extras);
                if (albumArtUri != null) {
                    builder.setIconUri(Uri.parse(albumArtUri));
                }
                result.add(new MediaItem(builder.build(), MediaItem.FLAG_PLAYABLE));
            }
        }

        return result;
    }

    List<MediaItem> getPlaylistItems() {
        if (!PermissionUtil.hasExternalStoragePermission(mContext)) {
            Log.w(TAG, "loadMetadata: application doesn't have external storage permissions.");
            return Collections.emptyList();
        }

        Cursor cursor = mContext.getContentResolver().query(Playlists.CONTENT_URI, null, null, null, null);
        if (cursor == null) {
            Log.e(TAG, "getPlaylistItems: playlists query failed. Aborting.");
            return Collections.emptyList();
        }

        int colId = cursor.getColumnIndexOrThrow(Playlists.PLAYLIST_ID);
        int colName = cursor.getColumnIndexOrThrow(Playlists.NAME);

        List<MediaItem> result = new ArrayList<>(cursor.getCount());
        MediaDescriptionCompat.Builder builder = new MediaDescriptionCompat.Builder();

        while (cursor.moveToNext()) {
            final long playlistId = cursor.getLong(colId);
            final String mediaId = MediaID.createMediaID(null, MediaID.ID_PLAYLISTS, String.valueOf(playlistId));

            builder.setMediaId(mediaId).setTitle(cursor.getString(colName));
            result.add(new MediaItem(builder.build(), MediaItem.FLAG_BROWSABLE | MediaItem.FLAG_PLAYABLE));
        }

        cursor.close();
        return result;
    }

    List<MediaMetadataCompat> getPlaylistMembers(@NonNull String playlistId) {
        List<MediaMetadataCompat> result = Collections.emptyList();
        Set<MediaMetadataCompat> members = mMusicByPlaylist.get(Long.parseLong(playlistId));
        if (members != null) {
            result = new ArrayList<>(members);
        }
        return result;
    }

    List<MediaItem> getPlaylistMembersItems(@NonNull String playlistId) {
        if (!PermissionUtil.hasExternalStoragePermission(mContext)) {
            Log.w(TAG, "loadMetadata: application doesn't have external storage permissions.");
            return Collections.emptyList();
        }

        Cursor cursor = mContext.getContentResolver()
                .query(Playlists.Members.getContentUri(Long.parseLong(playlistId)),
                        null, null, null, null);

        if (cursor == null) {
            Log.e(TAG, "getPlaylistItems: playlists query failed. Aborting.");
            return Collections.emptyList();
        }

        int colMusic = cursor.getColumnIndexOrThrow(Playlists.Members.MUSIC);

        List<MediaMetadataCompat> meta = new ArrayList<>(cursor.getCount());

        while (cursor.moveToNext()) {
            meta.add(getMusic(cursor.getString(colMusic)));
        }
        cursor.close();
        return MediaItemHelper.getPlaylistTracks(meta, playlistId);
    }

    /**
     * @return a random song from the music library as a MediaItem.
     */
    MediaItem getSongOfTheDayItem() {
        if (isNotInitialized()) {
            Log.w(TAG, "getSongOfTheDayItem: music library not initialized yet.");
            return null;
        }

        if (mMusicById.size() == 0) {
            Log.i(TAG, "getSongOfTheDayItem: music library is empty.");
            return null;
        }

        MediaMetadataCompat meta;

        long lastUpdate = Prefs.getLastDailySongUpdate(mContext);
        if (DateUtils.isToday(lastUpdate)) {
            // Song of the Day has already been determined for today
            long songId = Prefs.getDailySongId(mContext);
            meta = mMusicById.get(songId);
        } else {
            // It's a new day, get Song of the Day randomly and save its ID
            Random rand = new Random();
            int index = rand.nextInt(mMusicById.size());
            long musicId = mMusicById.keyAt(index);
            meta = mMusicById.valueAt(index);
            Prefs.setDailySongId(mContext, musicId);
        }

        String mediaId = MediaID.createMediaID(meta.getString(METADATA_KEY_MEDIA_ID), MediaID.ID_MUSIC);
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

    /**
     * Call this method to notify the provider that the media associated with the ID has changed.
     *
     * @param changedUri uri of the media that has changed
     */
    void notifySongChanged(@NonNull Uri changedUri) {
        Cursor cursor = mContext.getContentResolver()
                .query(changedUri, MEDIA_PROJECTION, null, null, Media.DEFAULT_SORT_ORDER);
        if (cursor != null) {
            if (!cursor.moveToFirst()) {
                // Cursor empty, song has been DELETED
                long songId = ContentUris.parseId(changedUri);
                if (songId >= 0) {
                    mMusicById.delete(songId);
                }
            } else {
                // INSERTED or UPDATED
                final int colId = cursor.getColumnIndexOrThrow(_ID);
                final int colTitle = cursor.getColumnIndexOrThrow(TITLE);
                final int colAlbum = cursor.getColumnIndexOrThrow(ALBUM);
                final int colArtist = cursor.getColumnIndexOrThrow(ARTIST);
                final int colDuration = cursor.getColumnIndexOrThrow(DURATION);
                final int colTrackNo = cursor.getColumnIndexOrThrow(TRACK);
                final int colTitleKey = cursor.getColumnIndexOrThrow(TITLE_KEY);
                final int colAlbumId = cursor.getColumnIndexOrThrow(ALBUM_ID);
                final int colArtistId = cursor.getColumnIndexOrThrow(ARTIST_ID);

                MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
                do {
                    long musicId = cursor.getLong(colId);
                    Uri artUri = ContentUris.withAppendedId(ALBUM_ART_URI, cursor.getLong(colAlbumId));
                    long trackNo = cursor.getLong(colTrackNo);

                    //noinspection WrongConstant
                    builder.putString(METADATA_KEY_MEDIA_ID, String.valueOf(musicId))
                            .putString(METADATA_KEY_TITLE, cursor.getString(colTitle))
                            .putString(METADATA_KEY_ALBUM, cursor.getString(colAlbum))
                            .putString(METADATA_KEY_ARTIST, cursor.getString(colArtist))
                            .putLong(METADATA_KEY_DURATION, cursor.getLong(colDuration))
                            .putLong(METADATA_KEY_TRACK_NUMBER, trackNo % 100)
                            .putLong(METADATA_KEY_DISC_NUMBER, trackNo / 100)
                            .putString(METADATA_KEY_ALBUM_ART_URI, artUri.toString())
                            .putString(METADATA_TITLE_KEY, cursor.getString(colTitleKey))
                            .putString(METADATA_KEY_MEDIA_URI, ContentUris.withAppendedId(
                                    Media.EXTERNAL_CONTENT_URI, musicId).toString());

                    final MediaMetadataCompat metadata = builder.build();
                    mMusicById.put(musicId, metadata);
                    // TODO J'en ai oublié quelques uns
                } while (cursor.moveToNext());
            }
            cursor.close();
        }
    }
}
