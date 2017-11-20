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

import android.annotation.SuppressLint
import android.graphics.RectF
import com.bumptech.glide.annotation.GlideExtension
import com.bumptech.glide.annotation.GlideOption
import com.bumptech.glide.request.RequestOptions
import fr.nihilus.music.glide.palette.PaletteBitmapTranscoder

@SuppressLint("CheckResult")
@GlideExtension
object PaletteExtensions {

    @GlideOption
    @JvmStatic fun maxColorCount(options: RequestOptions, maxColorCount: Int) {
        require(maxColorCount > 0) { "Invalid maxColorCount: $maxColorCount" }
        options.set(PaletteBitmapTranscoder.MAX_COLOR_COUNT, maxColorCount)
    }

    @GlideOption
    @JvmStatic fun region(options: RequestOptions, left: Float, top: Float, right: Float, bottom: Float) {
        require(left <= right) { "Invalid rectangle: X-coordinate of left > right" }
        require(top <= bottom) { "Invalid rectangle: Y-coordinate of top > bottom" }

        val rect = RectF(left, top, right, bottom)
        options.set(PaletteBitmapTranscoder.PALETTE_RELATIVE_REGION, rect)
    }
}