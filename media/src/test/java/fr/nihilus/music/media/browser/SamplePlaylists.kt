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

package fr.nihilus.music.media.browser

import fr.nihilus.music.core.database.playlists.Playlist
import fr.nihilus.music.core.database.playlists.PlaylistTrack

internal val SAMPLE_PLAYLISTS = listOf(
    Playlist(1, "Zen", 1551434321, null),
    Playlist(2, "Sport", 1551435123, null),
    Playlist(3, "Metal", 1551436125, null)
)

internal val SAMPLE_MEMBERS = listOf(
    PlaylistTrack(1L, 309L),
    PlaylistTrack(2L, 477L),
    PlaylistTrack(2L, 48L),
    PlaylistTrack(2L, 125L),
    PlaylistTrack(3L, 75L),
    PlaylistTrack(3L, 161L)
)
