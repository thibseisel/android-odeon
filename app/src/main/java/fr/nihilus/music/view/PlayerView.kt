/*
 * Copyright 2017 Thibault Seisel
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.nihilus.music.view

import android.content.Context
import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import android.support.constraint.ConstraintLayout
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.view.ViewCompat
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import android.view.View
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.utils.dipToPixels
import kotlinx.android.synthetic.main.view_player.view.*
import kotlinx.android.synthetic.main.view_player_top.view.*

class PlayerView
@JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private val glideRequest: RequestBuilder<Bitmap>

    private lateinit var autoUpdater: ProgressAutoUpdater

    private var isExpanded = false
    private var repeatMode = PlaybackStateCompat.REPEAT_MODE_NONE

    private var lastPlaybackState: PlaybackStateCompat? = null
    private var metadata: MediaMetadataCompat? = null

    private var listener: EventListener? = null

    init {
        View.inflate(context, R.layout.view_player, this)

        // Make this view appear above AppbarLayout
        ViewCompat.setElevation(this, resources.getDimension(R.dimen.playerview_elevation))
        // Prevent from dispatching touches to views behind
        isClickable = true

        val defaultIcon = AppCompatResources.getDrawable(context, R.drawable.ic_audiotrack_24dp)
        glideRequest = GlideApp.with(context).asBitmap()
                .centerCrop()
                .fallback(defaultIcon)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        // Configure auto-update for SeekBar update and associated TextViews
        autoUpdater = ProgressAutoUpdater(seekBar, seekPosition, seekDuration) { position ->
            listener?.onSeek(position)
        }

        seekBar.setOnSeekBarChangeListener(autoUpdater)

        // Change color when shuffle mode and repeat mode buttons are activated
        val activationStateList = AppCompatResources.getColorStateList(context,
                R.color.activation_state_list)
        with(shuffleButton) {
            val shuffleDrawable = DrawableCompat.wrap(this.drawable)
            DrawableCompat.setTintList(shuffleDrawable, activationStateList)
            setImageDrawable(shuffleDrawable)
        }

        with(repeatButton) {
            val repeatDrawable = DrawableCompat.wrap(this.drawable)
            DrawableCompat.setTintList(repeatDrawable, activationStateList)
            setImageDrawable(repeatDrawable)
        }

        // Associate a common click listener for each button
        val clickListener = WidgetClickListener()

        // Buttons in top bar
        playPauseButton.setOnClickListener(clickListener)

        // Buttons that are only present in landscape mode
        miniPrevButton?.setOnClickListener(clickListener)
        miniNextButton?.setOnClickListener(clickListener)

        // Playback control buttons at bottom
        repeatButton.setOnClickListener(clickListener)
        skipPrevButton.setOnClickListener(clickListener)
        masterPlayPause.setOnClickListener(clickListener)
        skipNextButton.setOnClickListener(clickListener)
        shuffleButton.setOnClickListener(clickListener)

    }

    override fun dispatchSetPressed(pressed: Boolean) {
        // Do not dispatch pressed event to View children
    }

    /**
     * Expand or collapse the PlayerView with an animation.
     * When expanded, it is drawn above the main content view.
     * When collapsed, only the top is visible.
     * If the playerView is not a direct child of CoordinatorLayout, this method will do nothing.
     *
     * @param expanded true to expand the PlayerView, false to collapse
     */
    fun setExpanded(expanded: Boolean) {
        if (isExpanded != expanded) {
            if (expanded)
                onOpen()
            else
                onClose()
            isExpanded = expanded
        }
    }

    /**
     * Updates the track's metadata currently represented by this PlayerView.
     * @param metadata the currently focused track metadata.
     */
    fun updateMetadata(metadata: MediaMetadataCompat?) {
        this.metadata = metadata

        if (metadata != null) {
            val media = metadata.description
            titleView.text = media.title
            subtitleView.text = media.subtitle

            val bitmap = media.iconBitmap
            if (bitmap != null) {
                iconView.setImageBitmap(media.iconBitmap)
            } else {
                iconView.setImageResource(R.drawable.ic_audiotrack_24dp)
            }

            glideRequest.load(media.iconUri).into(albumArtView)
            autoUpdater.setMetadata(metadata)
        }
    }

    /**
     * Updates the playback state currently represented by this PlayerView.
     * Playback state describes what actions are available.
     *
     * @param newState The last playback state.
     */
    fun updatePlaybackState(newState: PlaybackStateCompat?) {
        lastPlaybackState = newState

        if (newState != null) {
            toggleControls(newState.actions)
            autoUpdater.setPlaybackState(newState)

            val isPlaying = newState.state == PlaybackStateCompat.STATE_PLAYING
            playPauseButton.isPlaying = isPlaying
            masterPlayPause.isPlaying = isPlaying

        } else {
            // Reinitialize composing views to display nothing
            iconView.setImageDrawable(null)
            albumArtView.setImageDrawable(null)
            titleView.text = null
            subtitleView.text = null

            with(seekBar) {
                progress = 0
                max = 0
            }
        }
    }

    /**
     * Enables or disables actions depending on parameters provided by the current playback state.
     * If an action is disabled, its associated view is also disabled and does not react to clicks.
     *
     * @param actions A set of flags describing what actions are available on this media session.
     */
    private fun toggleControls(actions: Long) {
        playPauseButton.isEnabled = actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L

        miniPrevButton?.isEnabled = actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L
        miniNextButton?.isEnabled = actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L

        repeatButton.isEnabled = actions and PlaybackStateCompat.ACTION_SET_REPEAT_MODE != 0L
        skipPrevButton.isEnabled = actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L
        masterPlayPause.isEnabled = actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L
        skipNextButton.isEnabled = actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L
        shuffleButton.isEnabled = actions and PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE != 0L
    }

    private fun onOpen() {
        val eightDps = dipToPixels(context, 8f)
        iconView.setPadding(eightDps, eightDps, eightDps, eightDps)

        playPauseButton.visibility = View.GONE
        miniPrevButton?.visibility = View.GONE
        miniNextButton?.visibility = View.GONE
    }

    private fun onClose() {
        iconView.setPadding(0, 0, 0, 0)

        playPauseButton.visibility = View.VISIBLE
        miniPrevButton?.visibility = View.VISIBLE
        miniNextButton?.visibility = View.VISIBLE
    }

    /**
     * Updates display of the shuffle mode of this PlayerView.
     * For requests to change the shuffle mode to be accurate, this value should always
     * be in sync with the media session's.
     *
     * @param mode The current shuffle mode for this media session.
     */
    fun setRepeatMode(@PlaybackStateCompat.RepeatMode mode: Int) {
        with(repeatButton) {
            setImageLevel(mode)
            isActivated = mode != PlaybackStateCompat.REPEAT_MODE_NONE
        }
    }

    /**
     * Updates display of the repeat mode of this PlayerView.
     * For requests to change the repeat mode to be accurate, this value should always
     * be in sync with the media session's.
     *
     * @param mode The current repeat mode for this media session.
     */
    fun setShuffleMode(@PlaybackStateCompat.ShuffleMode mode: Int) {
        shuffleButton.isActivated = mode != PlaybackStateCompat.SHUFFLE_MODE_NONE
    }

    /**
     * Listens for events triggered by interactions with this PlayerView.
     */
    fun setEventListener(listener: EventListener) {
        this.listener = listener
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        val savedState = SavedState(superState)
        savedState.lastPlaybackState = lastPlaybackState
        savedState.metadata = metadata
        savedState.repeatMode = repeatMode
        savedState.expanded = isExpanded
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        if (state !is SavedState) {
            super.onRestoreInstanceState(state)
            return
        }

        super.onRestoreInstanceState(state.superState)

        lastPlaybackState = state.lastPlaybackState
        metadata = state.metadata
        repeatMode = state.repeatMode
        isExpanded = state.expanded
    }

    /**
     * Listens for events triggered by user interactions with the PlayerView.
     */
    interface EventListener {

        /**
         * Called to handle a request to start or resume playback of the currently selected track.
         */
        fun onActionPlay()

        /**
         * Called to handle a request to pause playback of the currently selected track.
         */
        fun onActionPause()

        /**
         * Called when user moved the progress cursor to a new position.
         * @param position The new position of the progress cursor
         */
        fun onSeek(position: Long)

        /**
         * Called to handle a request to move to the previous track in playlist.
         */
        fun onSkipToPrevious()

        /**
         * Called to handle a request to move to the next track in playlist.
         */
        fun onSkipToNext()

        /**
         * Called to handle a request to change the current repeat mode.
         * @param newMode The new repeat mode.
         */
        fun onRepeatModeChanged(@PlaybackStateCompat.RepeatMode newMode: Int)

        /**
         * Called to handle a request to change the current shuffle mode.
         * @param newMode The new shuffle mode.
         */
        fun onShuffleModeChanged(@PlaybackStateCompat.ShuffleMode newMode: Int)

    }

    private inner class WidgetClickListener : View.OnClickListener {

        override fun onClick(view: View) {
            listener?.also {
                when (view.id) {
                    R.id.masterPlayPause, R.id.playPauseButton -> {
                        if (lastPlaybackState?.state == PlaybackStateCompat.STATE_PLAYING) {
                            it.onActionPause()
                        } else {
                            it.onActionPlay()
                        }
                    }
                    R.id.skipPrevButton, R.id.miniPrevButton -> it.onSkipToPrevious()
                    R.id.skipNextButton, R.id.miniNextButton -> it.onSkipToNext()
                    R.id.shuffleButton -> it.onShuffleModeChanged(
                            if (shuffleButton.isActivated) PlaybackStateCompat.SHUFFLE_MODE_NONE
                            else PlaybackStateCompat.SHUFFLE_MODE_ALL)
                    R.id.repeatButton -> {
                        repeatMode = (repeatMode + 1) % 3
                        it.onRepeatModeChanged(repeatMode)
                    }
                }
            }
        }
    }

    /**
     * A parcelable object that saves the internal state of a PlayerView.
     */
    private class SavedState : View.BaseSavedState {
        @JvmField var lastPlaybackState: PlaybackStateCompat? = null
        @JvmField var metadata: MediaMetadataCompat? = null
        @JvmField var repeatMode: Int = 0
        @JvmField var expanded: Boolean = false

        constructor(superState: Parcelable) : super(superState)

        constructor(parcel: Parcel) : super(parcel) {
            lastPlaybackState = parcel.readParcelable(PlaybackStateCompat::class.java.classLoader)
            metadata = parcel.readParcelable(MediaMetadataCompat::class.java.classLoader)
            repeatMode = parcel.readInt()
            expanded = parcel.readInt() != 0
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeParcelable(lastPlaybackState, flags)
            out.writeParcelable(metadata, flags)
            out.writeInt(repeatMode)
            out.writeInt(if (expanded) 1 else 0)
        }

        companion object {

            @JvmField val CREATOR = object : Parcelable.Creator<SavedState> {
                override fun createFromParcel(parcel: Parcel) = SavedState(parcel)
                override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
            }
        }
    }
}
