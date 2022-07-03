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

package fr.nihilus.music.service

import fr.nihilus.music.media.tracks.Track
import kotlin.random.Random

private const val ALPHABET_SIZE = 26

/**
 * Generate a sequence of letters from the english alphabet in order.
 * Letters are combined after reaching Z.
 *
 * `A, B, C, [...], Y, Z, AA, AB, AC, [...], ZY, ZZ, AAA, AAB, etc.`
 */
internal val alphabetSequence: Sequence<String>
    get() = generateSequence(0) { it + 1 }
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

internal val randomCamelWordsSequence: Sequence<String>
    get() = generateSequence {
        // Generate titles consisting of a maximum of 3 words, each word having between 1 and 10 letters.
        val numberOfWords = Random.nextInt(1, 4)

        buildString {
            repeat(numberOfWords) { wordIndex ->
                // Separate words with spaces
                if (wordIndex > 0) {
                    append(' ')
                }

                // The first letter of a word is an uppercase letter.
                val firstLetter = Random.nextInt('A'.code, 'Z'.code).toChar()
                append(firstLetter)

                val numberOfLowercases = Random.nextInt(1, 10)
                repeat(numberOfLowercases) {
                    val letter = Random.nextInt('a'.code, 'z'.code).toChar()
                    append(letter)
                }
            }
        }
    }

private const val APRIL_30TH_2019 = 1556575200L
private const val ONE_DAY = 24 * 3600L

internal fun generateRandomTrackSequence(): Sequence<Track> = sequence {
    val alphabetTitles = alphabetSequence.iterator()
    val randomTitles = randomCamelWordsSequence.iterator()
    val releaseDates = generateSequence(APRIL_30TH_2019) { it - ONE_DAY }.iterator()

    while (true) {
        val trackId = Random.nextLong(0, 512)

        yield(
            Track(
                id = trackId,
                title = alphabetTitles.next(),
                artist = randomTitles.next(),
                album = randomTitles.next(),
                duration = Random.nextLong(30_000L, 15 * 60 * 1000L),
                discNumber = 1,
                trackNumber = Random.nextInt(1, 16),
                mediaUri = "path/to/file_$trackId",
                albumArtUri = null,
                availabilityDate = releaseDates.next(),
                artistId = Random.nextLong(0, 128),
                albumId = Random.nextLong(0, 256),
                fileSize = 0
            )
        )
    }
}