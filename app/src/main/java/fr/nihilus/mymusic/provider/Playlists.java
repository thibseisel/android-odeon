package fr.nihilus.mymusic.provider;

import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore;

public final class Playlists {

    private Playlists() {
        throw new AssertionError("Playlists class is not instantiable.");
    }

    static final String AUTHORITY = "fr.nihilus.mymusic.provider";
    static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
    static final String DEFAULT_SORT_ORDER = Playlists.DATE_CREATED;
    static final String TABLE = "playlists";
    public static final Uri CONTENT_URI = AUTHORITY_URI.buildUpon().appendPath(TABLE).build();

    static final String CONTENT_TYPE = "vnd.android.cursor.dir/" + CONTENT_URI  + "/" + TABLE;
    static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/" + CONTENT_URI + "/" + TABLE;

    /**
     * Unique id representing this playlist.
     * <p>Type: INTEGER (long)</p>
     */
    public static final String PLAYLIST_ID = BaseColumns._ID;
    /**
     * Name given to this playlist by the user.
     * <p>Type: TEXT</p>
     */
    public static final String NAME = "name";
    /**
     * URI of the album art chosen by the user to represent this playlist.
     * <p>Type: TEXT</p>
     */
    static final String ART = "album_art";
    /**
     * Date at which this playlist has been created.
     * <p>Type: INTEGER (long)</p>
     */
    static final String DATE_CREATED = "date_created";

    public static final class Tracks {

        private Tracks() {
            throw new AssertionError("Playlists.Tracks class is not instantiable.");
        }

        static final String TABLE = "tracks";

        static final String DEFAULT_SORT_ORDER = Tracks.POSITION;
        static final String CONTENT_TYPE = "vnd.android.cursor.dir/" + CONTENT_URI  + "/" + TABLE;
        static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/" + CONTENT_URI + "/" + TABLE;

        public static Uri getContentUri(long playlistId) {
            return Playlists.CONTENT_URI.buildUpon()
                    .appendPath(String.valueOf(playlistId))
                    .appendPath(TABLE)
                    .build();
        }

        /**
         * Unique identifier of the relation between a playlist and a music.
         * <p>Type: INTEGER (long)</p>
         * <p>Generated automatically when created.</p>
         */
        public static final String TRACK_ID = BaseColumns._ID;
        /**
         * Unique id representing the song that is part of this playlist.
         * This id must match the id of the song from {@link MediaStore.Audio.Media#EXTERNAL_CONTENT_URI}.
         * <p>Type: INTEGER (long)</p>
         * <p>Mandatory when inserting ou updating.</p>
         */
        public static final String MUSIC = "song_id";
        /**
         * Unique identifier of the playlist that contains this music.
         * <p>Type: INTEGER (long)</p>
         */
        static final String PLAYLIST = "playlist_id";
        /**
         * Position of this music within the playlist.
         * <p>Type: INTEGER</p>
         * Mandatory when inserting or updating.
         */
        public static final String POSITION = "position";
    }
}
