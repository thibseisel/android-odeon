package fr.nihilus.music.glide

import android.graphics.RectF
import com.bumptech.glide.annotation.GlideExtension
import com.bumptech.glide.annotation.GlideOption
import com.bumptech.glide.request.RequestOptions

@GlideExtension
object PaletteExtensions {

    @GlideOption
    @JvmStatic fun maxColorCount(options: RequestOptions, maxColorCount: Int) {
        if (maxColorCount < 1) {
            throw IllegalArgumentException("Invalid maxColorCount: $maxColorCount")
        }

        options.set(PaletteBitmapTranscoder.MAX_COLOR_COUNT, maxColorCount)
    }

    @GlideOption
    @JvmStatic fun region(options: RequestOptions, left: Float, top: Float, right: Float, bottom: Float) {
        val rect = RectF(left, top, right, bottom)
        options.set(PaletteBitmapTranscoder.PALETTE_RELATIVE_REGION, rect)
    }
}