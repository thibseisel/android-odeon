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

package fr.nihilus.music.media.tracks.local

import fr.nihilus.music.core.files.bytes

internal object LocalTracks {
    internal val Cartagena = LocalTrack(
        id = 161,
        title = "1741 (The Battle of Cartagena)",
        albumId = 65,
        album = "Sunset on the Golden Age",
        artistId = 26,
        artist = "Alestorm",
        duration = 437603,
        discNumber = 1,
        trackNumber = 4,
        mediaUri = "Music/1741_(The_Battle_of_Cartagena).mp3",
        availabilityDate = 1466283480,
        fileSize = 17_506_481.bytes,
        albumArtUri = null,
    )

    internal val IsolatedSystem = LocalTrack(
        id = 309,
        title = "The 2nd Law: Isolated System",
        albumId = 40,
        album = "The 2nd Law",
        artistId = 18,
        artist = "Muse",
        duration = 300042,
        discNumber = 1,
        trackNumber = 13,
        mediaUri = "Music/The_2nd_Law_(Isolated_System).mp3",
        availabilityDate = 1439653800,
        fileSize = 12_075_967.bytes,
        albumArtUri = null,
    )

    internal val Algorithm = LocalTrack(
        id = 865,
        title = "Algorithm",
        albumId = 98,
        album = "Simulation Theory",
        artistId = 18,
        artist = "Muse",
        duration = 245960,
        discNumber = 1,
        trackNumber = 1,
        mediaUri = "Music/Simulation Theory/Algorithm.mp3",
        availabilityDate = 1576838717,
        fileSize = 10_806_478.bytes,
        albumArtUri = null,
    )

    internal val DirtyWater = LocalTrack(
        id = 481,
        title = "Dirty Water",
        albumId = 102,
        album = "Concrete and Gold",
        artistId = 13,
        artist = "Foo Fighters",
        duration = 320914,
        discNumber = 1,
        trackNumber = 6,
        mediaUri = "Music/Concrete And Gold/Dirty_Water.mp3",
        availabilityDate = 1506374520,
        fileSize = 12_912_282.bytes,
        albumArtUri = null,
    )

    internal val ThePretenders = LocalTrack(
        id = 464,
        title = "The Pretenders",
        albumId = 95,
        album = "Echoes, Silence, Patience & Grace",
        artistId = 13,
        artist = "Foo Fighters",
        duration = 266509,
        discNumber = 1,
        trackNumber = 1,
        mediaUri = "Music/The_Pretenders.mp3",
        availabilityDate = 1439653740,
        fileSize = 4_296_041.bytes,
        albumArtUri = null,
    )

    internal val Run = LocalTrack(
        id = 477,
        title = "Run",
        albumId = 102,
        album = "Concrete and Gold",
        artistId = 13,
        artist = "Foo Fighters",
        duration = 323424,
        discNumber = 1,
        trackNumber = 2,
        mediaUri = "Music/Concrete And Gold/Run.mp3",
        availabilityDate = 1506374520,
        fileSize = 13_012_576.bytes,
        albumArtUri = null,
    )
}
