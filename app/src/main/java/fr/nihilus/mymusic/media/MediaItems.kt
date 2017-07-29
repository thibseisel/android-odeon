package fr.nihilus.mymusic.media

import android.support.v4.media.MediaBrowserCompat

/**
 * A helper class grouping [MediaBrowserCompat.MediaItem]-related constants.
 */
object MediaItems {
    /**
     * A non human-readable key used for sorting.
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
}
