package fr.nihilus.mymusic.provider;

import android.net.Uri;
import android.provider.BaseColumns;
import android.provider.MediaStore.Audio;

@Deprecated
interface Stats extends BaseColumns {

    String AUTHORITY = "fr.nihilus.mymusic.provider";
    Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
    String DEFAULT_SORT_ORDER = Stats.MUSIC_ID;
    String TABLE = "stats";
    Uri CONTENT_URI = AUTHORITY_URI.buildUpon().appendPath(TABLE).build();

    String CONTENT_TYPE = "vnd.android.cursor.dir/" + CONTENT_URI  + "/" + TABLE;
    String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/" + CONTENT_URI + "/" + TABLE;

    /**
     * Unique id representing the song associated with those stats.
     * This id must match the id of the song from {@link Audio.Media#EXTERNAL_CONTENT_URI}.
     * <p>Type: INTEGER (long)</p>
     */
    String MUSIC_ID = BaseColumns._ID;
    /**
     * Number of times this song has been played until the end.
     * <p>Type: INTEGER</p>
     */
    String READ_COUNT = "read_count";
    /**
     * Number of times this song has been skipped.
     * <p>Type: INTEGER</p>
     */
    String SKIP_COUNT = "skip_count";
    /**
     * Estimation of the tempo of this song.
     * <p>Type: INTEGER</p>
     */
    String TEMPO = "tempo";
    /**
     * Mean sound energy of this song.
     * <p>Type: INTEGER</p>
     */
    String ENERGY = "mean_energy";
}
