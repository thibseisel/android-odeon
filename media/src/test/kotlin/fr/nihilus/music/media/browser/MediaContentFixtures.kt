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

package fr.nihilus.music.media.browser

import androidx.core.net.toUri
import fr.nihilus.music.core.media.MediaId
import fr.nihilus.music.core.media.MediaId.Builder.CATEGORY_ALL
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ALBUMS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_ARTISTS
import fr.nihilus.music.core.media.MediaId.Builder.TYPE_TRACKS
import fr.nihilus.music.media.AudioTrack
import fr.nihilus.music.media.MediaCategory

internal object AudioTracks {
    val Cartagena = AudioTrack(
        id = MediaId(TYPE_TRACKS, CATEGORY_ALL, 161),
        title = "1741 (The Battle of Cartagena)",
        artist = "Alestorm",
        album = "Alestorm",
        mediaUri = "content://android/tracks/161".toUri(),
        iconUri = "content://android/artworks/65".toUri(),
        duration = 437603,
        disc = 1,
        number = 4,
    )

    val IsolatedSystem = AudioTrack(
        id = MediaId(TYPE_TRACKS, CATEGORY_ALL, 65),
        title = "The 2nd Law: Isolated System",
        artist = "Muse",
        album = "The 2nd Law",
        mediaUri = "content://android/tracks/65".toUri(),
        iconUri = "content://android/artworks/40".toUri(),
        duration = 300042,
        disc = 1,
        number = 13,
    )

    val Algorithm = AudioTrack(
        id = MediaId(TYPE_TRACKS, CATEGORY_ALL, 865),
        title = "Algorithm",
        artist = "Muse",
        album = "Simulation Theory",
        mediaUri = "content://android/tracks/865".toUri(),
        iconUri = "content://android/artworks/98".toUri(),
        duration = 245960,
        disc = 1,
        number = 1,
    )

    val DirtyWater = AudioTrack(
        id = MediaId(TYPE_TRACKS, CATEGORY_ALL, 481),
        title = "Dirty Water",
        artist = "Foo Fighters",
        album = "Concrete and Gold",
        mediaUri = "content://android/tracks/481".toUri(),
        iconUri = "content://android/artworks/102".toUri(),
        duration = 320914,
        disc = 1,
        number = 6,
    )

    val KnightsOfCydonia = AudioTrack(
        id = MediaId(TYPE_TRACKS, CATEGORY_ALL, 294),
        title = "Knights of Cydonia",
        artist = "Muse",
        album = "Black Holes and Revelations",
        mediaUri = "content://android/tracks/294".toUri(),
        iconUri = "content://android/artworks/38".toUri(),
        duration = 366946,
        disc = 1,
        number = 11,
    )

    val MatterOfTime = AudioTrack(
        id = MediaId(TYPE_TRACKS, CATEGORY_ALL, 219),
        title = "Matter of Time",
        artist = "Alestorm",
        album = "Alestorm",
        mediaUri = "content://android/tracks/219".toUri(),
        iconUri = "content://android/artworks/26".toUri(),
        duration = 437603,
        disc = 1,
        number = 8,
    )

    val ThePretenders = AudioTrack(
        id = MediaId(TYPE_TRACKS, CATEGORY_ALL, 464),
        title = "The Pretenders",
        artist = "Foo Fighters",
        album = "Echoes, Silence, Patience & Grace",
        mediaUri = "content://android/tracks/464".toUri(),
        iconUri = "content://android/artworks/95".toUri(),
        duration = 266509,
        disc = 1,
        number = 1,
    )

    val Run = AudioTrack(
        id = MediaId(TYPE_TRACKS, CATEGORY_ALL, 477),
        title = "Run",
        artist = "Foo Fighters",
        album = "Concrete and Gold",
        mediaUri = "content://android/tracks/477".toUri(),
        iconUri = "content://android/artworks/102".toUri(),
        duration = 323424,
        disc = 1,
        number = 2,
    )
}

internal object MediaCategories {
    val The2ndLaw = MediaCategory(
        id = MediaId(TYPE_ALBUMS, "40"),
        title = "The 2nd Law",
        subtitle = "Muse",
        iconUri = "content://android/artworks/40".toUri(),
        playable = false,
        count = 1,
    )

    val BlackHoles = MediaCategory(
        id = MediaId(TYPE_ALBUMS, "38"),
        title = "Black Holes and Revelations",
        subtitle = "Muse",
        iconUri = "content://android/artworks/38".toUri(),
        playable = false,
        count = 1,
    )

    val ConcreteAndGold = MediaCategory(
        id = MediaId(TYPE_ALBUMS, "102"),
        title = "Concrete and Gold",
        subtitle = "Foo Fighters",
        iconUri = "content://android/artworks/102".toUri(),
        playable = false,
        count = 2,
    )

    val EchoesSilence = MediaCategory(
        id = MediaId(TYPE_ALBUMS, "95"),
        title = "Echoes, Silence, Patience & Grace",
        subtitle = "Foo Fighters",
        iconUri = "content://android/artworks/95".toUri(),
        playable = false,
        count = 1,
    )

    val SimulationTheory = MediaCategory(
        id = MediaId(TYPE_ALBUMS, "98"),
        title = "Simulation Theory",
        subtitle = "Muse",
        iconUri = "content://android/artworks/98".toUri(),
        playable = false,
        count = 1,
    )

    val SunsetOnGoldenAge = MediaCategory(
        id = MediaId(TYPE_ALBUMS, "65"),
        title = "Sunset on the Golden Age",
        subtitle = "Alestorm",
        iconUri = "content://android/artworks/65".toUri(),
        playable = false,
        count = 1,
    )

    val WastingLight = MediaCategory(
        id = MediaId(TYPE_ALBUMS, "26"),
        title = "Wasting Light",
        subtitle = "Foo Fighters",
        iconUri = "content://android/artworks/26".toUri(),
        playable = false,
        count = 1,
    )

    val Alestorm = MediaCategory(
        id = MediaId(TYPE_ARTISTS, "26"),
        title = "Alestorm",
        iconUri = "content://android/artworks/65".toUri(),
        playable = false,
    )

    val FooFighters = MediaCategory(
        id = MediaId(TYPE_ARTISTS, "13"),
        title = "Foo Fighters",
        iconUri = "content://android/artworks/102".toUri(),
        playable = false,
    )

    val Muse = MediaCategory(
        id = MediaId(TYPE_ARTISTS, "26"),
        title = "Muse",
        iconUri = "content://android/artworks/98".toUri(),
        playable = false,
    )

    val MyFavorites = MediaCategory(
        id = MediaId(MediaId.TYPE_PLAYLISTS, "1"),
        title = "My favorites",
        subtitle = "2 tracks",
        iconUri = "content://playlists-icons/1".toUri(),
        playable = true,
        count = 2,
    )
}
