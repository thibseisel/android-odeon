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

package fr.nihilus.music.core.ui.extensions

import android.content.Context
import android.graphics.Color
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import kotlin.math.abs

/**
 * Produce a darker shade of this color by a given factor.
 */
@ColorInt
fun darker(@ColorInt color: Int, @FloatRange(from = 0.0, to = 1.0) factor: Float): Int {
    val hsv = FloatArray(3)
    Color.colorToHSV(color, hsv)
    hsv[2] *= factor
    return Color.HSVToColor(hsv)
}

/**
 * Return the alpha component of a color int.
 */
@Suppress("unused")
inline val @receiver:ColorInt Int.alpha get() = (this shr 24) and 0xff

/**
 * Return the red component of a color int.
 */
inline val @receiver:ColorInt Int.red get() = (this shr 16) and 0xff

/**
 * return the green component of a color int.
 */
inline val @receiver:ColorInt Int.green get() = (this shr 8) and 0xff

/**
 * Return the blue component of a color int.
 */
inline val @receiver:ColorInt Int.blue get() = this and 0xff

/**
 * Computes the relative luminance of a color.
 * Assumes sRGB encoding. Based on the formula for relative luminance
 * defined in WCAG 2.0, W3C Recommendation 11 December 2008.
 *
 * @receiver A color packed integer in the sRGB color space.
 * @return a value between 0 (darkest black) and 1 (lightest white).
 * @see Color.luminance
 */
val @receiver:ColorInt Int.luminance: Float
    get() = (0.2126f * red) + (0.7152f * green) + (0.0722f * blue)

/**
 * Convert RGB components of a color to HSL (hue-saturation-lightness).
 * - `outHsl[0]` is Hue in `[0..360[`
 * - `outHsl[1]` is Saturation in `[0..1]`
 * - `outHsl[2]` is Lightness in `[0..1]`
 *
 * @receiver A color from the sRGB space from which HSL components should be extracted.
 * @param outHsl An optional 3-element array which holds the resulting HSL components.
 * If this argument is not provided, a new array will be created.
 *
 * @return The resulting HSL components, for convenience. This is the same as [outHsl].
 */
fun @receiver:ColorInt Int.toHsl(outHsl: FloatArray = FloatArray(3)) = outHsl.also {
    val r = red
    val g = green
    val b = blue

    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f

    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val deltaMaxMin = max - min

    var h: Float
    val s: Float
    val l = (max + min) / 2f

    if (max == min) {
        // Monochromatic
        s = 0f
        h = s
    } else {
        h = when (max) {
            rf -> (gf - bf) / deltaMaxMin % 6f
            gf -> (bf - rf) / deltaMaxMin + 2f
            else -> (rf - gf) / deltaMaxMin + 4f
        }
        s = deltaMaxMin / (1f - abs(2f * l - 1f))
    }

    h = h * 60f % 360f
    if (h < 0) {
        h += 360f
    }

    outHsl[0] = h.coerceIn(0f, 360f)
    outHsl[1] = s.coerceIn(0f, 1f)
    outHsl[2] = l.coerceIn(0f, 1f)
}

fun resolveThemeColor(context: Context, themeAttrId: Int): Int {
    val outValue = TypedValue()
    context.theme.resolveAttribute(themeAttrId, outValue, true)
    return outValue.data
}