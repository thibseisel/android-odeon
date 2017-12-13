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

package fr.nihilus.music

/**
 * An object that groups constants used in the whole application.
 */
object Constants {

    /**
     * A argument key referring to the identifier of a fragment.
     *
     * Multiple fragments may share the same id: in this case they should be considered the same,
     * for example they should belong to the same navigation hierarchy.
     *
     * Type: Int
     */
    const val FRAGMENT_ID = "fragment_id"

    /**
     * Defines the action to explore the content of a specific item.
     */
    const val ACTION_BROWSE = 1

    /**
     * Defines the action to start playback of a specific item.
     */
    const val ACTION_PLAY = 2

}