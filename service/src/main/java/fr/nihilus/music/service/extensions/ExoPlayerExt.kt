/*
 * Copyright 2019 Thibault Seisel
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

package fr.nihilus.music.service.extensions

import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer

/**
 * Execute the given action once when the structure of media has changed.
 *
 * @param action The action to execute with the new timeline, right after it has changed.
 */
inline fun ExoPlayer.doOnPrepared(crossinline action: (Timeline) -> Unit) {
    val preparationListener = object : Player.Listener {
        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            this@doOnPrepared.removeListener(this)
            action(timeline)
        }
    }

    this.addListener(preparationListener)
}
