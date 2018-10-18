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

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.content.res.AppCompatResources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import fr.nihilus.music.R
import fr.nihilus.music.client.BrowserViewModel
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.SwitcherTarget
import fr.nihilus.music.media.extensions.isPlaying
import fr.nihilus.music.view.ProgressAutoUpdater
import kotlinx.android.synthetic.main.fragment_now_playing.*
import kotlinx.android.synthetic.main.fragment_now_playing_top.*

class NowPlayingFragment: Fragment() {

    private val glideRequest = GlideApp.with(this).asDrawable()
        .fallback(R.drawable.ic_audiotrack_24dp)
        .diskCacheStrategy(DiskCacheStrategy.NONE)
        .centerCrop()

    private lateinit var albumArtTarget: SwitcherTarget
    private lateinit var autoUpdater: ProgressAutoUpdater

    private val browserViewModel by lazy {
        ViewModelProviders.of(this)[BrowserViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_now_playing, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        albumArtTarget = SwitcherTarget(albumArtSwitcher)
        autoUpdater = ProgressAutoUpdater(seekBar, seekPosition, seekDuration) { position ->
            browserViewModel.post { it.transportControls.seekTo(position) }
        }

        seekBar.setOnSeekBarChangeListener(autoUpdater)

        // Change color when shuffle mode and repeat mode buttons are activated
        val activationStateList = AppCompatResources.getColorStateList(
            context,
            R.color.activation_state_list
        )
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

        val clickListener = WidgetClickListener()
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

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        browserViewModel.playbackState.observe(this, Observer(this::updatePlaybackState))
        browserViewModel.currentMetadata.observe(this, Observer(this::updateMetadata))
        browserViewModel.repeatMode.observe(this, Observer(this::setShuffleMode))

        browserViewModel.shuffleMode.observe(this, Observer {
            setRepeatMode(it ?: PlaybackStateCompat.SHUFFLE_MODE_NONE)
        })
    }

    private fun updateMetadata(metadata: MediaMetadataCompat?) {
        if (metadata != null) {
            val media = metadata.description
            titleView.text = media.title
            subtitleView.text = media.subtitle

            autoUpdater.setMetadata(metadata)
            val artUri = metadata.getString(MediaMetadataCompat.METADATA_KEY_ALBUM_ART_URI)
            glideRequest.load(artUri).into(albumArtTarget)

        } else {
            // Reset views
            titleView.text = null
            subtitleView.text = null

            autoUpdater.setMetadata(null)
            Glide.with(this).clear(albumArtTarget)
        }
    }

    private fun setShuffleMode(@PlaybackStateCompat.ShuffleMode mode: Int?) {
        shuffleButton.isActivated = mode == PlaybackStateCompat.SHUFFLE_MODE_ALL
    }

    private fun setRepeatMode(@PlaybackStateCompat.RepeatMode mode: Int) {
        with(repeatButton) {
            setImageLevel(mode)
            isActivated = mode != PlaybackStateCompat.SHUFFLE_MODE_NONE
        }
    }

    /**
     * Updates the playback state currently represented by this fragment's views.
     * Playback state describes what actions are available.
     *
     * @param newState The last playback state.
     */
    private fun updatePlaybackState(newState: PlaybackStateCompat?) {
        // Update playback state for seekBar region
        autoUpdater.setPlaybackState(newState)

        if (newState != null) {
            toggleControls(newState.actions)

            val isPlaying = newState.isPlaying
            playPauseButton.isPlaying = isPlaying
            masterPlayPause.isPlaying = isPlaying

        } else {
            toggleControls(0L)
            playPauseButton.isPlaying = false
            masterPlayPause.isPlaying = false
        }
    }

    /**
     * Enables or disables actions depending on parameters provided by the current playback state.
     * If an action is disabled, its associated view is also disabled and does not react to clicks.
     *
     * @param actions A set of flags describing what actions are available on this media session.
     */
    private fun toggleControls(@PlaybackStateCompat.Actions actions: Long) {
        playPauseButton.isEnabled = actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L

        miniPrevButton?.isEnabled = actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L
        miniNextButton?.isEnabled = actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L

        repeatButton.isEnabled = actions and PlaybackStateCompat.ACTION_SET_REPEAT_MODE != 0L
        skipPrevButton.isEnabled = actions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS != 0L
        masterPlayPause.isEnabled = actions and PlaybackStateCompat.ACTION_PLAY_PAUSE != 0L
        skipNextButton.isEnabled = actions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT != 0L
        shuffleButton.isEnabled = actions and PlaybackStateCompat.ACTION_SET_SHUFFLE_MODE != 0L
    }

    private inner class WidgetClickListener : View.OnClickListener {

        override fun onClick(view: View) = when(view.id) {
            R.id.masterPlayPause, R.id.playPauseButton -> browserViewModel.togglePlayPause()
            R.id.skipPrevButton, R.id.miniPrevButton -> browserViewModel.post { it.transportControls.skipToPrevious() }
            R.id.skipNextButton, R.id.miniNextButton -> browserViewModel.post { it.transportControls.skipToNext() }
            R.id.shuffleButton -> browserViewModel.toggleShuffleMode()
            R.id.repeatButton -> browserViewModel.toggleRepeatMode()
            else -> error("Unhandled click event for view: $view")
        }
    }
}