package fr.nihilus.mymusic.media

import android.support.v4.media.MediaBrowserCompat

/**
 * A helper class grouping [MediaBrowserCompat.MediaItem]-related constants.
 */
object MediaItems {
    /**
     * A non human-readable key used for sorting generated from the title of a track.
     *
     * Type: String
     */
    const val EXTRA_TITLE_KEY = "title_key"
    /**
     * The listening time represented by this item, may it be a track, an album or a playlist.
     *
     * Type: long
     */
    const val EXTRA_DURATION = "duration"
    /**
     * A non human-readable key used for sorting generated from the title of an album.
     *
     * Type: String
     */
    const val EXTRA_ALBUM_KEY = "album_key"
    /**
     * The number of tracks that this browsable media item contains.
     *
     * Type: integer
     */
    const val EXTRA_NUMBER_OF_TRACKS = "number_of_tracks"
    /**
     * Year in which this media item has been released.
     *
     * Type: integer
     */
    const val EXTRA_YEAR = "last_year"
}
