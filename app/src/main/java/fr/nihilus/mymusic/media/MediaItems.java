package fr.nihilus.mymusic.media;

import android.support.v4.media.MediaBrowserCompat;

/**
 * A helper class grouping {@link MediaBrowserCompat.MediaItem}-related constants.
 */
public final class MediaItems {
    /**
     * A non human-readable key used for sorting.
     * <p>Type: String</p>
     */
    public static final String EXTRA_TITLE_KEY = "title_key";
    /**
     * The listening time represented by this item, may it be a track, an album or a playlist.
     * <p>Type: long</p>
     */
    public static final String EXTRA_DURATION = "duration";
}
