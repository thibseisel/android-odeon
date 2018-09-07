/*
 * Copyright 2018 Thibault Seisel
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

@file:JvmName("ResourceUtils")

package fr.nihilus.music.media.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.support.annotation.*
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.ColorUtils
import android.util.TypedValue
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red

@Px
fun dipToPixels(context: Context, @Dimension(unit = Dimension.DP) dp: Float): Int {
    val metrics = context.resources.displayMetrics
    return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, metrics))
}

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
    ColorUtils.colorToHSL(this, it)
}

/**
 * Resolve a color attribute from the application theme such as `colorPrimary` or `colorAccent`.
 *
 * @param themeAttr theme attribute pointing on that color
 * @return the desired color packed as an aRGB color int.
 */
@ColorInt
fun resolveThemeColor(context: Context, @AttrRes themeAttr: Int): Int {
    val outValue = TypedValue()
    val theme = context.theme
    theme.resolveAttribute(themeAttr, outValue, true)
    return outValue.data
}

/**
 * Create a Bitmap from an Android Drawable Resource.
 *
 * @param context Context to access resources.
 * @param resourceId The ID of the drawable to load from resources.
 * @param desiredWidth Width of the resulting bitmap in pixels. Must be strictly positive.
 * @param desiredHeight Height of the resulting bitmap in pixels. Must be strictly positive.
 */
fun loadResourceAsBitmap(
    context: Context,
    resourceId: Int,
    desiredWidth: Int,
    desiredHeight: Int
): Bitmap {
    val resource = ContextCompat.getDrawable(context, resourceId)
            ?: throw IllegalStateException("Unable to decode resource.")

    // Short-circuit: the loaded drawable is already a bitmap resource
    if (resource is BitmapDrawable) {
        return resource.bitmap
    }

    // Otherwise, render the resource drawable onto a bitmap-baked canvas
    return if (resource.intrinsicWidth <= 0 || resource.intrinsicHeight <= 0) {
        // This kind of drawable has no dimension. Draw it with
        Bitmap.createBitmap(desiredWidth, desiredHeight, Bitmap.Config.ARGB_8888)
    } else {
        // Constraint width and height by the provided maximums
        val width = minOf(desiredWidth, resource.intrinsicWidth)
        val height = minOf(desiredHeight, resource.intrinsicHeight)
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }.apply {
        val canvas = Canvas(this)
        resource.setBounds(0, 0, canvas.width, canvas.height)
        resource.draw(canvas)
    }
}

