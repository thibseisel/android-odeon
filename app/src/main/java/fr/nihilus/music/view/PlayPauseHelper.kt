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

import android.graphics.drawable.Animatable
import android.os.Build
import android.widget.ImageView

/**
 * Show the pause icon while playing.
 */
private const val LEVEL_PLAYING = 1

/**
 * Show the play icon while not playing.
 */
private const val LEVEL_PAUSED = 0

/**
 * Helper class providing an easy way to implement an ImageView whose displayed image
 * can be toggled between two states, such as a play-pause button.
 *
 * @param view The ImageView that will display the icon.
 */
internal class PlayPauseHelper(private val view: ImageView) {

    /**
     * Whether the associated view should display its "playing" state.
     * If the callbackConfigured drawables can be animated, then the animation will only be triggered
     * if this property value changes.
     *
     * The default is `true`.
     */
    var isPlaying: Boolean = true
        set(value) {
            if (field != value) {
                field = value

                // Change image level depending on the new state
                view.setImageLevel(if (value) LEVEL_PLAYING else LEVEL_PAUSED)

                // Apply AnimatedVectorDrawable animation for API 21+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    (view.drawable?.current as? Animatable)?.start()
                }
            }
        }
}