package fr.nihilus.music.view

import android.animation.ValueAnimator
import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.widget.AppCompatSeekBar
import android.util.AttributeSet
import android.view.animation.LinearInterpolator
import android.widget.SeekBar

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
) : AppCompatSeekBar(context, attrs, defStyleAttr), ValueAnimator.AnimatorUpdateListener {

    private val mOnSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {}
        override fun onStartTrackingTouch(seekBar: SeekBar) {
            mIsTracking = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            mSeekListener?.onSeek(this@MediaSeekBar, progress)
            mIsTracking = false
        }
    }

    private var mSeekListener: OnSeekListener? = null
    private var mIsTracking = false

    private var mProgressAnimator: ValueAnimator? = null

    init {
        super.setOnSeekBarChangeListener(mOnSeekBarChangeListener)
    }

    /**
     * Sets a listener to receive notifications of changes to the SeekBar's progress level.
     *
     * This operation is prohibited and will throw an [UnsupportedOperationException].
     * Use [setOnSeekListener] to listen for SeekBar progress changes instead.
     */
    override fun setOnSeekBarChangeListener(l: SeekBar.OnSeekBarChangeListener) {
        // Prohibit adding seek listeners to this subclass.
        throw UnsupportedOperationException()
    }

    /**
     * Sets a listener to receive notifications of changes to this SeekBar's progress level.
     *
     * @param listener An object defining the callback function.
     * You can reset the listener by passing `null`.
     */
    fun setOnSeekListener(listener: OnSeekListener?) {
        mSeekListener = listener
    }

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
        // If there's an ongoing animation, stop it now.
        if (mProgressAnimator != null) {
            mProgressAnimator!!.cancel()
            mProgressAnimator = null
        }

        val progress = state?.position?.toInt() ?: 0
        setProgress(progress)

        // If the media is playing then the seekbar should follow it, and the easiest
        // way to do that is to create a ValueAnimator to update it so the bar reaches
        // the end of the media the same time as playback gets there (or close enough).
        if (state != null && state.state == PlaybackStateCompat.STATE_PLAYING) {
            val timeToEnd = ((max - progress) / state.playbackSpeed).toInt()

            mProgressAnimator = ValueAnimator.ofInt(progress, max).apply {
                duration = timeToEnd.toLong()
                interpolator = LinearInterpolator()
                addUpdateListener(this@MediaSeekBar)
                start()
            }
        }
    }

    override fun onAnimationUpdate(valueAnimator: ValueAnimator) {
        // If the user is changing the slider, cancel the animation.
        if (mIsTracking) {
            valueAnimator.cancel()
            return
        }

        progress = valueAnimator.animatedValue as Int
    }

    /**
     * Listens for progress changes initiated by the user.
     */
    interface OnSeekListener {

        /**
         * Called when the progress of a listened MediaSeekBar has been changed by the user.
         * @param view The SeekBar whose progress has been changed
         * @param newPosition The new progress of the SeekBar in milliseconds
         */
        fun onSeek(view: MediaSeekBar, newPosition: Int)
    }
}
