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