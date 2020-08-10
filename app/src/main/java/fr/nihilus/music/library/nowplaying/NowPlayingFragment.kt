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
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.load.engine.DiskCacheStrategy
import fr.nihilus.music.R
import fr.nihilus.music.core.playback.RepeatMode
import fr.nihilus.music.core.ui.base.BaseFragment
import fr.nihilus.music.glide.SwitcherTarget
import kotlinx.android.synthetic.main.fragment_now_playing.*
import kotlinx.android.synthetic.main.fragment_now_playing_top.*
import timber.log.Timber

private const val LEVEL_CHEVRON_UP = 0
private const val LEVEL_CHEVRON_DOWN = 1

private const val KEY_IS_COLLAPSED = "fr.nihilus.music.library.nowplaying.NowPlayingFragment.IS_COLLAPSED"

class NowPlayingFragment: BaseFragment(R.layout.fragment_now_playing) {
    private var playerExpansionListener: ((Boolean) -> Unit)? = null
    private var isCollapsed = true

    private lateinit var glideRequest: RequestBuilder<Drawable>
    private lateinit var albumArtTarget: SwitcherTarget
    private lateinit var autoUpdater: ProgressAutoUpdater

    private val viewModel by viewModels<NowPlayingViewModel> { viewModelFactory }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        albumArtTarget = SwitcherTarget(album_art_switcher)
        autoUpdater = ProgressAutoUpdater(seek_bar, seek_position, seek_duration) {
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

        glideRequest = Glide.with(this).asDrawable()
            .error(R.drawable.ic_audiotrack_24dp)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .centerCrop()

        viewModel.state.observe(viewLifecycleOwner, ::onPlayerStateChanged)

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

    private fun onPlayerStateChanged(state: PlayerState) {
        // Update controls activation based on available actions.
        updateControls(state.availableActions)

        // Update display of play/pause toggle buttons
        play_pause_button.isPlaying = state.isPlaying
        master_play_pause.isPlaying = state.isPlaying

        // Update appearance of shuffle and repeat buttons based on current mode.
        shuffle_button.isActivated = state.shuffleModeEnabled
        repeat_button.isActivated = state.repeatMode != RepeatMode.DISABLED
        repeat_button.setImageLevel(if (state.repeatMode == RepeatMode.ONE) 1 else 0)

        if (state.currentTrack != null) {
            val media = state.currentTrack

            // Set the title and the description.
            title_view.text = media.title
            subtitle_view.text = media.artist

            // Update progress and labels
            autoUpdater.update(state.position, media.duration, state.lastPositionUpdateTime, state.isPlaying)

            // Update artwork.
            glideRequest.load(media.artworkUri).into(albumArtTarget)

        } else {
            title_view.text = null
            subtitle_view.text = null

            autoUpdater.update(0L, 0L, state.lastPositionUpdateTime, false)
        }
    }

    private fun updateControls(availableActions: Set<PlayerState.Action>) {
        play_pause_button.isEnabled = PlayerState.Action.TOGGLE_PLAY_PAUSE in availableActions

        mini_prev_button?.isEnabled = PlayerState.Action.SKIP_BACKWARD in availableActions
        mini_next_button?.isEnabled = PlayerState.Action.SKIP_FORWARD in availableActions

        repeat_button.isEnabled = PlayerState.Action.SET_REPEAT_MODE in availableActions
        skip_prev_button.isEnabled = PlayerState.Action.SKIP_BACKWARD in availableActions
        master_play_pause.isEnabled = PlayerState.Action.TOGGLE_PLAY_PAUSE in availableActions
        skip_next_button.isEnabled = PlayerState.Action.SKIP_FORWARD in availableActions
        shuffle_button.isEnabled = PlayerState.Action.SET_SHUFFLE_MODE in availableActions
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
                else -> Timber.w("Unhandled click event for View with id: %s", view.id)
            }
        }
    }
}