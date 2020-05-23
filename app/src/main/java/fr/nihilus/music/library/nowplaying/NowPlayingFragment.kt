/*
 * Copyright 2020 Thibault Seisel
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
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.observe
import com.bumptech.glide.load.engine.DiskCacheStrategy
import fr.nihilus.music.R
import fr.nihilus.music.core.playback.RepeatMode
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.glide.GlideApp
import fr.nihilus.music.glide.GlideRequest
import fr.nihilus.music.glide.SwitcherTarget
import kotlinx.android.synthetic.main.player_expanded.*
import timber.log.Timber

class NowPlayingFragment: BaseFragment(R.layout.player_expanded) {

    private lateinit var glideRequest: GlideRequest<Drawable>
    private lateinit var albumArtTarget: SwitcherTarget
    private lateinit var autoUpdater: ProgressAutoUpdater

    private val viewModel by activityViewModels<NowPlayingViewModel> { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        albumArtTarget = SwitcherTarget(now_playing_artwork)
        autoUpdater = ProgressAutoUpdater(now_playing_progress, now_playing_position, now_playing_duration) {
            position -> viewModel.seekTo(position)
        }

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

        // Playback control buttons at bottom
        repeat_button.setOnClickListener(clickHandler)
        skip_prev_button.setOnClickListener(clickHandler)
        now_playing_toggle.setOnClickListener(clickHandler)
        skip_next_button.setOnClickListener(clickHandler)
        shuffle_button.setOnClickListener(clickHandler)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        glideRequest = GlideApp.with(this).asDrawable()
            .error(R.drawable.ic_audiotrack_24dp)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .centerCrop()

        viewModel.state.observe(viewLifecycleOwner, ::onPlayerStateChanged)
    }

    private fun onPlayerStateChanged(state: PlayerState) {
        // Update controls activation based on available actions.
        updateControls(state.availableActions)

        // Update display of play/pause toggle buttons
        now_playing_toggle.isPlaying = state.isPlaying

        // Update appearance of shuffle and repeat buttons based on current mode.
        shuffle_button.isActivated = state.shuffleModeEnabled
        repeat_button.isActivated = state.repeatMode != RepeatMode.DISABLED
        repeat_button.setImageLevel(if (state.repeatMode == RepeatMode.ONE) 1 else 0)

        if (state.currentTrack != null) {
            val media = state.currentTrack

            // Set the title and the description.
            now_playing_title.text = media.title

            // Update progress and labels
            autoUpdater.update(state.position, media.duration, state.lastPositionUpdateTime, state.isPlaying)

            // Update artwork.
            glideRequest.load(media.artworkUri).into(albumArtTarget)

        } else {
            now_playing_title.text = null
            autoUpdater.update(0L, 0L, state.lastPositionUpdateTime, false)
        }
    }

    private fun updateControls(availableActions: Set<PlayerState.Action>) {
        repeat_button.isEnabled = PlayerState.Action.SET_REPEAT_MODE in availableActions
        skip_prev_button.isEnabled = PlayerState.Action.SKIP_BACKWARD in availableActions
        now_playing_toggle.isEnabled = PlayerState.Action.TOGGLE_PLAY_PAUSE in availableActions
        skip_next_button.isEnabled = PlayerState.Action.SKIP_FORWARD in availableActions
        shuffle_button.isEnabled = PlayerState.Action.SET_SHUFFLE_MODE in availableActions
    }

    private inner class WidgetClickListener : View.OnClickListener {

        override fun onClick(view: View) {
            when(view.id) {
                R.id.now_playing_toggle -> viewModel.togglePlayPause()
                R.id.skip_prev_button -> viewModel.skipToPrevious()
                R.id.skip_next_button -> viewModel.skipToNext()
                R.id.shuffle_button -> viewModel.toggleShuffleMode()
                R.id.repeat_button -> viewModel.toggleRepeatMode()
                else -> Timber.w("Unhandled click event for View with id: %s", view.id)
            }
        }
    }
}