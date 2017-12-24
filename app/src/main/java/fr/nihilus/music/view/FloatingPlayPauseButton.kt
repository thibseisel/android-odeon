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
import android.graphics.drawable.Animatable
import android.os.Build
import android.support.design.widget.FloatingActionButton
import android.util.AttributeSet

/**
 * Show the pause icon while playing.
 */
private const val LEVEL_PLAYING = 1

/**
 * Show the play icon while not playing.
 */
private const val LEVEL_PAUSED = 0

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
): FloatingActionButton(context, attrs, defStyleAttr) {

    /**
     * Whether this button should display its "playing" state.
     */
    var isPlaying: Boolean = true
        set(value) {
            if (field != value) {
                field = value

                // Change image level depending on the new state
                setImageLevel(if (value) LEVEL_PLAYING else LEVEL_PAUSED)

                // Apply AnimatedVectorDrawable animation for API 21+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    (drawable?.current as? Animatable)?.start()
                }
            }
        }
}