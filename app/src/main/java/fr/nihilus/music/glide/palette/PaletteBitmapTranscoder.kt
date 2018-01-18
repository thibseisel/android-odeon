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

package fr.nihilus.music.glide.palette

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.support.v7.graphics.Palette
import com.bumptech.glide.load.Option
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder

class PaletteBitmapTranscoder(
    private val bitmapPool: BitmapPool
) : ResourceTranscoder<Bitmap, PaletteBitmap> {

    override fun transcode(
        toTranscode: Resource<Bitmap>,
        options: Options
    ): Resource<PaletteBitmap> {
        val bitmap = toTranscode.get()
        val palette = onGeneratePalette(bitmap, options)
        val result = PaletteBitmap(palette, bitmap)
        return PaletteBitmapResource(result, bitmapPool)
    }

    private fun onGeneratePalette(bitmap: Bitmap, options: Options): Palette {
        val region = getRegionFromRelative(bitmap, options[PALETTE_RELATIVE_REGION])
        return Palette.from(bitmap)
            .setRegion(region.left, region.top, region.right, region.bottom)
            .maximumColorCount(options[MAX_COLOR_COUNT])
            .generate()
    }

    private fun getRegionFromRelative(bitmap: Bitmap, relativeRegion: RectF?): Rect {
        if (relativeRegion == null) {
            return Rect(0, 0, bitmap.width, bitmap.height)
        }

        return Rect(
            (bitmap.width * relativeRegion.left).toInt(),
            (bitmap.height * relativeRegion.top).toInt(),
            (bitmap.width * relativeRegion.right).toInt(),
            (bitmap.height * relativeRegion.bottom).toInt()
        )
    }

    companion object {
        /**
         * The maximum number of colors to use for the palette generation.
         * Must be a positive number.
         */
        @JvmField val MAX_COLOR_COUNT: Option<Int> =
            Option.memory("fr.nihilus.glidepalette.PaletteBitmapTranscoder.maxColorCount", 16)

        @JvmField val PALETTE_RELATIVE_REGION: Option<RectF?> =
            Option.memory("fr.nihilus.glidepalette.PaletteBitmapTranscoder.relativeRegion")
    }
}
