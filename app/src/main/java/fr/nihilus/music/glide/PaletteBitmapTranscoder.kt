package fr.nihilus.music.glide

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.support.v7.graphics.Palette
import com.bumptech.glide.load.Option
import com.bumptech.glide.load.engine.Resource
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool
import com.bumptech.glide.load.resource.transcode.ResourceTranscoder

class PaletteBitmapTranscoder(
        private val bitmapPool: BitmapPool
) : ResourceTranscoder<Bitmap, PaletteBitmap> {

    override fun transcode(toTranscode: Resource<Bitmap>): Resource<PaletteBitmap> {
        // TODO With Glide 4.2, signature allow passing options to configure Palette.Builder
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

    companion object {
        /**
         * The maximum number of colors to use for the palette generation.
         * Must be a positive number.
         */
        @JvmStatic val MAX_COLOR_COUNT: Option<Int> =
                Option.memory("fr.nihilus.glidepalette.PaletteBitmapTranscoder.maxColorCount", 16)
        /**
         *
         */
        @JvmStatic val PALETTE_REGION: Option<Rect?> =
                Option.memory("fr.nihilus.glidepalette.PaletteBitmapTranscoder.region")
        @JvmStatic val PALETTE_RELATIVE_REGION: Option<RectF?> =
                Option.memory("fr.nihilus.glidepalette.PaletteBitmapTranscoder.relativeRegion")
    }
}
