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
import android.support.design.widget.FloatingActionButton
import android.util.AttributeSet

/**
 * Extension of a FloatingActionButton specialized for a play/pause button.
 *
 * The drawable displayed by this FloatingActionButton must be a LevelListDrawable with 2 levels:
 * - (0) is the drawable shown while playback is paused (play icon)
 * - (1) is the drawable shown while playing (pause icon)
 */
class FloatingPlayPauseButton
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FloatingActionButton(context, attrs, defStyleAttr) {

    private val helper = PlayPauseHelper(this)

    /**
     * Whether this button should display its "playing" state.
     */
    var isPlaying: Boolean
        get() = helper.isPlaying == true
        set(value) {
            helper.isPlaying = value
        }
}