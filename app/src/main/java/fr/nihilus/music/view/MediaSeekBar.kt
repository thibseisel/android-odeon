package fr.nihilus.music.view

import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.widget.AppCompatSeekBar
import android.util.AttributeSet

/**
 * SeekBar that can be used with a [MediaSessionCompat] to track and seek in playing
 * media.
 *
 * @constructor
 * @param context The context of the activity that holds this view
 * @param attrs Layout param attributes
 * @param defStyleAttr
 */
class MediaSeekBar
@JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : AppCompatSeekBar(context, attrs, defStyleAttr) {

    /**
     * Updates the media metadata for this SeekBar. It will be used to define the maximum progress
     * of this SeekBar based on the track's duration.
     *
     * It the metadata is `null`, the maximum progress will be reset to zero.
     *
     * @param metadata The metadata of the currently playing track
     */
    fun setMetadata(metadata: MediaMetadataCompat?) {
        val max = metadata?.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) ?: 0L
        setMax(max.toInt())
    }

    /**
     * Updates the playback state for this SeekBar.
     */
    fun setPlaybackState(state: PlaybackStateCompat?) {
        val progress = state?.position?.toInt() ?: 0
        setProgress(progress)
    }
}
