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
import android.support.v7.graphics.Palette
import android.util.Log
import com.bumptech.glide.load.EncodeStrategy
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.ResourceEncoder
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.bitmap.BitmapEncoder
import com.bumptech.glide.load.resource.bitmap.BitmapResource
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Stores PaletteBitmap resources into the cache.
 */
internal class PaletteBitmapEncoder(
        private val bitmapPool: BitmapPool
) : ResourceEncoder<PaletteBitmap> {
    private val bitmapEncoder = BitmapEncoder()

    override fun encode(data: Resource<PaletteBitmap>, file: File, options: Options?): Boolean {
        val (palette: Palette, bitmap: Bitmap) = data.get()
        val bitmapRes = BitmapResource(bitmap, bitmapPool)

        return bitmapEncoder.encode(bitmapRes, file, options)
                && writePalette(palette, file)
    }

    override fun getEncodeStrategy(options: Options?): EncodeStrategy =
            bitmapEncoder.getEncodeStrategy(options)

    private fun writePalette(palette: Palette, file: File): Boolean {

        val fos = FileOutputStream(file, true)
        try {
            DataOutputStream(fos).use {
                for (swatch in palette.swatches) {
                    it.writeInt(swatch.rgb)
                    it.writeInt(swatch.population)
                }
            }

            return true
        } catch (ioe: IOException) {
            Log.w("PaletteBitmapEncoder", "Failed to encode PaletteBitmap", ioe)
            return false
        }
    }
}