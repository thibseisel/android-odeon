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

@file:JvmName("ViewUtils")

package fr.nihilus.music.utils

import android.content.Context
import android.graphics.Color
import android.support.annotation.AttrRes
import android.support.annotation.ColorInt
import android.support.annotation.FloatRange
import android.support.annotation.Px
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
