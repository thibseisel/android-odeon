/*
 * Copyright 2022 Thibault Seisel
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

package fr.nihilus.music.media.artists

/**
 * The metadata of an artist that produced tracks that are saved on the device's storage.
 */
data class Artist(
    /**
     * The unique identifier of this artist on the device's media storage index.
     */
    val id: Long,
    /**
     * The full name of this artist as it should be displayed to the user.
     */
    val name: String,
    /**
     * The number of album this artist as recorded.
     */
    val albumCount: Int,
    /**
     * The number of tracks that have been produced by this artist, all albums combined.
     */
    val trackCount: Int,
    /**
     * An optional Uri-formatted String pointing to an artwork representing this artist.
     */
    val iconUri: String?
)