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

package fr.nihilus.music.spotify.service

import fr.nihilus.music.spotify.model.Album
import fr.nihilus.music.spotify.model.Artist
import fr.nihilus.music.spotify.model.Track

internal class SpotifyQuery(
    val query: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val track: String? = null
)

internal sealed class SearchType<T : Any> {
    object Tracks : SearchType<Track>()
    object Albums : SearchType<Album>()
    object Artists : SearchType<Artist>()
}