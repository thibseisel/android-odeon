package fr.nihilus.mymusic.provider;

import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;

public interface Playlists {

    String AUTHORITY = "fr.nihilus.mymusic.provider";
    Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
    String DEFAULT_SORT_ORDER = Playlists.DATE_CREATED;
    String TABLE_NAME = "playlist";
    public Uri CONTENT_URI = AUTHORITY_URI.buildUpon().appendPath(TABLE_NAME).build();

    String CONTENT_TYPE = "vnd.android.cursor.dir/" + CONTENT_URI  + "/" + TABLE_NAME;
    String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/" + CONTENT_URI + "/" + TABLE_NAME;

    /**
     * Unique id representing this playlist.
     * <p>Type: INTEGER (long)</p>
     */
    String PLAYLIST_ID = BaseColumns._ID;
    /**
     * Name given to this playlist by the user.
     * <p>Type: TEXT</p>
     */
    String NAME = "name";

    /**
     * URI of the album art chosen by the user to represent this playlist.
     * <p>Type: TEXT</p>
     */
    String ART = "album_art";
    /**
     * Date at which this playlist has been created.
     * <p>Type: INTEGER (long)</p>
     */
    String DATE_CREATED = "date_created";

    interface Tracks {
        String TABLE_NAME = "tracks";
        Uri CONTENT_URI = AUTHORITY_URI.buildUpon().appendPath(TABLE_NAME).build();

        String DEFAULT_SORT_ORDER = Tracks.INDEX;
        String CONTENT_TYPE = "vnd.android.cursor.dir/" + CONTENT_URI  + "/" + TABLE_NAME;
        String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/" + CONTENT_URI + "/" + TABLE_NAME;

        /**
         * Unique identifier of the relation between a playlist and a music.
         * <p>Type: INTEGER (long)</p>
         */
        String TRACK_ID = BaseColumns._ID;
        /**
         * Unique id representing the song that is part of this playlist.
         * This id must match the id of the song from {@link MediaStore.Audio.Media#EXTERNAL_CONTENT_URI}.
         * <p>Type: INTEGER (long)</p>
         */
        String MUSIC = "song_id";
        /**
         * Unique identifier of the playlist that contains this music.
         * <p>Type: INTEGER (long)</p>
         */
        String PLAYLIST = "playlist_id";
        /**
         * Position of this music within the playlist.
         * <p>Type: INTEGER</p>
         */
        String INDEX = "index";
    }
}
