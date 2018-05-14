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
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Helper class providing an easy way to implement an ImageView whose displayed image
 * can be toggled between two states, such as a play-pause button.
 *
 * If the configured drawables can be animated, then the animation will only be triggered
 * if this property value changes.
 */
class PlayPauseDelegate : ReadWriteProperty<ImageView, Boolean> {

    private var isPlaying: Boolean? = null

    override operator fun getValue(thisRef: ImageView, property: KProperty<*>) = isPlaying == true

    override operator fun setValue(thisRef: ImageView, property: KProperty<*>, value: Boolean) {
        if (isPlaying != value) {

            if (isPlaying == null) {
                // Initializing state : do not trigger an animation.
                // Reverse the logic to show the correct static icon.
                thisRef.setImageLevel(if (value) LEVEL_PAUSED else LEVEL_PLAYING)
            } else {
                // Set the correct level then trigger transition to the next state
                thisRef.setImageLevel(if (value) LEVEL_PLAYING else LEVEL_PAUSED)
                (thisRef.drawable?.current as? Animatable)?.start()
            }

            isPlaying = value
        }
    }

    private companion object {
        private const val LEVEL_PLAYING = 0
        private const val LEVEL_PAUSED = 1
    }
}