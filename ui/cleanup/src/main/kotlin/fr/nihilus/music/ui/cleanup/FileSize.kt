/*
 * Copyright 2023 Thibault Seisel
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

package fr.nihilus.music.ui.cleanup

import java.math.RoundingMode
import java.text.DecimalFormat
import kotlin.math.ln
import kotlin.math.pow

/**
 * Represents a [FileSize] as the given number of bytes.
 */
val Int.bytes get() = FileSize(toLong())

/**
 * Represents a [FileSize] as the given number of bytes.
 */
val Long.bytes get() = FileSize(this)

/**
 * Represents the size of a binary file.
 * @property bytes Size of the file expressed as a number of bytes. Should not be negative.
 */
@JvmInline
value class FileSize(val bytes: Long) {

    init {
        require(bytes >= 0) { "Invalid file size: $bytes bytes" }
    }

    override fun toString(): String = buildString {
        if (bytes < 1000) {
            append(bytes.coerceAtLeast(0))
            append(' ')
            append('o')
        } else {
            val formatter = DecimalFormat("0.##")
            formatter.roundingMode = RoundingMode.HALF_EVEN

            val scale = (ln(bytes.toDouble()) / ln(1000.0)).toInt()

            val formattedSize = formatter.format(bytes / 1000.0.pow(scale))
            val magnitudeUnit = when (scale) {
                1 -> 'k'
                2 -> 'M'
                3 -> 'G'
                4 -> 'T'
                else -> error("Unexpectedly high byte count: $bytes")
            }

            append(formattedSize)
            append(' ')
            append(magnitudeUnit)
            append('o')
        }
    }
}
