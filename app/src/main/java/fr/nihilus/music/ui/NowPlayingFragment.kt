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

package fr.nihilus.music.ui

import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.PlaybackStateCompat.*
import android.support.v7.content.res.AppCompatResources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import fr.nihilus.music.R
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.SwitcherTarget
import fr.nihilus.music.media.extensions.albumArtUri
import fr.nihilus.music.media.extensions.isPlaying
import fr.nihilus.music.utils.observeK
import fr.nihilus.music.view.ProgressAutoUpdater
import kotlinx.android.synthetic.main.fragment_now_playing.*
import kotlinx.android.synthetic.main.fragment_now_playing_top.*
import timber.log.Timber
import javax.inject.Inject

private const val LEVEL_CHEVRON_UP = 0
private const val LEVEL_CHEVRON_DOWN = 1

class NowPlayingFragment: Fragment() {

    @Inject lateinit var vmFactory: ViewModelProvider.Factory

    private val glideRequest = GlideApp.with(this).asDrawable()
        .fallback(R.drawable.ic_audiotrack_24dp)
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .centerCrop()

    private lateinit var albumArtTarget: SwitcherTarget
    private lateinit var autoUpdater: ProgressAutoUpdater

    private lateinit var viewModel: NowPlayingViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_now_playing, container, false)

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

        val clickListener = WidgetClickListener()
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewModel = ViewModelProviders.of(this, vmFactory)[NowPlayingViewModel::class.java]

        viewModel.isExpanded.observeK(this) {
            onSheetExpansionChanged(it ?: false)
        }

        viewModel.playbackState.observeK(this, this::onPlaybackStateChanged)
        viewModel.nowPlaying.observeK(this, this::onMetadataChanged)
        viewModel.repeatMode.observeK(this) {
            onShuffleModeChanged(it ?: REPEAT_MODE_INVALID)
        }
        viewModel.shuffleMode.observeK(this) {
            onRepeatModeChanged(it ?: SHUFFLE_MODE_INVALID)
        }
    }

    private fun onSheetExpansionChanged(isExpanded: Boolean) {
        chevron.setImageLevel(if (isExpanded) LEVEL_CHEVRON_DOWN else LEVEL_CHEVRON_UP)
    }

    private fun onMetadataChanged(metadata: MediaMetadataCompat?) {
        if (metadata != null) {
            val media = metadata.description
            title_view.text = media.title
            subtitle_view.text = media.subtitle

            autoUpdater.setMetadata(metadata)
            glideRequest.load(metadata.albumArtUri).into(albumArtTarget)

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
            isEnabled = mode != SHUFFLE_MODE_INVALID
        }
    }

    private fun onRepeatModeChanged(@PlaybackStateCompat.RepeatMode mode: Int) {
        repeat_button.apply {
            isEnabled = mode != REPEAT_MODE_INVALID
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

        override fun onClick(view: View) = when(view.id) {
            R.id.master_play_pause, R.id.play_pause_button -> viewModel.togglePlayPause()
            R.id.skip_prev_button, R.id.mini_prev_button -> viewModel.skipToPrevious()
            R.id.skip_next_button, R.id.mini_next_button -> viewModel.skipToNext()
            R.id.shuffle_button -> viewModel.toggleShuffleMode()
            R.id.repeat_button -> viewModel.toggleRepeatMode()
            else -> Timber.w("Unhandled click event for View : %s", view.javaClass.name)
        }
    }
}