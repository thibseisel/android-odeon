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

package fr.nihilus.music.media

/**
 * An object that groups constants used in the whole application.
 */
object Constants {

    /**
     * Extra for `prepareFrom*` and `playFrom*` methods of `MediaControllerCompat`.
     * If supplied and `true`, shuffle mode will be enabled once media has been prepared.
     *
     * Type: `Boolean`
     */
    const val EXTRA_PLAY_SHUFFLED = "play_shuffled"
}