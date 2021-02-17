/*
 * Copyright 2021 Thibault Seisel
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
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import fr.nihilus.music.R
import fr.nihilus.music.core.playback.RepeatMode
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.core.ui.glide.SwitcherTarget
import fr.nihilus.music.databinding.FragmentNowPlayingBinding
import fr.nihilus.music.databinding.FragmentNowPlayingTopBinding
import timber.log.Timber

private const val LEVEL_CHEVRON_UP = 0
private const val LEVEL_CHEVRON_DOWN = 1

private const val KEY_IS_COLLAPSED = "fr.nihilus.music.library.nowplaying.NowPlayingFragment.IS_COLLAPSED"

class NowPlayingFragment: BaseFragment(R.layout.fragment_now_playing) {
    private var playerExpansionListener: ((Boolean) -> Unit)? = null
    private var isCollapsed = true

    private lateinit var albumArtTarget: SwitcherTarget
    private lateinit var autoUpdater: ProgressAutoUpdater

    private var binding: FragmentNowPlayingBinding? = null
    private var topBinding: FragmentNowPlayingTopBinding? = null

    private val viewModel by viewModels<NowPlayingViewModel> { viewModelFactory }

    private lateinit var glideRequest: RequestBuilder<Drawable>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        glideRequest = Glide.with(this).asDrawable()
            .error(R.drawable.ic_audiotrack_24dp)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .centerCrop()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = FragmentNowPlayingBinding.bind(view)
        val topBinding = FragmentNowPlayingTopBinding.bind(view)
        this.binding = binding
        this.topBinding = topBinding

        val context = requireContext()
        albumArtTarget = SwitcherTarget(binding.albumArtSwitcher)
        autoUpdater = ProgressAutoUpdater(binding.seekBar, binding.seekPosition, binding.seekDuration) {
            position -> viewModel.seekTo(position)
        }

        // Change color when shuffle mode and repeat mode buttons are activated
        val activationStateList = AppCompatResources.getColorStateList(
            context,
            R.color.activation_state_list
        )
        with(binding.shuffleButton) {
            val shuffleDrawable = DrawableCompat.wrap(this.drawable)
            DrawableCompat.setTintList(shuffleDrawable, activationStateList)
            setImageDrawable(shuffleDrawable)
        }

        with(binding.repeatButton) {
            val repeatDrawable = DrawableCompat.wrap(this.drawable)
            DrawableCompat.setTintList(repeatDrawable, activationStateList)
            setImageDrawable(repeatDrawable)
        }

        val clickHandler = WidgetClickListener()
        topBinding.playPauseButton.setOnClickListener(clickHandler)

        // Buttons that are only present in landscape mode
        topBinding.miniPrevButton?.setOnClickListener(clickHandler)
        topBinding.miniNextButton?.setOnClickListener(clickHandler)

        // Playback control buttons at bottom
        binding.repeatButton.setOnClickListener(clickHandler)
        binding.skipPrevButton.setOnClickListener(clickHandler)
        binding.masterPlayPause.setOnClickListener(clickHandler)
        binding.skipNextButton.setOnClickListener(clickHandler)
        binding.shuffleButton.setOnClickListener(clickHandler)
        topBinding.collapseIndicator.setOnClickListener(clickHandler)

        viewModel.state.observe(viewLifecycleOwner) {
            onPlayerStateChanged(it, binding, topBinding)
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
        val topBinding = topBinding ?: return
        topBinding.collapseIndicator.setImageLevel(
            when {
                isCollapsed -> LEVEL_CHEVRON_UP
                else -> LEVEL_CHEVRON_DOWN
            }
        )
        val targetVisibility = if (isCollapsed) View.VISIBLE else View.GONE
        topBinding.playPauseButton.visibility = targetVisibility
        topBinding.miniPrevButton?.visibility = targetVisibility
        topBinding.miniNextButton?.visibility = targetVisibility
    }

    private fun onPlayerStateChanged(
        state: PlayerState,
        binding: FragmentNowPlayingBinding,
        topBinding: FragmentNowPlayingTopBinding
    ) {
        // Update controls activation based on available actions.
        updateControls(state.availableActions)

        // Update display of play/pause toggle buttons
        topBinding.playPauseButton.isPlaying = state.isPlaying
        binding.masterPlayPause.isPlaying = state.isPlaying

        // Update appearance of shuffle and repeat buttons based on current mode.
        binding.shuffleButton.isActivated = state.shuffleModeEnabled
        binding.repeatButton.isActivated = state.repeatMode != RepeatMode.DISABLED
        binding.repeatButton.setImageLevel(if (state.repeatMode == RepeatMode.ONE) 1 else 0)

        if (state.currentTrack != null) {
            val media = state.currentTrack

            // Set the title and the description.
            topBinding.trackTitle.text = media.title
            topBinding.trackArtist.text = media.artist

            // Update progress and labels
            autoUpdater.update(state.position, media.duration, state.lastPositionUpdateTime, state.isPlaying)

            // Update artwork.
            glideRequest.load(media.artworkUri).into(albumArtTarget)

        } else {
            topBinding.trackTitle.text = null
            topBinding.trackArtist.text = null

            autoUpdater.update(0L, 0L, state.lastPositionUpdateTime, false)
        }
    }

    private fun updateControls(availableActions: Set<PlayerState.Action>) {
        val binding = binding ?: return
        val topBinding = topBinding ?: return

        topBinding.playPauseButton.isEnabled = PlayerState.Action.TOGGLE_PLAY_PAUSE in availableActions

        topBinding.miniPrevButton?.isEnabled = PlayerState.Action.SKIP_BACKWARD in availableActions
        topBinding.miniNextButton?.isEnabled = PlayerState.Action.SKIP_FORWARD in availableActions

        binding.repeatButton.isEnabled = PlayerState.Action.SET_REPEAT_MODE in availableActions
        binding.skipPrevButton.isEnabled = PlayerState.Action.SKIP_BACKWARD in availableActions
        binding.masterPlayPause.isEnabled = PlayerState.Action.TOGGLE_PLAY_PAUSE in availableActions
        binding.skipNextButton.isEnabled = PlayerState.Action.SKIP_FORWARD in availableActions
        binding.shuffleButton.isEnabled = PlayerState.Action.SET_SHUFFLE_MODE in availableActions
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        topBinding = null
    }

    private inner class WidgetClickListener : View.OnClickListener {

        override fun onClick(view: View) {
            when(view.id) {
                R.id.collapse_indicator -> playerExpansionListener?.invoke(!isCollapsed)
                R.id.master_play_pause, R.id.play_pause_button -> viewModel.togglePlayPause()
                R.id.skip_prev_button, R.id.mini_prev_button -> viewModel.skipToPrevious()
                R.id.skip_next_button, R.id.mini_next_button -> viewModel.skipToNext()
                R.id.shuffle_button -> viewModel.toggleShuffleMode()
                R.id.repeat_button -> viewModel.toggleRepeatMode()
                else -> Timber.w("Unhandled click event for View with id: %s", view.id)
            }
        }
    }
}