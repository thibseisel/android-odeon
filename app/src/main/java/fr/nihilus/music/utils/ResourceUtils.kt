/*
 * Copyright 2017 Thibault Seisel
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

package fr.nihilus.music.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.support.annotation.AttrRes
import android.support.annotation.ColorInt
import android.support.annotation.FloatRange
import android.support.annotation.Px
import android.support.v7.content.res.AppCompatResources
import android.util.TypedValue

@Px
fun dipToPixels(context: Context, dp: Float): Int {
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
    val resource = AppCompatResources.getDrawable(context, resourceId)
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

