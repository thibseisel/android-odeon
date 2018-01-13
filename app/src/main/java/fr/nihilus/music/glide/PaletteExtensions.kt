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

package fr.nihilus.music.glide

import android.graphics.RectF
import com.bumptech.glide.annotation.GlideExtension
import com.bumptech.glide.annotation.GlideOption
import com.bumptech.glide.request.RequestOptions
import fr.nihilus.music.glide.palette.PaletteBitmapTranscoder

/**
 * Extends Glide API with Android Palette generation abilities.
 */
@GlideExtension
object PaletteExtensions {

    /**
     * Set the maximum number of colors to use for the Palette generation.
     * The more color are used to generate a palette, the more accurate it is but the more time it
     * takes to generate it.
     * @param maxColorCount maximum number of colors. Must be strictly positive.
     */
    @GlideOption
    @JvmStatic
    fun maxColorCount(options: RequestOptions, maxColorCount: Int): RequestOptions {
        require(maxColorCount > 0) { "Invalid maxColorCount: $maxColorCount" }
        return options.set(PaletteBitmapTranscoder.MAX_COLOR_COUNT, maxColorCount)
    }

    /**
     * Define a region of the bitmap from which colors are eligible to generate a Palette.
     * As the bitmap size may not be known in advance, this region is defined relative to the
     * loaded bitmap's width and height. All parameters are expected to fit in `[0 ; 1]`.
     *
     * If this option is not set, the whole bitmap will be used as the region.
     *
     * @param left Position of the left edge of the region rectangle
     * relative to the width of the loaded bitmap.
     * @param top Position of the top edge of the region rectangle,
     * relative to the height of the loaded bitmap.
     * @param right Position of the right edge of the region rectangle
     * relative to the width of the loaded bitmap. Must be greater or equal to `left`.
     * @param bottom Position of the bottom edge of the region rectangle,
     * relative to the height of the loaded bitmap. Must be greater or equ to `top`.
     */
    @GlideOption
    @JvmStatic
    fun region(
            options: RequestOptions,
            left: Float,
            top: Float,
            right: Float,
            bottom: Float
    ): RequestOptions {
        require(left in 0f..1f)
        require(top in 0f..1f)
        require(right in 0f..1f)
        require(bottom in 0f..1f)
        require(left <= right) { "Invalid rectangle: X-coordinate of left > right" }
        require(top <= bottom) { "Invalid rectangle: Y-coordinate of top > bottom" }

        val rect = RectF(left, top, right, bottom)
        return options.set(PaletteBitmapTranscoder.PALETTE_RELATIVE_REGION, rect)
    }
}