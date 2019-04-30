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

package fr.nihilus.music.media.provider

import fr.nihilus.music.media.playlists.Playlist
import kotlin.random.Random

internal val SAMPLE_TRACKS = listOf(
    Track(161, "1741 (The Battle of Cartagena)", "Alestorm", "Sunset on the Golden Age", 437603, 1, 4, "", null, 1466283480, 26, 65),
    Track(309, "The 2nd Law: Isolated System", "Muse", "The 2nd Law", 300042, 1, 13, "", null, 1439653800, 18, 40),
    Track(481, "Dirty Water", "Foo Fighters", "Concrete and Gold", 320914, 1, 6, "", null, 1506374520, 13, 102),
    Track(48, "Give It Up", "AC/DC", "Greatest Hits 30 Anniversary Edition", 233592, 1, 19, "", null, 1455310080, 5, 7),
    Track(125, "Jailbreak", "AC/DC", "Greatest Hits 30 Anniversary Edition", 276668, 2, 14, "", null, 1455310140, 5, 7),
    Track(294, "Knights of Cydonia", "Muse", "Black Holes and Revelations", 366946, 1, 11, "", null, 1414880700, 18, 38),
    Track(219, "A Matter of Time", "Foo Fighters", "Wasting Light", 276140, 1, 8, "", null, 1360677660, 13, 26),
    Track(75, "Nightmare", "Avenged Sevenfold", "Nightmare", 374648, 1, 1, "", null, 1439590380, 4, 6),
    Track(464, "The Pretenders", "Foo Fighters", "Echoes, Silence, Patience & Grace", 266509, 1, 1, "", null, 1439653740, 13, 95),
    Track(477, "Run", "Foo Fighters", "Concrete and Gold", 323424, 1, 2, "", null, 1506374520, 13, 102)
)
internal val SAMPLE_ALBUMS = listOf(
    Album(40, "The 2nd Law", "Muse", 1, 2012, null, 18),
    Album(38, "Black Holes and Revelations", "Muse", 1, 2006, null, 18),
    Album(102, "Concrete and Gold", "Foo Fighters", 2, 2017, null, 13),
    Album(95, "Echoes, Silence, Patience & Grace", "Foo Fighters", 1, 2007, null, 13),
    Album(7, "Greatest Hits Anniversary Edition", "AC/DC", 2, 2010, null, 5),
    Album(6, "Nightmare", "Avenged Sevenfold", 1, 2010, null, 4),
    Album(65, "Sunset on the Golden Age", "Alestorm", 1, 2014, null, 26),
    Album(26, "Wasting Light", "Foo Fighters", 1, 2011, null, 13)
)

internal val SAMPLE_ARTISTS = listOf(
    Artist(5, "AC/DC", 1, 2, null),
    Artist(26, "Alestorm", 1, 1, null),
    Artist(4, "Avenged Sevenfold", 1, 1, null),
    Artist(13, "Foo Fighters", 4, 3, null),
    Artist(18, "Muse", 2, 2, null)
)

internal val SAMPLE_PLAYLISTS = listOf(
    Playlist(1, "Zen", 1551434321, null),
    Playlist(2, "Sport", 1551435123, null),
    Playlist(3, "Metal", 1551436125, null)
)

internal val SAMPLE_PLAYLIST_TRACKS = mapOf(
    1L to listOf(SAMPLE_TRACKS[1]),
    2L to listOf(SAMPLE_TRACKS[9], SAMPLE_TRACKS[3], SAMPLE_TRACKS[4]),
    3L to listOf(SAMPLE_TRACKS[7], SAMPLE_TRACKS[0])
)

internal val SAMPLE_MOST_RATED_TRACKS = listOf(
    SAMPLE_TRACKS[7], // 75 - Nightmare
    SAMPLE_TRACKS[8], // 464 - The Pretenders
    SAMPLE_TRACKS[3], // 48 - Give It Up
    SAMPLE_TRACKS[9], // 477 - Run
    SAMPLE_TRACKS[5] // 294 - Knights of Cydonia
)

private const val ALPHABET_SIZE = 26

/**
 * Generate a sequence of letters from the english alphabet in order.
 * Letters are combined after reaching Z.
 *
 * `A, B, C, [...], Y, Z, AA, AB, AC, [...], ZY, ZZ, AAA, AAB, etc.`
 */
internal val alphabetSequence = generateSequence(0) { it + 1 }
    .map { seed ->
        buildString {
            var n = seed
            while (n >= ALPHABET_SIZE) {
                val code = n % ALPHABET_SIZE
                append('A' + code)
                n = (n - code) / ALPHABET_SIZE - 1
            }

            append('A' + n)
            reverse()
        }
    }

internal val randomCamelWordsSequence = generateSequence {
    // Generate titles consisting of a maximum of 3 words, each word having between 1 and 10 letters.
    val numberOfWords = Random.nextInt(1, 4)

    buildString {
        repeat(numberOfWords) { wordIndex ->
            // Separate words with spaces
            if (wordIndex > 0) {
                append(' ')
            }

            // The first letter of a word is an uppercase letter.
            val firstLetter = Random.nextInt('A'.toInt(), 'Z'.toInt()).toChar()
            append(firstLetter)

            val numberOfLowercases = Random.nextInt(1, 10)
            repeat(numberOfLowercases) {
                val letter = Random.nextInt('a'.toInt(), 'z'.toInt()).toChar()
                append(letter)
            }
        }
    }
}

private const val APRIL_30TH_2019 = 1556575200L
private const val ONE_DAY_MILLIS = 24 * 3600 * 1000L

internal fun generateRandomTrackSequence(): Sequence<Track> = sequence {
    val alphabetTitles = alphabetSequence.iterator()
    val randomTitles = randomCamelWordsSequence.iterator()
    val releaseDates = generateSequence(APRIL_30TH_2019) { it - ONE_DAY_MILLIS }.iterator()

    while (true) {
        val trackId = Random.nextLong(0, 512)

        yield(
            Track(
                id = trackId,
                title = alphabetTitles.next(),
                album = randomTitles.next(),
                artist = randomTitles.next(),
                duration = Random.nextLong(30_000L, 15 * 60 * 1000L),
                discNumber = 1,
                trackNumber = Random.nextInt(1, 16),
                mediaUri = "path/to/file_$trackId",
                albumArtUri = null,
                availabilityDate = releaseDates.next(),
                albumId = Random.nextLong(0, 256),
                artistId = Random.nextLong(0, 128)
            )
        )
    }
}