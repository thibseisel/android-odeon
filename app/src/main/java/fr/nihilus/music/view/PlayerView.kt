/*
 * Copyright 2018 Thibault Seisel
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
import android.graphics.drawable.Drawable
import android.os.Parcel
import android.os.Parcelable
import android.support.constraint.ConstraintLayout
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.content.res.AppCompatResources
import android.util.AttributeSet
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.GlideRequest
import fr.nihilus.music.glide.SwitcherTarget
import fr.nihilus.music.utils.dipToPixels
import kotlinx.android.synthetic.main.view_player.view.*
import kotlinx.android.synthetic.main.view_player_top.view.*

@Deprecated("This View is progressively replaced by NowPlayingFragment")
class PlayerView
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private lateinit var glideRequest: GlideRequest<Drawable>

    private lateinit var autoUpdater: ProgressAutoUpdater
    private lateinit var albumArtTarget: SwitcherTarget

    private var isExpanded = false
    private var repeatMode = PlaybackStateCompat.REPEAT_MODE_NONE

    private var lastPlaybackState: PlaybackStateCompat? = null
    private var metadata: MediaMetadataCompat? = null

    private var listener: EventListener? = null

    init {
        View.inflate(context, R.layout.view_player, this)

        // Prevent from dispatching touches to views behind
        isClickable = true

        if (!isInEditMode) {
            glideRequest = GlideApp.with(context).asDrawable()
                .fallback(R.drawable.ic_audiotrack_24dp)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .centerCrop()
        }
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        // Create target for loading album art into ImageSwitcher
        albumArtTarget = SwitcherTarget(album_art_switcher)

        // Configure auto-update for SeekBar update and associated TextViews
        autoUpdater = ProgressAutoUpdater(seek_bar, seek_position, seek_duration) { position ->
            listener?.onSeek(position)
        }

        seek_bar.setOnSeekBarChangeListener(autoUpdater)

        // Change color when shuffle mode and repeat mode buttons are activated
        val activationStateList = AppCompatResources.getColorStateList(
            context,
            R.color.activation_state_list
        )
        with(shuffle_button) {
            val shuffleDrawable = DrawableCompat.wrap(this.drawable)
            DrawableCompat.setTintList(shuffleDrawable, activationStateList)
            setImageDrawable(shuffleDrawable)
        }

        with(repeat_button) {
            val repeatDrawable = DrawableCompat.wrap(this.drawable)
            DrawableCompat.setTintList(repeatDrawable, activationStateList)
            setImageDrawable(repeatDrawable)
        }

        // Associate a common click listener for each button
        val clickListener = WidgetClickListener()

        // Buttons in top bar
        play_pause_button.setOnClickListener(clickListener)

        // Buttons that are only present in landscape mode
        mini_prev_button?.setOnClickListener(clickListener)
        mini_next_button?.setOnClickListener(clickListener)

        // Playback control buttons at bottom
        repeat_button.setOnClickListener(clickListener)
        skip_prev_button.setOnClickListener(clickListener)
        master_play_pause.setOnClickListener(clickListener)
        skip_next_button.setOnClickListener(clickListener)
        shuffle_button.setOnClickListener(clickListener)

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
            if (expanded) onOpen() else onClose()
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
            title_view.text = media.title
            subtitle_view.text = media.subtitle

            val bitmap = media.iconBitmap
            if (bitmap != null) {
                icon_view.setImageBitmap(media.iconBitmap)
            } else {
                icon_view.setImageResource(R.drawable.ic_audiotrack_24dp)
            }

        } else {
            // Reset views
            title_view.text = null
            subtitle_view.text = null
            icon_view.setImageDrawable(null)
        }

        // Only update PlayerView bottom if it is currently visible
        if (isExpanded) {
            updateExpandedView(metadata)
        }

    }

    private fun updateExpandedView(metadata: MediaMetadataCompat?) {

        if (metadata != null) {
            autoUpdater.setMetadata(metadata)

            val artUri = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
            glideRequest.load(artUri).into(albumArtTarget)

        } else {
            Glide.with(this).clear(albumArtTarget)
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

        // Update playback state for seekBar region
        autoUpdater.setPlaybackState(newState)

        if (newState != null) {
            toggleControls(newState.actions)

            val isPlaying = newState.state == PlaybackStateCompat.STATE_PLAYING
            play_pause_button.isPlaying = isPlaying
            master_play_pause.isPlaying = isPlaying

        } else {
            toggleControls(0L)
            play_pause_button.isPlaying = false
            master_play_pause.isPlaying = false
        }
    }

    /**
     * Enables or disables actions depending on parameters provided by the current playback state.
     * If an action is disabled, its associated view is also disabled and does not react to clicks.
     *
     * @param actions A set of flags describing what actions are available on this media session.
     */
    private fun toggleControls(@PlaybackStateCompat.Actions actions: Long) {
        play_pause_button.isEnabled = actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L

        mini_prev_button?.isEnabled = actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L
        mini_next_button?.isEnabled = actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L

        repeat_button.isEnabled = actions and PlaybackStateCompat.ACTION_SET_REPEAT_MODE != 0L
        skip_prev_button.isEnabled = actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L
        master_play_pause.isEnabled = actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L
        skip_next_button.isEnabled = actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L
        shuffle_button.isEnabled = actions and PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE != 0L
    }

    private fun onOpen() {
        val eightDps = dipToPixels(context, 8f)
        icon_view.setPadding(eightDps, eightDps, eightDps, eightDps)

        play_pause_button.visibility = View.GONE
        mini_prev_button?.visibility = View.GONE
        mini_next_button?.visibility = View.GONE

        // Update display of the hidden part of PlayerView as it is made visible
        updateExpandedView(metadata)
    }

    private fun onClose() {
        icon_view.setPadding(0, 0, 0, 0)

        play_pause_button.visibility = View.VISIBLE
        mini_prev_button?.visibility = View.VISIBLE
        mini_next_button?.visibility = View.VISIBLE
    }

    /**
     * Updates display of the shuffle mode of this PlayerView.
     * For requests to change the shuffle mode to be accurate, this value should always
     * be in sync with the media session's.
     *
     * @param mode The current shuffle mode for this media session.
     */
    fun setRepeatMode(@PlaybackStateCompat.RepeatMode mode: Int) {
        with(repeat_button) {
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
        shuffle_button.isActivated = mode != PlaybackStateCompat.SHUFFLE_MODE_NONE
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

        repeatMode = state.repeatMode

        setExpanded(state.expanded)
        updateMetadata(state.metadata)
        updatePlaybackState(state.lastPlaybackState)
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
                    R.id.master_play_pause, R.id.play_pause_button -> {
                        if (lastPlaybackState?.state == PlaybackStateCompat.STATE_PLAYING) {
                            it.onActionPause()
                        } else {
                            it.onActionPlay()
                        }
                    }
                    R.id.skip_prev_button, R.id.mini_prev_button -> it.onSkipToPrevious()
                    R.id.skip_next_button, R.id.mini_next_button -> it.onSkipToNext()
                    R.id.shuffle_button -> it.onShuffleModeChanged(
                        if (shuffle_button.isActivated) PlaybackStateCompat.SHUFFLE_MODE_NONE
                        else PlaybackStateCompat.SHUFFLE_MODE_ALL
                    )
                    R.id.repeat_button -> {
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

        constructor(superState: Parcelable?) : super(superState)

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

        companion object CREATOR : Parcelable.Creator<SavedState> {
            override fun createFromParcel(parcel: Parcel) = SavedState(parcel)
            override fun newArray(size: Int) = arrayOfNulls<SavedState>(size)
        }
    }
}
