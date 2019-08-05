/*
 * Copyright 2019 Thibault Seisel
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

package fr.nihilus.music.library.nowplaying

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.lifecycle.ViewModelProviders
import androidx.lifecycle.observe
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import fr.nihilus.music.R
import fr.nihilus.music.base.BaseFragment
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.GlideRequest
import fr.nihilus.music.glide.SwitcherTarget
import fr.nihilus.music.media.extensions.displayIconUri
import fr.nihilus.music.media.extensions.isPlaying
import fr.nihilus.music.ui.ProgressAutoUpdater
import kotlinx.android.synthetic.main.fragment_now_playing.*
import kotlinx.android.synthetic.main.fragment_now_playing_top.*
import timber.log.Timber

private const val LEVEL_CHEVRON_UP = 0
private const val LEVEL_CHEVRON_DOWN = 1

private const val KEY_IS_COLLAPSED = "fr.nihilus.music.library.nowplaying.NowPlayingFragment.IS_COLLAPSED"

class NowPlayingFragment: BaseFragment(R.layout.fragment_now_playing) {
    private var playerExpansionListener: ((Boolean) -> Unit)? = null
    private var isCollapsed = true

    private lateinit var glideRequest: GlideRequest<Drawable>
    private lateinit var albumArtTarget: SwitcherTarget
    private lateinit var autoUpdater: ProgressAutoUpdater

    private val viewModel by lazy(LazyThreadSafetyMode.NONE) {
        ViewModelProviders.of(this, viewModelFactory).get(NowPlayingViewModel::class.java)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        albumArtTarget = SwitcherTarget(album_art_switcher)
        autoUpdater = ProgressAutoUpdater(seek_bar, seek_position, seek_duration) { position ->
            viewModel.seekTo(position)
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

        val clickHandler = WidgetClickListener()
        play_pause_button.setOnClickListener(clickHandler)

        // Buttons that are only present in landscape mode
        mini_prev_button?.setOnClickListener(clickHandler)
        mini_next_button?.setOnClickListener(clickHandler)

        // Playback control buttons at bottom
        repeat_button.setOnClickListener(clickHandler)
        skip_prev_button.setOnClickListener(clickHandler)
        master_play_pause.setOnClickListener(clickHandler)
        skip_next_button.setOnClickListener(clickHandler)
        shuffle_button.setOnClickListener(clickHandler)
        chevron.setOnClickListener(clickHandler)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        glideRequest = GlideApp.with(this).asDrawable()
            .fallback(R.drawable.ic_audiotrack_24dp)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .centerCrop()

        viewModel.playbackState.observe(this, this::onPlaybackStateChanged)
        viewModel.nowPlaying.observe(this, this::onMetadataChanged)
        viewModel.repeatMode.observe(this) {
            onRepeatModeChanged(it)
        }
        viewModel.shuffleMode.observe(this) {
            onShuffleModeChanged(it)
        }

        if (savedInstanceState != null) {
            isCollapsed = savedInstanceState.getBoolean(KEY_IS_COLLAPSED, true)
            setCollapsedInternal(isCollapsed)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_COLLAPSED, isCollapsed)
    }

    /**
     * Listens for requests for this Fragment to expand the BottomSheet it is in, if any.
     */
    fun setOnRequestPlayerExpansionListener(listener: ((isCollapsed: Boolean) -> Unit)?) {
        playerExpansionListener = listener
    }

    /**
     * Requests this fragment to change its display to reflect a collapsed or an expanded state.
     * When collapsed, only a part of its Views are visible at a time.
     * When expanded, all its views are visible.
     */
    fun setCollapsed(isCollapsed: Boolean) {
        if (this.isCollapsed != isCollapsed) {
            this.isCollapsed = isCollapsed
            setCollapsedInternal(isCollapsed)
        }
    }

    private fun setCollapsedInternal(isCollapsed: Boolean) {
        chevron.setImageLevel(if (isCollapsed) LEVEL_CHEVRON_UP else LEVEL_CHEVRON_DOWN)
        val targetVisibility = if (isCollapsed) View.VISIBLE else View.GONE
        play_pause_button.visibility = targetVisibility
        mini_prev_button?.visibility = targetVisibility
        mini_next_button?.visibility = targetVisibility
    }

    private fun onMetadataChanged(metadata: MediaMetadataCompat?) {
        if (metadata != null) {
            val media = metadata.description
            title_view.text = media.title
            subtitle_view.text = media.subtitle

            autoUpdater.setMetadata(metadata)
            glideRequest.load(metadata.displayIconUri).into(albumArtTarget)

        } else {
            // Reset views
            title_view.text = null
            subtitle_view.text = null

            autoUpdater.setMetadata(null)
            Glide.with(this).clear(albumArtTarget)
        }
    }

    private fun onShuffleModeChanged(@PlaybackStateCompat.ShuffleMode mode: Int) {
        shuffle_button.apply {
            isActivated = mode == SHUFFLE_MODE_ALL
        }
    }

    private fun onRepeatModeChanged(@PlaybackStateCompat.RepeatMode mode: Int) {
        repeat_button.apply {
            isActivated = (mode == REPEAT_MODE_ONE) || (mode == REPEAT_MODE_ALL)
            setImageLevel(mode)
        }
    }

    /**
     * Updates the playback state currently represented by this fragment's views.
     * Playback state describes what actions are available.
     *
     * @param newState The last playback state.
     */
    private fun onPlaybackStateChanged(newState: PlaybackStateCompat?) {
        // Update playback state for seekBar region
        autoUpdater.setPlaybackState(newState)

        if (newState != null) {
            toggleControls(newState.actions)

            val isPlaying = newState.isPlaying
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
        play_pause_button.isEnabled = actions and ACTION_PLAY_PAUSE != 0L

        mini_prev_button?.isEnabled = actions and ACTION_SKIP_TO_PREVIOUS != 0L
        mini_next_button?.isEnabled = actions and ACTION_SKIP_TO_NEXT != 0L

        repeat_button.isEnabled = actions and ACTION_SET_REPEAT_MODE != 0L
        skip_prev_button.isEnabled = actions and ACTION_SKIP_TO_PREVIOUS != 0L
        master_play_pause.isEnabled = actions and ACTION_PLAY_PAUSE != 0L
        skip_next_button.isEnabled = actions and ACTION_SKIP_TO_NEXT != 0L
        shuffle_button.isEnabled = actions and ACTION_SET_SHUFFLE_MODE != 0L
    }

    private inner class WidgetClickListener : View.OnClickListener {

        override fun onClick(view: View) {
            when(view.id) {
                R.id.chevron -> playerExpansionListener?.invoke(!isCollapsed)
                R.id.master_play_pause, R.id.play_pause_button -> viewModel.togglePlayPause()
                R.id.skip_prev_button, R.id.mini_prev_button -> viewModel.skipToPrevious()
                R.id.skip_next_button, R.id.mini_next_button -> viewModel.skipToNext()
                R.id.shuffle_button -> viewModel.toggleShuffleMode()
                R.id.repeat_button -> viewModel.toggleRepeatMode()
                else -> Timber.w("Unhandled click event for View : %s", view.javaClass.name)
            }
        }
    }
}