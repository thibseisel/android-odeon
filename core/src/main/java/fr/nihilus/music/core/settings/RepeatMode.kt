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

package fr.nihilus.music.core.settings

/**
 * Defines how tracks in a playing queue should be repeated.
 */
enum class RepeatMode {
    /**
     * All tracks in the queue are played once in order.
     * When all tracks have been played, the player stops.
     */
    DISABLED,
    /**
     * When the playback of the current track completes the same track is replayed indefinitely.
     * To play a different track it is necessary to skip to another track in the queue.
     */
    ALL,
    /**
     * All tracks in the queue are played once in order.
     * When all tracks have been played the player replays the whole queue until manually stopped.
     */
    ONE
}