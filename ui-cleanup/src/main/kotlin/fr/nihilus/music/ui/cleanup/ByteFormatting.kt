/*
 * Copyright 2020 Thibault Seisel
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
 * Format a given number of bytes to a human-readable amount, for example `23.45 Mo`.
 *
 * The number is expressed in decimal octets, its unit being the closest power of 1000.
 * If the converted number is decimal, the result is rounded evenly to the 2nd decimal.
 *
 * @param byteCount The byte count to be formatted.
 * The maximum supported value is `10^15 - 1`.
 * Negative values are considered to be `0`.
 *
 * @return A string that can be used to display the specified number of bytes to users.
 */
internal fun formatToHumanReadableByteCount(byteCount: Long) = buildString {
    if (byteCount < 1000) {
        append(byteCount.coerceAtLeast(0))
        append(' ')
        append('o')
    } else {
        val formatter = DecimalFormat("0.##")
        formatter.roundingMode = RoundingMode.HALF_EVEN

        val scale = (ln(byteCount.toDouble()) / ln(1000.0)).toInt()

        val formattedSize = formatter.format(byteCount / 1000.0.pow(scale))
        val magnitudeUnit = when (scale) {
            1 -> 'k'
            2 -> 'M'
            3 -> 'G'
            4 -> 'T'
            else -> error("Unexpectedly high byte count: $byteCount")
        }

        append(formattedSize)
        append(' ')
        append(magnitudeUnit)
        append('o')
    }
}