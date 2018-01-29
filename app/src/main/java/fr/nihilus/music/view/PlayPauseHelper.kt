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

import android.graphics.drawable.Animatable
import android.widget.ImageView

private const val LEVEL_PLAYING = 0
private const val LEVEL_PAUSED = 1

/**
 * Helper class providing an easy way to implement an ImageView whose displayed image
 * can be toggled between two states, such as a play-pause button.
 *
 * @param view The ImageView that will display the icon.
 */
class PlayPauseHelper(val view: ImageView) {

    /**
     * Whether the associated view should display its "playing" state.
     * If the configured drawables can be animated, then the animation will only be triggered
     * if this property value changes.
     *
     * This is `null` by default so that the icon is always animated
     * the first time this property value is set.
     */
    var isPlaying: Boolean? = null
        set(value) {
            if (field != value) {

                if (field == null) {
                    // Initializing state : do not trigger an animation.
                    // Reverse the logic to show the correct static icon.
                    view.setImageLevel(if (value == true) LEVEL_PAUSED else LEVEL_PLAYING)
                } else {
                    // Set the correct level then trigger transition to the next state
                    view.setImageLevel(if (value == true) LEVEL_PLAYING else LEVEL_PAUSED)
                    (view.drawable?.current as? Animatable)?.start()
                }

                field = value
            }
        }
}