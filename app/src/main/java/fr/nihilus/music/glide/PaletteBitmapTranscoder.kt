package fr.nihilus.music.glide

import android.graphics.Bitmap
import android.support.v7.graphics.Palette
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder

class PaletteBitmapTranscoder(
        private val bitmapPool: BitmapPool
) : ResourceTranscoder<Bitmap, PaletteBitmap> {

    override fun transcode(toTranscode: Resource<Bitmap>): Resource<PaletteBitmap> {
        val bitmap = toTranscode.get()
        val palette = onGeneratePalette(bitmap)
        val result = PaletteBitmap(palette, bitmap)
        return PaletteBitmapResource(result, bitmapPool)
    }

    private fun onGeneratePalette(bitmap: Bitmap): Palette {
        return Palette.from(bitmap)
                .setRegion(0, 3 * bitmap.height / 4, bitmap.width, bitmap.height)
                .maximumColorCount(16)
                .generate()
    }
}
